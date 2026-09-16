# NFIQ 2 quality-score sidecar for the MOTABIQ platform.
#
# Wraps NIST's NFIQ 2 (v2.3.0, public domain) command-line tool in a tiny HTTP
# service. The backend POSTs a fingerprint image (PNG or WSQ) and receives the
# 0–100 quality score. Nothing is persisted; images live only in a tmpfs for the
# duration of one call.
FROM ubuntu:24.04

ARG NFIQ2_VERSION=2.3.0
ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update -qq \
 && apt-get install -y -qq --no-install-recommends ca-certificates curl python3 \
 && curl -sSL -o /tmp/nfiq2.deb \
      "https://github.com/usnistgov/NFIQ2/releases/download/v${NFIQ2_VERSION}/nfiq2_${NFIQ2_VERSION}-1_ubuntu24_amd64.deb" \
 && apt-get install -y -qq --no-install-recommends /tmp/nfiq2.deb \
 && rm -f /tmp/nfiq2.deb \
 && apt-get purge -y -qq curl \
 && apt-get autoremove -y -qq \
 && rm -rf /var/lib/apt/lists/*

COPY server.py /app/server.py

# Unprivileged user. Images are written under /tmp (compose mounts a tmpfs
# there) and deleted right after scoring.
RUN useradd -r -s /usr/sbin/nologin nfiq2
USER nfiq2
WORKDIR /tmp

ENV NFIQ2_PORT=8090 NFIQ2_BIN=/usr/local/bin/nfiq2 NFIQ2_TIMEOUT_S=8 NFIQ2_WORK=/tmp/nfiq2
EXPOSE 8090
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD python3 -c "import urllib.request,sys; sys.exit(0 if urllib.request.urlopen('http://127.0.0.1:8090/health', timeout=4).status == 200 else 1)"

CMD ["python3", "/app/server.py"]
