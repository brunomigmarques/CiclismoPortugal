package com.ciclismo.portugal.domain.model

/**
 * Maps World Tour race names to their official logo/image URLs.
 * Uses Wikipedia/Wikimedia Commons and official race logos.
 */
object RaceImageMapper {

    // Known race images mapping (case-insensitive matching)
    // Using cycling race photos from public sources
    private val raceImages = mapOf(
        // Grand Tours - using scenic race photos
        "tour de france" to "https://images.unsplash.com/photo-1517649763962-0c623066013b?w=400&h=300&fit=crop",
        "giro d'italia" to "https://images.unsplash.com/photo-1541625602330-2277a4c46182?w=400&h=300&fit=crop",
        "vuelta a espana" to "https://images.unsplash.com/photo-1534787238916-9ba6764efd4f?w=400&h=300&fit=crop",
        "la vuelta" to "https://images.unsplash.com/photo-1534787238916-9ba6764efd4f?w=400&h=300&fit=crop",
        "vuelta espana" to "https://images.unsplash.com/photo-1534787238916-9ba6764efd4f?w=400&h=300&fit=crop",

        // Monuments (One-day classics)
        "milan-sanremo" to "https://images.unsplash.com/photo-1571188654248-7a89213915f7?w=400&h=300&fit=crop",
        "milano-sanremo" to "https://images.unsplash.com/photo-1571188654248-7a89213915f7?w=400&h=300&fit=crop",
        "tour of flanders" to "https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=400&h=300&fit=crop",
        "ronde van vlaanderen" to "https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=400&h=300&fit=crop",
        "paris-roubaix" to "https://images.unsplash.com/photo-1507035895480-2b3156c31fc8?w=400&h=300&fit=crop",
        "liege-bastogne-liege" to "https://images.unsplash.com/photo-1541625602330-2277a4c46182?w=400&h=300&fit=crop",
        "liege bastogne liege" to "https://images.unsplash.com/photo-1541625602330-2277a4c46182?w=400&h=300&fit=crop",
        "il lombardia" to "https://images.unsplash.com/photo-1517649763962-0c623066013b?w=400&h=300&fit=crop",
        "giro di lombardia" to "https://images.unsplash.com/photo-1517649763962-0c623066013b?w=400&h=300&fit=crop",

        // Other major classics
        "strade bianche" to "https://images.unsplash.com/photo-1507035895480-2b3156c31fc8?w=400&h=300&fit=crop",
        "amstel gold race" to "https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=400&h=300&fit=crop",
        "fleche wallonne" to "https://images.unsplash.com/photo-1541625602330-2277a4c46182?w=400&h=300&fit=crop",
        "la fleche wallonne" to "https://images.unsplash.com/photo-1541625602330-2277a4c46182?w=400&h=300&fit=crop",
        "san sebastian" to "https://images.unsplash.com/photo-1534787238916-9ba6764efd4f?w=400&h=300&fit=crop",
        "clasica san sebastian" to "https://images.unsplash.com/photo-1534787238916-9ba6764efd4f?w=400&h=300&fit=crop",

        // Stage races
        "paris-nice" to "https://images.unsplash.com/photo-1571188654248-7a89213915f7?w=400&h=300&fit=crop",
        "tirreno-adriatico" to "https://images.unsplash.com/photo-1571188654248-7a89213915f7?w=400&h=300&fit=crop",
        "volta a catalunya" to "https://images.unsplash.com/photo-1534787238916-9ba6764efd4f?w=400&h=300&fit=crop",
        "tour de romandie" to "https://images.unsplash.com/photo-1517649763962-0c623066013b?w=400&h=300&fit=crop",
        "criterium du dauphine" to "https://images.unsplash.com/photo-1517649763962-0c623066013b?w=400&h=300&fit=crop",
        "tour de suisse" to "https://images.unsplash.com/photo-1541625602330-2277a4c46182?w=400&h=300&fit=crop",
        "tour de pologne" to "https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=400&h=300&fit=crop",
        "vuelta a burgos" to "https://images.unsplash.com/photo-1534787238916-9ba6764efd4f?w=400&h=300&fit=crop",
        "tour of britain" to "https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=400&h=300&fit=crop",

        // Early season
        "volta ao algarve" to "https://images.unsplash.com/photo-1534787238916-9ba6764efd4f?w=400&h=300&fit=crop",
        "tour down under" to "https://images.unsplash.com/photo-1517649763962-0c623066013b?w=400&h=300&fit=crop",
        "uae tour" to "https://images.unsplash.com/photo-1507035895480-2b3156c31fc8?w=400&h=300&fit=crop",

        // Belgian classics
        "omloop het nieuwsblad" to "https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=400&h=300&fit=crop",
        "kuurne-brussel-kuurne" to "https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=400&h=300&fit=crop",
        "e3 saxo classic" to "https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=400&h=300&fit=crop",
        "gent-wevelgem" to "https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=400&h=300&fit=crop",
        "dwars door vlaanderen" to "https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=400&h=300&fit=crop",
        "brabantse pijl" to "https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=400&h=300&fit=crop",
        "scheldeprijs" to "https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=400&h=300&fit=crop",

        // World Championships
        "world championships" to "https://images.unsplash.com/photo-1517649763962-0c623066013b?w=400&h=300&fit=crop",
        "uci world championships" to "https://images.unsplash.com/photo-1517649763962-0c623066013b?w=400&h=300&fit=crop",

        // Portuguese races
        "volta a portugal" to "https://images.unsplash.com/photo-1534787238916-9ba6764efd4f?w=400&h=300&fit=crop",
        "grande premio de portugal" to "https://images.unsplash.com/photo-1534787238916-9ba6764efd4f?w=400&h=300&fit=crop"
    )

    /**
     * Get image URL for a race by name or ID.
     * First tries exact match, then partial match.
     */
    fun getImageUrl(raceName: String, raceId: String? = null): String? {
        val normalizedName = raceName.lowercase().trim()

        // Try exact match
        raceImages[normalizedName]?.let { return it }

        // Try partial match (race name contains key)
        for ((key, url) in raceImages) {
            if (normalizedName.contains(key) || key.contains(normalizedName)) {
                return url
            }
        }

        // Try matching by ID patterns
        raceId?.lowercase()?.let { id ->
            when {
                id.contains("tdf") || id.contains("tour-de-france") -> return raceImages["tour de france"]
                id.contains("giro") -> return raceImages["giro d'italia"]
                id.contains("vuelta") -> return raceImages["vuelta a espana"]
                id.contains("paris-roubaix") -> return raceImages["paris-roubaix"]
                id.contains("sanremo") || id.contains("milano-sanremo") -> return raceImages["milan-sanremo"]
                id.contains("flanders") || id.contains("vlaanderen") -> return raceImages["tour of flanders"]
                id.contains("liege") -> return raceImages["liege-bastogne-liege"]
                id.contains("lombardia") -> return raceImages["il lombardia"]
            }
        }

        return null
    }
}
