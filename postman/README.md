# Sannad — Postman Collection

This folder contains a Postman collection and environment files that you can share with banks and fintechs integrating with the **Sannad Identity Verification Platform**.

## Files

| File | Purpose |
|---|---|
| `Sannad-Verification-API.postman_collection.json` | The collection — verify identity endpoints, auth, request-id automation, tests, sample responses, and inline documentation. Share this with every integrator. |
| `Sannad-Local-Dev.postman_environment.json` | Environment for `localhost:8080` (the dev backend). Use during integration testing on your laptop. |
| `Sannad-Staging.postman_environment.json` | Environment template for staging. Update `baseUrl` to your real staging URL before sharing. |
| `Sannad-Production.postman_environment.json` | Environment template for production. Update `baseUrl` to your real production URL before sharing. |

## What banks need to do (the message you send them)

> Hi {bank},
>
> Attached are the Postman files to integrate with the Sannad Identity Verification API.
>
> 1. **Import** `Sannad-Verification-API.postman_collection.json` and the environment file we sent you (e.g. `Sannad-Production.postman_environment.json`) into Postman.
> 2. Open the imported environment, paste the **`clientId`** and **`clientSecret`** we issued to you during onboarding (the `clientSecret` was shown to you exactly once — if you lost it, ask us to issue a new credential).
> 3. Open **Identity Verification → Verify identity** and click **Send**.
>
> The collection's documentation tab covers authentication, error codes, response shapes, and how the `X-Request-Id` correlation header works. If you hit any issue, send us the `requestId` and the timestamp and we'll trace it.
>
> — The Sannad team

## What's wired up at the collection level

- **HTTP Basic auth** — uses `{{clientId}}` / `{{clientSecret}}` from the environment, so individual requests inherit it automatically.
- **Pre-request script** — generates a fresh **UUID v7** and sets the `X-Request-Id` header on every call. Banks can override the `requestId` collection variable to inject their own trace id.
- **Test scripts** — every successful response is asserted against the canonical envelope (`transaction.id`, `transaction.timestamp`, `result`), and the `lastTransactionId` collection variable is automatically populated for chaining.
- **Saved example responses** — six examples saved per request (200 hit-only, 200 full data, 400, 401, 403, 429, 502) so banks can preview the shapes without hitting the API.

## How to import into Postman

1. Open Postman → click **Import** (top-left).
2. Drag in **all four files** at once (or import them one at a time).
3. Top-right of the Postman window, click the environment dropdown → pick **Sannad — Local Dev** (or whichever you imported).
4. Click **Sannad — Identity Verification API → Identity Verification → Verify identity**.
5. Fill in `clientId` / `clientSecret` in the environment.
6. Click **Send**.

You should get a `200 OK` from `localhost:8080` if your local backend is running.

## Updating the collection

When you add new bank-facing endpoints (e.g. `/api/v1/verify/passport`):
1. Add a new request inside the **Identity Verification** folder (or create a new folder).
2. Update the collection-level description with any new error codes or fields.
3. Add saved example responses for at least the happy path and the most common error.
4. Re-export from Postman → **Collections → ⋯ → Export → Collection v2.1** and overwrite `Sannad-Verification-API.postman_collection.json`.
5. Send the new file to your integrators (or keep it in a shared S3 bucket / SharePoint that they pull from).

## Versioning

This collection targets the API version **`/api/v1/...`**. If we ever ship a `v2`, the collection should be renamed `Sannad-Verification-API-v2.postman_collection.json` and shipped alongside v1 during the deprecation window so banks can migrate at their own pace.

## Sanity-check the collection JSON

Postman v11 will reject collections with malformed JSON. To validate before shipping:

```bash
python3 -m json.tool postman/Sannad-Verification-API.postman_collection.json > /dev/null && echo "✅ valid"
```

## Notes for the team

- `clientSecret` in the environment files is marked as `type: "secret"` so Postman masks it in the UI and URL bars.
- Saved example responses use realistic values that match what the WireMock stub returns in dev — keep them in sync if the canonical schema evolves.
- The pre-request script depends on `crypto.getRandomValues`, which is available in Postman v9.0+. We don't support older Postman.
