# NagiTube backend setup (Supabase)

This repository currently uses the official YouTube Data API v3 for public YouTube search and the official YouTube embedded player. This migration adds a secure database foundation for NagiTube-owned creator channels, uploaded-video metadata, likes, comments, and subscriptions. It does not turn YouTube-hosted videos into NagiTube uploads.

## 1. Create backend
1. Create a Supabase project.
2. In **SQL Editor**, run `supabase/migrations/202610090001_community_creator.sql`.
3. In **Authentication → Providers**, enable the login methods you want (email/password is a good starting point); configure email confirmation and redirect URLs.
4. In **Storage**, create a private bucket named `creator-videos`. Configure file-size and MIME-type limits before accepting uploads.
5. Configure thumbnail storage separately if needed. Do not make the video bucket public by default.

## 2. Android email login (implemented)
- The app now requires sign-in or account creation before showing Home, Search, Shorts, Music, Library, or Settings. Sign out is in Settings.
- In the project root, add these lines to `local.properties` for local builds (use your real Supabase project values):
  ```properties
  SUPABASE_URL=https://YOUR_PROJECT.supabase.co
  SUPABASE_ANON_KEY=YOUR_PUBLISHABLE_OR_ANON_KEY
  ```
- For GitHub Actions, add repository Actions secrets named `SUPABASE_URL` and `SUPABASE_ANON_KEY`. If absent, the APK can still build, but login displays a setup-required message.
- In Supabase **Authentication → Providers**, enable Email; configure email confirmation and your site/redirect settings. With confirmation enabled, users must confirm their email before signing in.
- Only use the publishable/anon key in the client. Never ship `service_role`, OAuth client secrets, or server secrets.
- Sessions are stored in app preferences and refreshed on launch using the Supabase refresh-token endpoint. The migration creates a profile row on sign-up.
- Read the public feed from `videos` where `visibility=public` and `processing_status=ready`; fetch channel metadata from `channels`.
- Like/unlike with insert/delete in `video_likes`; comments in `comments`; subscribe/unsubscribe in `subscriptions`. RLS policies enforce user ownership.
- Upload videos to the private `creator-videos` bucket under `<auth-user-uuid>/<filename>`, then create the corresponding `videos` row. A production service should validate, transcode, scan, and generate thumbnails before setting `processing_status=ready`. Do not mark an upload ready just because the raw file upload succeeded.

## 3. Community data integration\nThe login screen authenticates accounts, but it does not yet wire likes, comments, subscriptions, creator uploads, or the public feed to the Supabase tables. Those features still need authenticated API integration.\n\n## 4. Upload and moderation safety
Video upload requires resumable/retryable uploads, size/type limits, transcoding/streaming formats, thumbnail generation, report/block tools, copyright process, abuse moderation, and a retention policy. This SQL is the data foundation, not a complete video-hosting/transcoding service. Add server-side validation before public playback.

## 5. Playback policy
NagiTube continues to use YouTube's official embedded player for YouTube-hosted videos. PiP depends on Android version/device and the embedded player; test it on-device. Do not force playback after the user stops it, extract media streams, suppress ads, or bypass YouTube's background-playback rules. For NagiTube-owned uploads, compliant background audio can be implemented only for content NagiTube is authorized to stream, with a foreground media service and visible playback controls/notification.

## 6. Build verification
A commit is not a build result. Check **Actions → Android Debug APK** for the latest branch run, confirm the build and APK-verification steps passed, and test playback on a real device. A successful Gradle build does not prove OAuth, Storage upload, PiP, or media-service behavior works end-to-end.
