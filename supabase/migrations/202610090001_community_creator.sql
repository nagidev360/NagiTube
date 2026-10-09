-- NagiTube creator/community backend foundation for Supabase.
-- Run in Supabase SQL Editor. Configure Auth providers and Storage bucket separately.
-- Never place a Supabase service_role key in the Android app.

create extension if not exists pgcrypto;

create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  username text unique not null check (char_length(username) between 3 and 30),
  display_name text not null default '',
  avatar_url text,
  bio text not null default '' check (char_length(bio) <= 500),
  created_at timestamptz not null default now()
);

create table if not exists public.channels (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null unique references auth.users(id) on delete cascade,
  name text not null check (char_length(name) between 1 and 80),
  handle text unique not null check (handle ~ '^[a-zA-Z0-9_]{3,30}$'),
  description text not null default '' check (char_length(description) <= 2000),
  avatar_url text,
  banner_url text,
  created_at timestamptz not null default now()
);

create table if not exists public.videos (
  id uuid primary key default gen_random_uuid(),
  channel_id uuid not null references public.channels(id) on delete cascade,
  title text not null check (char_length(title) between 1 and 150),
  description text not null default '' check (char_length(description) <= 5000),
  video_path text not null,
  thumbnail_path text,
  visibility text not null default 'public' check (visibility in ('public','unlisted','private')),
  processing_status text not null default 'pending' check (processing_status in ('pending','ready','failed')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.video_likes (
  user_id uuid not null references auth.users(id) on delete cascade,
  video_id uuid not null references public.videos(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (user_id, video_id)
);

create table if not exists public.comments (
  id uuid primary key default gen_random_uuid(),
  video_id uuid not null references public.videos(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  body text not null check (char_length(body) between 1 and 2000),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.subscriptions (
  subscriber_id uuid not null references auth.users(id) on delete cascade,
  channel_id uuid not null references public.channels(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (subscriber_id, channel_id)
);

create index if not exists videos_feed_idx on public.videos (created_at desc)
  where visibility = 'public' and processing_status = 'ready';
create index if not exists videos_channel_idx on public.videos (channel_id, created_at desc);
create index if not exists comments_video_idx on public.comments (video_id, created_at desc);
create index if not exists subscriptions_channel_idx on public.subscriptions (channel_id, created_at desc);

-- Create a profile when a user signs up. A username can be changed later by the user.
create or replace function public.handle_new_user()
returns trigger language plpgsql security definer set search_path = ''
as $$
declare
  candidate text;
begin
  candidate := lower(regexp_replace(coalesce(new.raw_user_meta_data ->> 'user_name',
      split_part(coalesce(new.email, 'creator'), '@', 1)), '[^a-zA-Z0-9_]', '', 'g'));
  if char_length(candidate) < 3 then candidate := 'user' || substr(replace(new.id::text, '-', ''), 1, 12); end if;
  candidate := substr(candidate, 1, 22) || '_' || substr(replace(new.id::text, '-', ''), 1, 6);
  insert into public.profiles(id, username, display_name)
  values (new.id, candidate, coalesce(new.raw_user_meta_data ->> 'full_name', split_part(coalesce(new.email, 'Creator'), '@', 1)))
  on conflict (id) do nothing;
  return new;
end;
$$;

drop trigger if exists on_auth_user_created_nagitube on auth.users;
create trigger on_auth_user_created_nagitube
  after insert on auth.users for each row execute procedure public.handle_new_user();

alter table public.profiles enable row level security;
alter table public.channels enable row level security;
alter table public.videos enable row level security;
alter table public.video_likes enable row level security;
alter table public.comments enable row level security;
alter table public.subscriptions enable row level security;

create policy "Profiles are viewable by signed-in users" on public.profiles
  for select to authenticated using (true);
create policy "Users create their own profile" on public.profiles
  for insert to authenticated with check (id = (select auth.uid()));
create policy "Users update their own profile" on public.profiles
  for update to authenticated using (id = (select auth.uid())) with check (id = (select auth.uid()));

create policy "Channels are publicly readable" on public.channels
  for select to anon, authenticated using (true);
create policy "Users create their own channel" on public.channels
  for insert to authenticated with check (owner_id = (select auth.uid()));
create policy "Owners update their channel" on public.channels
  for update to authenticated using (owner_id = (select auth.uid())) with check (owner_id = (select auth.uid()));
create policy "Owners delete their channel" on public.channels
  for delete to authenticated using (owner_id = (select auth.uid()));

create policy "Ready public videos are readable" on public.videos
  for select to anon, authenticated using (
    (visibility = 'public' and processing_status = 'ready')
    or exists (select 1 from public.channels c where c.id = channel_id and c.owner_id = (select auth.uid()))
  );
create policy "Channel owners upload video metadata" on public.videos
  for insert to authenticated with check (
    exists (select 1 from public.channels c where c.id = channel_id and c.owner_id = (select auth.uid()))
  );
create policy "Channel owners update video metadata" on public.videos
  for update to authenticated using (
    exists (select 1 from public.channels c where c.id = channel_id and c.owner_id = (select auth.uid()))
  ) with check (
    exists (select 1 from public.channels c where c.id = channel_id and c.owner_id = (select auth.uid()))
  );
create policy "Channel owners delete videos" on public.videos
  for delete to authenticated using (
    exists (select 1 from public.channels c where c.id = channel_id and c.owner_id = (select auth.uid()))
  );

create policy "Likes visible to everyone" on public.video_likes
  for select to anon, authenticated using (true);
create policy "Users like as themselves" on public.video_likes
  for insert to authenticated with check (user_id = (select auth.uid()));
create policy "Users remove their own likes" on public.video_likes
  for delete to authenticated using (user_id = (select auth.uid()));

create policy "Comments are readable" on public.comments
  for select to anon, authenticated using (true);
create policy "Signed-in users comment as themselves" on public.comments
  for insert to authenticated with check (user_id = (select auth.uid()));
create policy "Authors edit their own comments" on public.comments
  for update to authenticated using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));
create policy "Authors delete their own comments" on public.comments
  for delete to authenticated using (user_id = (select auth.uid()));

create policy "Subscriptions are readable" on public.subscriptions
  for select to authenticated using (true);
create policy "Users subscribe as themselves" on public.subscriptions
  for insert to authenticated with check (
    subscriber_id = (select auth.uid())
    and not exists (select 1 from public.channels c where c.id = channel_id and c.owner_id = (select auth.uid()))
  );
create policy "Users unsubscribe as themselves" on public.subscriptions
  for delete to authenticated using (subscriber_id = (select auth.uid()));

-- Storage setup: create private bucket "creator-videos" in the Dashboard.
-- Use per-user first path segment: <auth.uid()>/<filename>. Apply these policies only after creating it.
create policy "Creator uploads to own folder" on storage.objects
  for insert to authenticated with check (
    bucket_id = 'creator-videos' and (storage.foldername(name))[1] = (select auth.uid())::text
  );
create policy "Creators read own uploads" on storage.objects
  for select to authenticated using (
    bucket_id = 'creator-videos' and (storage.foldername(name))[1] = (select auth.uid())::text
  );
create policy "Creators update own uploads" on storage.objects
  for update to authenticated using (
    bucket_id = 'creator-videos' and (storage.foldername(name))[1] = (select auth.uid())::text
  ) with check (
    bucket_id = 'creator-videos' and (storage.foldername(name))[1] = (select auth.uid())::text
  );
create policy "Creators delete own uploads" on storage.objects
  for delete to authenticated using (
    bucket_id = 'creator-videos' and (storage.foldername(name))[1] = (select auth.uid())::text
  );
