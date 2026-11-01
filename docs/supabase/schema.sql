-- Zunivo designer pages: schema + Row Level Security. Run in the Supabase SQL editor.
-- The Android app only ever uses the public anon key. NEVER put the service_role key in the app or in git.

create table public.designers (
  id           uuid primary key references auth.users on delete cascade,
  display_name text not null check (char_length(display_name) between 2 and 60),
  bio          text check (char_length(bio) <= 500),
  avatar_url   text,
  specialties  text[] not null default '{}',
  created_at   timestamptz not null default now()
);

create table public.designer_works (
  id           uuid primary key default gen_random_uuid(),
  designer_id  uuid not null references public.designers on delete cascade,
  title        text not null check (char_length(title) between 2 and 80),
  description  text check (char_length(description) <= 1000),
  icon_ids     text[] not null default '{}',         -- "prefix:name", licenses are re-checked client-side
  visibility   text not null default 'private' check (visibility in ('private', 'public')),
  created_at   timestamptz not null default now(),
  updated_at   timestamptz not null default now()
);

-- Real download counts only: one row per actual download event.
create table public.download_events (
  id         bigint generated always as identity primary key,
  work_id    uuid not null references public.designer_works on delete cascade,
  user_id    uuid not null references auth.users on delete cascade,
  created_at timestamptz not null default now(),
  unique (work_id, user_id)
);

-- Copyright / abuse reports. Reviewed by you in the Supabase dashboard (service role), never readable by the public.
create table public.copyright_reports (
  id          bigint generated always as identity primary key,
  work_id     uuid references public.designer_works on delete set null,
  reporter_id uuid references auth.users on delete set null,
  reason      text not null check (char_length(reason) between 10 and 2000),
  status      text not null default 'open' check (status in ('open', 'reviewing', 'removed', 'rejected')),
  created_at  timestamptz not null default now()
);

alter table public.designers          enable row level security;
alter table public.designer_works     enable row level security;
alter table public.download_events    enable row level security;
alter table public.copyright_reports  enable row level security;

-- designers: anyone can read profiles; only the owner can write their own.
create policy "profiles are public"      on public.designers for select using (true);
create policy "owner inserts profile"    on public.designers for insert with check (auth.uid() = id);
create policy "owner updates profile"    on public.designers for update using (auth.uid() = id) with check (auth.uid() = id);
create policy "owner deletes profile"    on public.designers for delete using (auth.uid() = id);

-- works: public works are readable by everyone, private ones only by the owner; only the owner writes.
create policy "read public or own works" on public.designer_works for select
  using (visibility = 'public' or designer_id = auth.uid());
create policy "owner inserts works"      on public.designer_works for insert with check (designer_id = auth.uid());
create policy "owner updates works"      on public.designer_works for update using (designer_id = auth.uid()) with check (designer_id = auth.uid());
create policy "owner deletes works"      on public.designer_works for delete using (designer_id = auth.uid());

-- downloads: signed-in users may record their own download of a PUBLIC work; counts come from count(*).
create policy "record own download" on public.download_events for insert
  with check (user_id = auth.uid()
    and exists (select 1 from public.designer_works w where w.id = work_id and w.visibility = 'public'));
create policy "owner sees own work downloads" on public.download_events for select
  using (exists (select 1 from public.designer_works w where w.id = work_id and w.designer_id = auth.uid()));

-- reports: any signed-in user may file one; nobody can read them through the app API.
create policy "file a report" on public.copyright_reports for insert with check (reporter_id = auth.uid());
