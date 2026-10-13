# NagiTube Web Sign-In

A small static sign-in page using Supabase Auth + Google OAuth. It is a companion web page; the Android app remains the native app.

## Enable web Google sign-in
1. Create/configure a Supabase project and enable **Authentication → Providers → Google**.
2. In Google Cloud Console, configure the OAuth consent screen and create a Web OAuth client. Add the redirect URI shown by Supabase's Google provider settings (the Supabase Auth callback URL).
3. In Supabase **Authentication → URL Configuration**, add your deployed website origin to Site URL / Redirect URLs.
4. Edit `web/index.html`: replace `YOUR_SUPABASE_URL` and `YOUR_SUPABASE_ANON_KEY`. The anon/publishable key is intended for client apps; never put a service-role key in the website.
5. Deploy the `web/` directory as a static site (for example, Render Static Site). HTTPS is required for production.

## Enable Android Google sign-in
Set GitHub Actions secrets `SUPABASE_URL` and `SUPABASE_ANON_KEY`. In Supabase URL Configuration, allow the deep link `nagitube://auth/callback`. Enable Google provider and set its client ID/secret in Supabase. This app uses Supabase OAuth + PKCE; it does not need a Google client secret embedded in the APK.

## AdMob setup (Android)
1. Create an AdMob account and register the Android app package `com.nagidev360.nagitube`.
2. Create a Banner ad unit.
3. Add GitHub Actions repository secrets:
   - `ADMOB_APP_ID` — the AdMob **App ID** (format `ca-app-pub-...~...`)
   - `ADMOB_BANNER_AD_UNIT_ID` — the Banner **Ad unit ID** (format `ca-app-pub-.../...`)
4. The project defaults to Google's official test App ID and test banner unit so development builds can safely show test ads. Replace these with your own IDs only after registering the app. Never click your own live ads. Follow AdMob consent/privacy requirements for your audience and region.

The current app displays a banner ad on the main browsing screens, not over the video player. This is an integration baseline, not proof of AdMob approval or revenue. Build and test the app on a device before release.
