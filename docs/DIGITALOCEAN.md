# Deploying Sanad to DigitalOcean

## Architecture

```
Internet
  │
  ├── sanad-api.promatrix.ai    ──┐
  └── sanad-portal.promatrix.ai ──┤
                                  ▼
                           Cloudflare (SSL)
                                  │ HTTP
                                  ▼
                    DigitalOcean Droplet ($12/mo)
                    ┌─────────────────────────────┐
                    │  nginx (:80)                │
                    │  ├── api host → backend     │
                    │  └── portal host            │
                    │      ├── / → portal         │
                    │      ├── /admin/* → backend  │
                    │      └── /api/* → backend    │
                    │                             │
                    │  backend (:8080, Spring)    │
                    │  portal  (:8080, nginx)     │
                    │  mysql   (:3306, internal)  │
                    └─────────────────────────────┘
```

The portal's `/admin/*` and `/api/*` calls are proxied to the backend by nginx — same-origin from the browser's perspective, so **no CORS is needed** for the portal. Banks access the backend directly via `sanad-api.promatrix.ai`.

---

## Prerequisites

- DigitalOcean account
- Domain `promatrix.ai` on Cloudflare
- GitHub repo: `shijazi88/authentication_platform`

---

## Step 1 — Create the Droplet

1. https://cloud.digitalocean.com → **Create → Droplets**
2. Settings:
   - **Region**: Singapore (closest to Yemen)
   - **Image**: Ubuntu 24.04 LTS
   - **Size**: Basic → **$12/mo** (2 GB RAM / 1 vCPU / 50 GB)
   - **Auth**: SSH key (add your laptop's `~/.ssh/id_rsa.pub`)
   - **Hostname**: `sanad-prod`
3. Click **Create** → copy the **IPv4 address**

---

## Step 2 — Cloudflare DNS

In Cloudflare dashboard → `promatrix.ai` → **DNS → Records** → add:

| Type | Name | Content | Proxy | TTL |
|---|---|---|---|---|
| A | `sanad-api` | `<Droplet IP>` | Proxied (orange) | Auto |
| A | `sanad-portal` | `<Droplet IP>` | Proxied (orange) | Auto |

Then go to **SSL/TLS** → set mode to **Full** (not Flexible, not Strict).

---

## Step 3 — Set up the Droplet

SSH in and run the setup script:

```bash
ssh root@<DROPLET_IP>
curl -sL https://raw.githubusercontent.com/shijazi88/authentication_platform/main/scripts/setup-droplet.sh | bash
```

This installs Docker, configures the firewall, clones the repo to `/opt/sanad`, and creates `.env` from `.env.example`.

---

## Step 4 — Configure secrets

```bash
nano /opt/sanad/.env
```

Fill in real values:

```env
DB_ROOT_PASSWORD=<generate: openssl rand -base64 24>
DB_NAME=sanad
DB_USERNAME=sanad_app
DB_PASSWORD=<generate: openssl rand -base64 24>

JWT_SECRET=<generate: openssl rand -hex 32>

YEMEN_ID_BASE_URL=https://the-real-yemen-id-api.com/base_path
YEMEN_ID_BEARER_TOKEN=<your provider token>

BOOTSTRAP_ADMIN_ENABLED=true
BOOTSTRAP_ADMIN_EMAIL=admin@promatrix.ai
BOOTSTRAP_ADMIN_PASSWORD=<strong temporary password>
```

---

## Step 5 — Build and start (first deploy)

For the very first deploy, build the images on the Droplet directly:

```bash
cd /opt/sanad

# Build backend image
docker build -t ghcr.io/shijazi88/sanad-backend:latest .

# Build portal image
docker build -t ghcr.io/shijazi88/sanad-portal:latest ./portal-admin

# Start everything
docker compose -f docker-compose.prod.yml up -d

# Watch the backend boot
docker compose -f docker-compose.prod.yml logs -f backend
```

You should see:
```
Successfully applied 8 migrations
Bootstrap admin user created: admin@promatrix.ai
Started PlatformApplication in 3.xx seconds
```

---

## Step 6 — Verify

```bash
# From your laptop
curl https://sanad-api.promatrix.ai/actuator/health
# {"status":"UP"}

# Open the portal
open https://sanad-portal.promatrix.ai
```

Log in with the bootstrap admin credentials → create a real admin → then:

```bash
# On the Droplet: disable bootstrap admin
cd /opt/sanad
sed -i 's/BOOTSTRAP_ADMIN_ENABLED=true/BOOTSTRAP_ADMIN_ENABLED=false/' .env
docker compose -f docker-compose.prod.yml up -d backend
```

---

## Step 7 — Set up CI/CD (auto-deploy on git push)

### GitHub Secrets

In your GitHub repo → **Settings → Secrets and variables → Actions → New repository secret**, add:

| Secret name | Value |
|---|---|
| `DROPLET_IP` | Your Droplet's IPv4 address |
| `DROPLET_USER` | `root` |
| `DROPLET_SSH_KEY` | The **private** SSH key that can access the Droplet. Generate a dedicated deploy key: `ssh-keygen -t ed25519 -f ~/.ssh/sanad_deploy -N ""`. Add the public key to the Droplet's `~/.ssh/authorized_keys`. Paste the private key here. |

### How it works

Every `git push` to `main` triggers `.github/workflows/deploy.yml`:
1. Builds both Docker images on GitHub's runners
2. Pushes them to GitHub Container Registry (`ghcr.io`)
3. SSHs into the Droplet
4. Pulls the new images and restarts the containers

### Test the pipeline

```bash
# On your laptop — any small change triggers a deploy
cd "/Users/rushai/Documents/work/authentication middleware"
echo "" >> README.md
git add -A && git commit -m "test: trigger CI/CD" && git push
```

Watch the **Actions** tab on GitHub — you should see the workflow run.

---

## Useful commands (on the Droplet)

```bash
cd /opt/sanad

# Status
docker compose -f docker-compose.prod.yml ps

# Logs
docker compose -f docker-compose.prod.yml logs -f backend
docker compose -f docker-compose.prod.yml logs -f portal
docker compose -f docker-compose.prod.yml logs -f nginx

# Restart a single service
docker compose -f docker-compose.prod.yml restart backend

# Full restart
docker compose -f docker-compose.prod.yml down && docker compose -f docker-compose.prod.yml up -d

# MySQL shell
docker exec -it sanad-mysql mysql -u sanad_app -p sanad

# Rebuild after code change (manual, if not using CI/CD)
git pull origin main
docker build -t ghcr.io/shijazi88/sanad-backend:latest .
docker build -t ghcr.io/shijazi88/sanad-portal:latest ./portal-admin
docker compose -f docker-compose.prod.yml up -d
```

---

## Cost

| Resource | Cost |
|---|---|
| Droplet (2 GB RAM) | $12/mo |
| Cloudflare (Free plan) | $0 |
| Domain (`promatrix.ai` — already owned) | $0 |
| GitHub Container Registry (free for public repos) | $0 |
| **Total** | **$12/mo** |

---

## Scaling later

When traffic grows past what a single Droplet can handle:

1. **Vertical**: Resize the Droplet to 4 GB RAM / 2 vCPU ($24/mo) — handles 10x more traffic.
2. **Horizontal**: Split MySQL onto a managed DigitalOcean Database ($15/mo) + add a second Droplet behind a DigitalOcean Load Balancer ($12/mo). Total ~$39/mo but fault-tolerant.
3. **Kubernetes**: Migrate to DigitalOcean Kubernetes (DOKS) when you need auto-scaling. The Docker images and configs work as-is — just swap `docker compose` for Helm charts.
