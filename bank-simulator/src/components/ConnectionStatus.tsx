import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Wifi, WifiOff, Loader2 } from "lucide-react";
import { cn } from "../lib/cn";
import { useApp } from "../store";

type Status = "connecting" | "online" | "offline";

const POLL_MS = 30_000;
const TIMEOUT_MS = 8_000;

async function ping(baseUrl: string): Promise<boolean> {
  const controller = new AbortController();
  const t = setTimeout(() => controller.abort(), TIMEOUT_MS);
  try {
    // /actuator/health is public on the Sanad backend
    const res = await fetch(
      baseUrl.replace(/\/+$/, "") + "/actuator/health",
      { signal: controller.signal, cache: "no-store" },
    );
    return res.ok;
  } catch {
    return false;
  } finally {
    clearTimeout(t);
  }
}

export function ConnectionStatus() {
  const baseUrl = useApp((s) => s.credentials?.baseUrl);
  const { t } = useTranslation();
  const [status, setStatus] = useState<Status>("connecting");
  const [lastCheckedMs, setLastCheckedMs] = useState<number | null>(null);

  useEffect(() => {
    if (!baseUrl) return;
    let cancelled = false;

    const check = async () => {
      setStatus((s) => (s === "online" ? "online" : "connecting"));
      const ok = await ping(baseUrl);
      if (cancelled) return;
      setStatus(ok ? "online" : "offline");
      setLastCheckedMs(Date.now());
    };

    void check();
    const id = setInterval(check, POLL_MS);
    return () => {
      cancelled = true;
      clearInterval(id);
    };
  }, [baseUrl]);

  const isOnline = status === "online";
  const isConnecting = status === "connecting";

  return (
    <div
      className={cn(
        "flex items-center gap-2 text-[11px] px-2.5 py-1.5 rounded-md border transition",
        isOnline
          ? "border-moi-green/30 bg-moi-green/5 text-moi-green"
          : isConnecting
          ? "border-ink-faint bg-paper-raised text-ink-muted"
          : "border-moi-red/30 bg-moi-red/5 text-moi-red",
      )}
      title={
        lastCheckedMs
          ? t("connection.lastChecked", {
              time: new Date(lastCheckedMs).toLocaleTimeString(),
            })
          : undefined
      }
    >
      <span className="relative flex h-2 w-2">
        {isOnline && (
          <span className="absolute inline-flex h-full w-full rounded-full bg-moi-green opacity-60 animate-ping" />
        )}
        <span
          className={cn(
            "relative inline-flex h-2 w-2 rounded-full",
            isOnline && "bg-moi-green",
            isConnecting && "bg-ink-dim",
            status === "offline" && "bg-moi-red",
          )}
        />
      </span>
      {isConnecting ? (
        <>
          <Loader2 className="h-3 w-3 animate-spin" />
          <span className="font-semibold">{t("connection.connecting")}</span>
        </>
      ) : isOnline ? (
        <>
          <Wifi className="h-3 w-3" />
          <span className="font-semibold">{t("connection.connected")}</span>
        </>
      ) : (
        <>
          <WifiOff className="h-3 w-3" />
          <span className="font-semibold">{t("connection.offline")}</span>
        </>
      )}
    </div>
  );
}
