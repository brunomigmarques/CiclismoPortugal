/**
 * Firebase Cloud Functions for Ciclismo Portugal
 *
 * This function runs daily to sync YouTube videos for the app.
 * All users read from Firestore - no client-side API calls needed.
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");
const fetch = require("node-fetch");

admin.initializeApp();

const db = admin.firestore();

// YouTube API configuration
const YOUTUBE_API_KEY = functions.config().youtube?.api_key || "";
const YOUTUBE_BASE_URL = "https://www.googleapis.com/youtube/v3";
const VIDEOS_COLLECTION = "cycling_videos";
const SYNC_METADATA_DOC = "sync_metadata";

/**
 * Scheduled function to sync YouTube videos daily.
 * Runs at 6:00 AM UTC every day.
 */
exports.syncCyclingVideos = functions
    .region("europe-west1")
    .pubsub
    .schedule("0 6 * * *")  // Every day at 6:00 AM UTC
    .timeZone("Europe/Lisbon")
    .onRun(async (context) => {
        console.log("Starting daily video sync...");

        if (!YOUTUBE_API_KEY) {
            console.error("YouTube API key not configured. Set it with: firebase functions:config:set youtube.api_key=YOUR_KEY");
            return null;
        }

        try {
            const videos = await fetchCyclingVideos();

            if (videos.length > 0) {
                await saveVideosToFirestore(videos);
                console.log(`Successfully synced ${videos.length} videos`);
            } else {
                console.log("No videos found to sync");
            }

            return null;
        } catch (error) {
            console.error("Error syncing videos:", error);
            return null;
        }
    });

/**
 * HTTP endpoint to manually trigger video sync (for testing/admin).
 */
exports.triggerVideoSync = functions
    .region("europe-west1")
    .https
    .onRequest(async (req, res) => {
        // Only allow POST requests
        if (req.method !== "POST") {
            res.status(405).send("Method Not Allowed");
            return;
        }

        if (!YOUTUBE_API_KEY) {
            res.status(500).send("YouTube API key not configured");
            return;
        }

        try {
            const videos = await fetchCyclingVideos();

            if (videos.length > 0) {
                await saveVideosToFirestore(videos);
                res.status(200).json({
                    success: true,
                    message: `Synced ${videos.length} videos`,
                    videos: videos.map(v => ({ id: v.videoId, title: v.title }))
                });
            } else {
                res.status(200).json({
                    success: false,
                    message: "No videos found"
                });
            }
        } catch (error) {
            console.error("Error in manual sync:", error);
            res.status(500).json({
                success: false,
                error: error.message
            });
        }
    });

/**
 * Fetch cycling videos from YouTube Data API.
 *
 * Priority order:
 * 1. Next 3 upcoming Portuguese races (from Firestore provas collection)
 * 2. Recent news articles (from Firestore)
 * 3. Backup reliable cycling topics
 */
async function fetchCyclingVideos() {
    const allVideos = [];
    const seenIds = new Set();
    const searchTopics = [];
    const now = Date.now();

    // 1. PRIORITY: Get next 3 upcoming provas from Firestore
    try {
        const provasSnapshot = await db.collection("provas")
            .where("data", ">=", now)
            .orderBy("data", "asc")
            .limit(3)
            .get();

        console.log(`Found ${provasSnapshot.size} upcoming provas in Firestore`);

        provasSnapshot.forEach(doc => {
            const prova = doc.data();
            const query = buildProvaQuery(prova.nome, prova.tipo);
            console.log(`Prova: ${prova.nome} -> Query: ${query}`);
            searchTopics.push({
                query: query,
                priority: 1
            });
        });
    } catch (error) {
        console.warn("Could not fetch provas from Firestore:", error.message);
    }

    // 2. PRIORITY: Get recent news from Firestore
    try {
        const threeDaysAgo = now - (3 * 24 * 60 * 60 * 1000);
        const newsSnapshot = await db.collection("news_articles")
            .where("publishedAt", ">=", threeDaysAgo)
            .orderBy("publishedAt", "desc")
            .limit(3)
            .get();

        console.log(`Found ${newsSnapshot.size} recent news in Firestore`);

        newsSnapshot.forEach(doc => {
            const article = doc.data();
            const query = extractNewsQuery(article.title);
            if (query) {
                searchTopics.push({
                    query: query,
                    priority: 2
                });
            }
        });
    } catch (error) {
        console.warn("Could not fetch news from Firestore:", error.message);
    }

    // 3. BACKUP: Add reliable cycling topics if we don't have enough
    const backupTopics = [
        { query: "ciclismo Portugal 2026", priority: 3 },
        { query: "BTT Portugal 2026", priority: 3 },
        { query: "GCN Racing cycling highlights", priority: 4 },
        { query: "Volta a Portugal ciclismo", priority: 4 },
        { query: "Tour de France highlights 2026", priority: 5 },
        { query: "Pogacar cycling 2026", priority: 5 },
        { query: "João Almeida ciclismo", priority: 5 }
    ];

    // Add backup topics that aren't duplicates
    const existingQueries = new Set(searchTopics.map(t => t.query.toLowerCase()));
    for (const topic of backupTopics) {
        if (!existingQueries.has(topic.query.toLowerCase())) {
            searchTopics.push(topic);
        }
    }

    console.log(`Total search topics: ${searchTopics.length}`);

    // Search YouTube for each topic
    for (const topic of searchTopics.slice(0, 10)) {  // Limit to 10 searches
        try {
            const videos = await searchYouTube(topic.query, 2);

            for (const video of videos) {
                if (!seenIds.has(video.videoId)) {
                    seenIds.add(video.videoId);
                    allVideos.push({
                        ...video,
                        priority: topic.priority
                    });
                }
            }

            // Small delay to avoid rate limiting
            await sleep(100);

        } catch (error) {
            console.error(`Error searching for "${topic.query}":`, error.message);
        }
    }

    // Sort by priority, then by publish date
    allVideos.sort((a, b) => {
        if (a.priority !== b.priority) {
            return a.priority - b.priority;
        }
        return new Date(b.publishedAt) - new Date(a.publishedAt);
    });

    // Return top 15 videos
    return allVideos.slice(0, 15);
}

