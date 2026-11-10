# Setting up designer pages (Supabase)

The app only needs the **project URL** and the **public anon key**. Never put the `service_role` key in the app, the repository or CI logs.

1. Create a project at supabase.com.
2. SQL editor: run `docs/supabase/schema.sql` (tables, Row Level Security, the download-count view).
3. Authentication > Providers: keep **Email** enabled. Authentication > Email templates: edit the *Magic Link* template (and *Confirm signup*) so the mail contains the
   6-digit code (`{{ .Token }}`), because the app signs in with a one-time code, not a link. Keep Supabase's email rate limits on.
4. Settings > API: copy **Project URL** and the **anon public** key.
5. Build with them: `SUPABASE_URL=https://xxxx.supabase.co SUPABASE_ANON_KEY=... gradle :app:assembleDebug`
   (or `-PSUPABASE_URL=... -PSUPABASE_ANON_KEY=...`; in GitHub add repository secrets `SUPABASE_URL` and `SUPABASE_ANON_KEY`).
   Without both values the designer section shows "not connected" and nothing is sent anywhere.

## What the rules guarantee (see the SQL)
- Anyone can read profiles and **public** works; private works are readable only by their owner.
- Only the signed-in owner can create, change or delete their own profile and works. Nobody can edit another person's work.
- A user can record at most one download per public work; counts come only from real rows.
- Any signed-in user can file a copyright report; reports cannot be read through the app API.

## Reviewing copyright reports
Open Table editor > `copyright_reports` in the Supabase dashboard (it uses your admin access). Set `status` to `reviewing`, `removed` or `rejected`;
to take a work down set its `visibility` to `private` or delete the row.

## Deleting an account completely
The app's "Delete my designer page" removes the profile, works and download events. The sign-in account itself (email) is removed from
Authentication > Users in the dashboard (this needs admin rights, so it cannot be done from the app).

## Not verified
This code was written without a live Supabase project; the REST/Auth calls follow Supabase's documented endpoints but have never been run.
