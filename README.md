# NagiTube

NagiTube is a Kotlin + Jetpack Compose Android app using the official YouTube Data API v3 for public video search/metadata and YouTube's official embedded player for playback.

## Current features

- Compose dark/light theme and Home, Search, Shorts-style, Music, Library, and Settings tabs.
- Search public YouTube videos and display thumbnails, titles, channel names, publish dates, and descriptions.
- Embedded YouTube playback with YouTube-provided controls and supported fullscreen/captions options.
- Recent searches, category chips, loading/error/retry states.
- Local Watch Later, favorites, and watch-history IDs; share a YouTube watch link.
- Basic keyword content filter (not a full parental control).

## Configure the YouTube Data API

1. Open Google Cloud Console, select a project, enable YouTube Data API v3, and create an API key.
2. Restrict the key to YouTube Data API v3. For release builds, configure Android application restrictions for package `com.nagidev360.nagitube` and the signing certificate SHA-1. Never publish an unrestricted key.
3. For local builds, add this line to root `local.properties` (do not commit that file):

   `YOUTUBE_API_KEY=YOUR_RESTRICTED_API_KEY`

4. For GitHub Actions, add a repository Actions secret named `YOUTUBE_API_KEY`. Gradle reads this environment variable. The app compiles without a key, but online search reports a configuration error until one is supplied.

**Security:** A key embedded in an app can be extracted. Restrict it by API and Android signing identity, monitor quota, and rotate if exposed. A trusted backend can provide stronger quota protection.

## Build an APK

### Android Studio
1. Install Android Studio, Android SDK Platform 35, and JDK 17.
2. Clone this repository and open it in Android Studio.
3. Configure the API key as above and wait for Gradle sync.
4. Choose **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
5. APK path: `app/build/outputs/apk/debug/app-debug.apk`.

### GitHub Actions
1. Push to `main`/`master`, or open **Actions → Android Debug APK → Run workflow**.
2. Wait for the build job to pass and download the `NagiTube-debug-apk` artifact. Artifacts are retained for 14 days.

## Not implemented yet

This is an actively developed app, not a complete YouTube client replacement. Google/YouTube OAuth sign-in, authenticated like/subscribe/comment actions, full channel pages, accurate view-count statistics, server-backed recommendations, and account syncing are not enabled. Shorts is currently a short-video search feed, not an infinite swipe player. Watch Later/favorites/history persist IDs locally; metadata is only available for videos loaded in the current feed. The keyword filter is not a substitute for YouTube Restricted Mode or device-level parental controls.

## Playback and policy

Playback uses YouTube's official embedded player and does not download, scrape, or bypass restrictions. Some videos disable embedding, require sign-in/age verification, or are blocked by region, and therefore may not play inside the app. Authenticated actions require supported official APIs and user authorization; this app does not simulate them.


## Creator/community backend foundation

A Supabase SQL migration is available at [`supabase/migrations/202610090001_community_creator.sql`](supabase/migrations/202610090001_community_creator.sql). It defines profiles, creator channels, uploaded-video metadata, likes, comments, subscriptions, Row Level Security policies, and private Storage-folder policies. Setup instructions: [`docs/SUPABASE_BACKEND_SETUP.md`](docs/SUPABASE_BACKEND_SETUP.md).

**Important:** these backend files establish the database and access-control foundation; they do not yet connect the Android UI to Supabase. Login, live likes/comments/subscriptions, creator upload screens, video processing/transcoding, and account-synced feeds require client and server integration. Do not treat the SQL migration alone as a complete production video-hosting service.

## Project structure

- `app/src/main/java/com/nagidev360/nagitube/MainActivity.kt` — Compose UI, player, local library, settings.
- `app/src/main/java/com/nagidev360/nagitube/YouTubeDataApi.kt` — official YouTube Data API v3 search client.
- `.github/workflows/android-build.yml` — CI debug APK build.

## Testing status

GitHub Actions builds and verifies the APK artifact. CI success is not a substitute for physical-device testing of playback, API-key restrictions, fullscreen, captions, and accessibility before release.
