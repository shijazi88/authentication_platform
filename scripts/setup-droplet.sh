#!/bin/bash
# =============================================================================
# Sanad — One-time Droplet setup script.
#
# Run on a fresh Ubuntu 24.04 Droplet:
#   ssh root@<DROPLET_IP>
#   curl -sL https://raw.githubusercontent.com/shijazi88/authentication_platform/main/scripts/setup-droplet.sh | bash
#
# Or copy & paste this whole script into the Droplet's terminal.
# =============================================================================

set -euo pipefail

echo "──────────────────────────────────────────"
echo "  Sanad Droplet Setup"
echo "──────────────────────────────────────────"

# 1. System updates
echo "[1/6] Updating system packages..."
apt-get update -qq && apt-get upgrade -y -qq

# 2. Install Docker
echo "[2/6] Installing Docker..."
if ! command -v docker &>/dev/null; then
    curl -fsSL https://get.docker.com | sh
    systemctl enable docker
    systemctl start docker
fi
docker --version

# 3. Install Docker Compose plugin (if not bundled)
echo "[3/6] Checking Docker Compose..."
docker compose version || {
    apt-get install -y docker-compose-plugin
}

# 4. Firewall
echo "[4/6] Configuring firewall..."
ufw allow OpenSSH
ufw allow 80/tcp
ufw allow 443/tcp
ufw --force enable
ufw status

# 5. Create project directory
echo "[5/6] Setting up /opt/sanad..."
mkdir -p /opt/sanad
cd /opt/sanad

# Clone the repo (just the deployment files)
if [ ! -d ".git" ]; then
    git clone https://github.com/shijazi88/authentication_platform.git .
else
    git pull origin main
fi

# Create .env from example if it doesn't exist
if [ ! -f ".env" ]; then
    cp .env.example .env
    echo ""
    echo "╔════════════════════════════════════════════════════════════╗"
    echo "║  IMPORTANT: Edit /opt/sanad/.env with your real secrets  ║"
    echo "║  nano /opt/sanad/.env                                     ║"
    echo "╚════════════════════════════════════════════════════════════╝"
    echo ""
fi

# 6. Done
echo "[6/6] Setup complete!"
echo ""
echo "Next steps:"
echo "  1. Edit secrets:  nano /opt/sanad/.env"
echo "  2. Start stack:   cd /opt/sanad && docker compose -f docker-compose.prod.yml up -d"
echo "  3. Check status:  docker compose -f docker-compose.prod.yml ps"
echo "  4. View logs:     docker compose -f docker-compose.prod.yml logs -f backend"
echo ""
echo "DNS: Point these to this Droplet's IP ($(curl -4s ifconfig.me)):"
echo "  sanad-api.promatrix.ai    → A record"
echo "  sanad-portal.promatrix.ai → A record"
echo ""
