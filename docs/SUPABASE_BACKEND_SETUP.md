# NagiTube backend setup (Supabase)

This repository currently uses the official YouTube Data API v3 for public YouTube search and the official YouTube embedded player. This migration adds a secure database foundation for NagiTube-owned creator channels, uploaded-video metadata, likes, comments, and subscriptions. It does not turn YouTube-hosted videos into NagiTube uploads.

## 1. Create backend
1. Create a Supabase project.
2. In **SQL Editor**, run `supabase/migrations/202610090001_community_creator.sql`.
3. In **Authentication → Providers**, enable the login methods you want (email/password is a good starting point); configure email confirmation and redirect URLs.
4. In **Storage**, create a private bucket named `creator-videos`. Configure file-size and MIME-type limits before accepting uploads.
5. Configure thumbnail storage separately if needed. Do not make the video bucket public by default.

## 2. Required client integration (next app step)
- Add the Supabase Kotlin client or call a trusted backend using a maintained Android networking/auth stack.
- Supply only the Supabase project URL and publishable/anon key in the Android app. Never ship `service_role`, OAuth client secrets, or server secrets.
- Use Supabase Auth for sessions; the migration creates a profile row on sign-up.
- Read the public feed from `videos` where `visibility=public` and `processing_status=ready`; fetch channel metadata from `channels`.
- Like/unlike with insert/delete in `video_likes`; comments in `comments`; subscribe/unsubscribe in `subscriptions`. RLS policies enforce user ownership.
- Upload videos to the private `creator-videos` bucket under `<auth-user-uuid>/<filename>`, then create the corresponding `videos` row. A production service should validate, transcode, scan, and generate thumbnails before setting `processing_status=ready`. Do not mark an upload ready just because the raw file upload succeeded.

## 3. Upload and moderation safety
Video upload requires resumable/retryable uploads, size/type limits, transcoding/streaming formats, thumbnail generation, report/block tools, copyright process, abuse moderation, and a retention policy. This SQL is the data foundation, not a complete video-hosting/transcoding service. Add server-side validation before public playback.

## 4. Playback policy
NagiTube continues to use YouTube's official embedded player for YouTube-hosted videos. PiP depends on Android version/device and the embedded player; test it on-device. Do not force playback after the user stops it, extract media streams, suppress ads, or bypass YouTube's background-playback rules. For NagiTube-owned uploads, compliant background audio can be implemented only for content NagiTube is authorized to stream, with a foreground media service and visible playback controls/notification.

## 5. Build verification
A commit is not a build result. Check **Actions → Android Debug APK** for the latest branch run, confirm the build and APK-verification steps passed, and test playback on a real device. A successful Gradle build does not prove OAuth, Storage upload, PiP, or media-service behavior works end-to-end.
