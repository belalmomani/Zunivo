# Zunivo - Privacy policy (DRAFT)

> Draft describing what the app does today. Not legal advice; it does not claim compliance with any law or store policy.
> Review it, fill in the contact details, and update it whenever features change, before publishing.

**Developer / contact:** _(fill in)_   **Effective date:** _(fill in)_

## What stays on the phone
Favorites, folders, packs, saved edits, imported GitHub projects (with downloaded files), preferences and the language choice are kept in the app's private storage. There are no accounts and no server-side copy. Everything is removed by *Settings > Storage and data > Delete all local data* or by uninstalling.

## What is sent
| Feature | Sent to | Data |
|---|---|---|
| Icon search, icon images, exports | Iconify API (api.iconify.design) | search words, icon names, requested colors/rotation |
| My store import / refresh | GitHub (api.github.com and GitHub file hosts) | the public repository you entered |
| App update check | GitHub Releases of the Zunivo repository | an unauthenticated request |
| Image to icon set | nothing (the image never leaves the phone) | only category keywords go to Iconify |
| Ads (only if enabled in the build) | Start.io | what the SDK collects under Start.io's terms, e.g. advertising ID; it shows its own consent notice |

These services can see the device's IP address and request details, as any website can.

## Permissions
`INTERNET`; `REQUEST_INSTALL_PACKAGES` (only to open Android's installer for an APK you chose to install). The CI job `permissions.txt` lists every permission in the merged manifest, including those added by libraries - review it before release.

## Your choices
- *My preferences*: turn learning off, keep or delete search history, clear what was learned.
- *Storage and data*: clear cache; delete all local data.
- Ads can be removed entirely by building without a Start.io App ID.

## Third-party content
Icons come from sets with different licenses. Zunivo shows each icon's author and license, keeps them in exports, and excludes icons with unknown or restrictive licenses from automatic pack export.

## Children, data safety forms, retention
_(to complete before publishing: age rating, Google Play Data safety answers, advertising-ID declaration, retention statement for Start.io data)_
