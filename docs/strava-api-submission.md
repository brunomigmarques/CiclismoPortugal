# Strava API Submission - Ciclismo Portugal

## App Information for Strava Review

### App Name
Ciclismo Portugal

### App Description (for Strava submission)

```
Ciclismo Portugal is a Portuguese cycling events app that helps cyclists discover and track cycling races (provas) across Portugal, including road cycling, MTB, and gravel events.

HOW WE USE STRAVA DATA:

1. EVENT HISTORY TRACKING
   - Users add cycling events to their personal calendar
   - After an event date passes, users can view their history
   - We fetch Strava activities ONLY for dates when the user had a registered event
   - This allows users to see their ride stats (distance, time, elevation, speed) alongside the event details

2. RACE DAY ACTIVITY DISPLAY
   - On the event details screen, we show the user's Strava activity for that specific race day
   - This creates a complete picture: event info + actual ride data

DATA USAGE:
- We ONLY access cycling activities (Ride, VirtualRide, GravelRide, MountainBikeRide)
- We ONLY fetch activities for dates the user has events in their calendar
- We display: distance, moving time, elevation gain, average speed, average heart rate
- Data is displayed within the app UI only - NOT exported or shared

PRIVACY:
- Strava tokens stored locally on user's device only
- Users can disconnect Strava anytime from Profile settings
- No Strava data is stored on our servers
- Full privacy policy: [Your hosted URL]

The app enhances the cycling event experience by connecting what users planned (events) with what they actually rode (Strava activities).
```

### Category
Health & Fitness / Sports

### Platform
Android (Kotlin, Jetpack Compose)

### Scopes Requested
- `read` - Access basic profile information
- `activity:read` - Access activity data (read-only)

### Screenshots to Include

1. **Profile Screen** - Shows "Serviços Ligados" section with Strava connection button
2. **Event Details Screen** - Shows Strava activity card with ride stats
3. **Event History Screen** - Shows past events with Strava mini-stats

### Privacy Policy URL
Host the privacy-policy.md file and provide the URL (e.g., on GitHub Pages or your website)

### Callback URL
`ciclismoportugal://strava/callback`

---

## Checklist Before Submission

- [ ] Privacy policy hosted and accessible via URL
- [ ] App description clearly explains Strava data usage
- [ ] Screenshots showing Strava integration in the app
- [ ] Callback URL matches AndroidManifest.xml
- [ ] Terms of service (optional but recommended)

## Recommended Hosting for Privacy Policy

Option 1: GitHub Pages
1. Enable GitHub Pages for your repo
2. Access at: https://[username].github.io/CiclismoPortugal/docs/privacy-policy

Option 2: Firebase Hosting
1. Deploy to Firebase Hosting
2. Access at: https://[project-id].web.app/privacy-policy

Option 3: Google Sites
1. Create a simple Google Site
2. Copy/paste the privacy policy content
