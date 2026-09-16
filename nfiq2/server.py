#!/usr/bin/env python3
"""HTTP front for the NIST NFIQ 2 command-line tool.

POST /score   body = raw image bytes (PNG or WSQ), Content-Type image/png | image/x-wsq | application/octet-stream
              -> 200 {"score": 61, "ms": 412}                       image scored
              -> 200 {"score": null, "error": "...", "ms": 380}     NFIQ 2 could not score it (blank, too small, palette PNG…)
              -> 400 {"error": "..."}                                 empty body / too large
GET  /health  -> 200 {"status":"UP","nfiq2":"2.3.0"}

Standard library only. One nfiq2 process per request (≈100 ms of scoring plus
model load); images are written to a tmpfs and deleted immediately.
"""
import json
import os
import re
import subprocess
import tempfile
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

PORT = int(os.environ.get("NFIQ2_PORT", "8090"))
BIN = os.environ.get("NFIQ2_BIN", "/usr/local/bin/nfiq2")
TIMEOUT_S = float(os.environ.get("NFIQ2_TIMEOUT_S", "8"))
MAX_BYTES = int(os.environ.get("NFIQ2_MAX_BYTES", str(8 * 1024 * 1024)))
WORK = os.environ.get("NFIQ2_WORK", "/tmp/nfiq2")

_VERSION = None


def nfiq2_version():
    global _VERSION
    if _VERSION is None:
        try:
            out = subprocess.run([BIN], capture_output=True, text=True, timeout=10).stdout
            m = re.search(r"NFIQ 2:\s*([\d.]+)", out)
            _VERSION = m.group(1) if m else "unknown"
        except Exception:
            _VERSION = "unavailable"
    return _VERSION


def score_image(data: bytes, content_type: str):
    """Run nfiq2 on one image; returns (score|None, error|None)."""
    suffix = ".wsq" if (data[:2] == b"\xff\xa0" or "wsq" in content_type) else ".png"
    fd, path = tempfile.mkstemp(prefix="fp-", suffix=suffix, dir=WORK)
    try:
        with os.fdopen(fd, "wb") as f:
            f.write(data)
        # -F: never prompt. -v: CSV with QualityScore + OptionalError columns.
        proc = subprocess.run([BIN, "-F", "-v", "-i", path], capture_output=True, text=True, timeout=TIMEOUT_S)
        lines = [l for l in proc.stdout.splitlines() if l.strip()]
        header = next((l for l in lines if l.startswith('"Filename"')), None)
        row = next((l for l in lines if l.startswith('"' + path.split("/")[-1]) or l.startswith('"' + path)), None)
        if header is None or row is None:
            tail = (proc.stderr or proc.stdout).strip().splitlines()[-1:] or ["no output"]
            return None, f"nfiq2 produced no result: {tail[0][:200]}"
        cols = header.split(",")
        vals = next(csv_split(row))
        rec = dict(zip([c.strip('"') for c in cols], vals))
        err = rec.get("OptionalError", "NA")
        q = rec.get("QualityScore", "NA")
        if q not in ("NA", "", None):
            return int(float(q)), None
        return None, clean_error(err)
    finally:
        try:
            os.unlink(path)
        except OSError:
            pass


def csv_split(line: str):
    """Minimal CSV splitter honouring double quotes (nfiq2 quotes filename and error)."""
    out, cur, inq = [], [], False
    for ch in line:
        if ch == '"':
            inq = not inq
        elif ch == "," and not inq:
            out.append("".join(cur)); cur = []
        else:
            cur.append(ch)
    out.append("".join(cur))
    yield out


def clean_error(err: str) -> str:
    """Turn NFIQ 2's internal error text into one plain sentence."""
    e = err or "unknown error"
    e = e.replace("Error: ", "").replace("NFIQ2 computeUnifiedQualityScore returned an error code: ", "")
    e = e.replace("Could not create feature set from raw data: ", "")
    e = re.sub(r"FRFXLL_ERR_\w+:\s*", "", e)
    if "Not enough data to generate histogram" in e or "blank" in e.lower():
        return "image appears blank"
    if "too small" in e.lower():
        return "fingerprint area is too small (tip of the finger or partial capture)"
    if "palette" in e.lower():
        return "indexed/palette PNG is not supported; use 8-bit greyscale"
    if "Could not open image" in e:
        return "image could not be decoded"
    return e.strip()[:200]


class Handler(BaseHTTPRequestHandler):
    server_version = "nfiq2-sidecar/1.0"

    def log_message(self, fmt, *args):  # quieter access log
        if os.environ.get("NFIQ2_ACCESS_LOG"):
            super().log_message(fmt, *args)

    def _json(self, status: int, body: dict):
        data = json.dumps(body).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def do_GET(self):
        if self.path.startswith("/health"):
            v = nfiq2_version()
            return self._json(200 if v not in ("unavailable",) else 503, {"status": "UP" if v != "unavailable" else "DOWN", "nfiq2": v})
        self._json(404, {"error": "not found"})

    def do_POST(self):
        if not self.path.startswith("/score"):
            return self._json(404, {"error": "not found"})
        length = int(self.headers.get("Content-Length") or 0)
        if length <= 0:
            return self._json(400, {"error": "empty body"})
        if length > MAX_BYTES:
            return self._json(400, {"error": f"image larger than {MAX_BYTES} bytes"})
        data = self.rfile.read(length)
        t0 = time.time()
        try:
            score, err = score_image(data, self.headers.get("Content-Type", ""))
        except subprocess.TimeoutExpired:
            return self._json(504, {"error": f"nfiq2 timed out after {TIMEOUT_S:.0f}s"})
        except Exception as ex:  # never let one bad image take the service down
            return self._json(500, {"error": f"scoring failed: {str(ex)[:200]}"})
        ms = int((time.time() - t0) * 1000)
        self._json(200, {"score": score, "error": err, "ms": ms})


if __name__ == "__main__":
    os.makedirs(WORK, mode=0o700, exist_ok=True)
    print(f"nfiq2 sidecar listening on :{PORT} (nfiq2 {nfiq2_version()})", flush=True)
    ThreadingHTTPServer(("0.0.0.0", PORT), Handler).serve_forever()
