/**
 * Holds the short-lived PIN unlock token for the Transactions page. Kept in
 * sessionStorage so it survives in-tab navigation but not a new session, and
 * sent as the X-Txn-Unlock header on transaction requests.
 */
const KEY = "txn-unlock";

type Stored = { token: string; expiresAt: number };

export function setUnlock(token: string, expiresInSeconds: number) {
  const s: Stored = { token, expiresAt: Date.now() + expiresInSeconds * 1000 };
  sessionStorage.setItem(KEY, JSON.stringify(s));
}

/** Returns the token if present and not within 5s of expiry, else null. */
export function getUnlockToken(): string | null {
  const raw = sessionStorage.getItem(KEY);
  if (!raw) return null;
  try {
    const s = JSON.parse(raw) as Stored;
    if (s.expiresAt > Date.now() + 5000) return s.token;
  } catch {
    /* ignore malformed */
  }
  return null;
}

export function clearUnlock() {
  sessionStorage.removeItem(KEY);
}

/** Fired when the server rejects a request for a missing/expired unlock (423). */
export const TXN_LOCKED_EVENT = "txn-locked";