/**
 * Build a search query for a Portuguese race.
 */
function buildProvaQuery(nome, tipo) {
    // Extract main words from race name
    const nameWords = nome
        .replace(/[0-9]+/g, "")
        .replace(/\s+/g, " ")
        .trim()
        .split(" ")
        .filter(word => word.length > 2)
        .slice(0, 4)
        .join(" ");

    // Add type context
    let typeContext = "ciclismo";
    if (tipo && tipo.toLowerCase().includes("btt")) {
        typeContext = "BTT ciclismo";
    } else if (tipo && tipo.toLowerCase().includes("gravel")) {
        typeContext = "gravel cycling";
    }

    return `${nameWords} ${typeContext}`.trim();
}

/**
 * Extract search query from news article title.
 */
function extractNewsQuery(title) {
    const titleLower = title.toLowerCase();

    // Check for cyclist names
    const cyclists = ["pogacar", "vingegaard", "evenepoel", "van aert", "van der poel", "almeida", "oliveira"];
    for (const name of cyclists) {
        if (titleLower.includes(name)) {
            return `${name} cycling 2026`;
        }
    }

    // Check for race names
    const races = ["tour de france", "giro", "vuelta", "volta a portugal", "paris-roubaix"];
    for (const race of races) {
        if (titleLower.includes(race)) {
            return `${race} highlights 2026`;
        }
    }

    // Generic cycling
    if (titleLower.includes("ciclismo") || titleLower.includes("cycling")) {
        return "ciclismo portugal 2026";
    }

    return null;
}

/**
 * Search YouTube for videos matching a query.
 */
async function searchYouTube(query, maxResults = 5) {
    const encodedQuery = encodeURIComponent(query);
    const url = `${YOUTUBE_BASE_URL}/search?part=snippet&type=video&q=${encodedQuery}&maxResults=${maxResults}&key=${YOUTUBE_API_KEY}`;

    const response = await fetch(url);

    if (!response.ok) {
        const error = await response.text();
        throw new Error(`YouTube API error: ${response.status} - ${error}`);
    }

    const data = await response.json();
    const videos = [];

    for (const item of (data.items || [])) {
        const videoId = item.id?.videoId;
        if (!videoId || videoId.length !== 11) continue;

        const snippet = item.snippet || {};
        const thumbnails = snippet.thumbnails || {};

        videos.push({
            videoId: videoId,
            title: snippet.title || "",
            description: snippet.description || "",
            channelName: snippet.channelTitle || "",
            thumbnailUrl: thumbnails.high?.url || thumbnails.medium?.url || thumbnails.default?.url || `https://img.youtube.com/vi/${videoId}/hqdefault.jpg`,
            publishedAt: snippet.publishedAt || "",
            videoUrl: `https://www.youtube.com/watch?v=${videoId}`
        });
    }

    console.log(`Found ${videos.length} videos for query: ${query}`);
    return videos;
}

/**
 * Save videos to Firestore.
 */
async function saveVideosToFirestore(videos) {
    const batch = db.batch();
    const collectionRef = db.collection(VIDEOS_COLLECTION);

    // Delete existing videos (except metadata)
    const existingDocs = await collectionRef.get();
    for (const doc of existingDocs.docs) {
        if (doc.id !== SYNC_METADATA_DOC) {
            batch.delete(doc.ref);
        }
    }

    // Add new videos
    videos.forEach((video, index) => {
        const docRef = collectionRef.doc(video.videoId);
        batch.set(docRef, {
            videoId: video.videoId,
            title: video.title,
            description: video.description,
            channelName: video.channelName,
            thumbnailUrl: video.thumbnailUrl,
            publishedAt: video.publishedAt,
            videoUrl: video.videoUrl,
            priority: index,
            timestamp: admin.firestore.FieldValue.serverTimestamp(),
            source: "YOUTUBE"
        });
    });

    // Update sync metadata
    const metadataRef = collectionRef.doc(SYNC_METADATA_DOC);
    batch.set(metadataRef, {
        lastSyncTimestamp: admin.firestore.FieldValue.serverTimestamp(),
        videoCount: videos.length,
        syncedBy: "cloud_function"
    });

    await batch.commit();
    console.log(`Saved ${videos.length} videos to Firestore`);
}

/**
 * Helper function to sleep for a given number of milliseconds.
 */
function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}
