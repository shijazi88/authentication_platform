import type {
  CaptureOptions,
  CaptureResult,
  DeviceInfo,
  FingerprintProvider,
  ProviderConfig,
  ProviderId,
} from "./types";

// Talks to a vendor sidecar daemon over HTTP. Each vendor ships its own
// daemon (sanad-secugen-capture.exe, sanad-dermalog-capture.exe, …) that
// implements this contract:
//
//   GET  {baseUrl}/info     → DeviceInfo
//   POST {baseUrl}/capture  ← { fingerPosition, qualityThreshold, multiCaptureCount, timeoutMs }
//                           → CaptureResult
//
// The daemon runs locally because it needs a USB device. CORS is permissive
// (Electron renderer is `file://` or `http://localhost:5173`).
export class HttpFingerprintProvider implements FingerprintProvider {
  constructor(
    public readonly id: ProviderId,
    public readonly displayName: string,
    private readonly config: ProviderConfig,
  ) {}

  async info(): Promise<DeviceInfo> {
    try {
      const r = await fetch(`${this.config.baseUrl}/info`, {
        method: "GET",
        signal: AbortSignal.timeout(3_000),
      });
      if (!r.ok) {
        return { connected: false, notes: `HTTP ${r.status} from /info` };
      }
      const body = (await r.json()) as DeviceInfo;
      return { ...body, connected: true };
    } catch (err) {
      return {
        connected: false,
        notes: err instanceof Error ? err.message : "Unreachable",
      };
    }
  }

  async capture(opts: CaptureOptions): Promise<CaptureResult> {
    const body = {
      fingerPosition: opts.fingerPosition,
      qualityThreshold: opts.qualityThreshold ?? this.config.qualityThreshold,
      multiCaptureCount: opts.multiCaptureCount ?? this.config.multiCaptureCount,
      timeoutMs: opts.timeoutMs ?? this.config.captureTimeoutMs,
    };
    const r = await fetch(`${this.config.baseUrl}/capture`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
      signal: opts.signal,
    });
    if (!r.ok) {
      const text = await r.text().catch(() => "");
      throw new Error(`Daemon returned HTTP ${r.status}: ${text || r.statusText}`);
    }
    return (await r.json()) as CaptureResult;
  }
}
