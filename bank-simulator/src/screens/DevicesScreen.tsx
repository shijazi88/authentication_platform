import { useEffect, useMemo, useRef, useState } from "react";
import { toast } from "sonner";
import { useTranslation } from "react-i18next";
import {
  Fingerprint as FpIcon,
  ScanLine,
  Loader2,
  CheckCircle2,
  AlertTriangle,
  Plug,
  Save,
  RotateCcw,
  X,
  Image as ImageIcon,
  FileText,
  Clipboard,
  Code2,
  Globe,
  Hash,
  Timer,
  Layers,
  Server,
  CircleDot,
} from "lucide-react";
import { FingerHands } from "../components/FingerHands";
import { useApp } from "../store";
import { cn } from "../lib/cn";
import type {
  CaptureResult,
  DeviceInfo,
  ProviderConfig,
} from "../providers/types";
import {
  PROVIDER_CATALOG,
  buildProvider,
  defaultConfigFor,
  getProviderInfo,
} from "../providers/registry";
import {
  copyPngBase64ToClipboard,
  copyWsqBase64ToClipboard,
  exportJson,
  exportPng,
  exportWsq,
} from "../providers/encoders";
import { FINGER_LABELS, type FingerPosition } from "../types";

export function DevicesScreen() {
  const { t } = useTranslation();
  const selectedProviderId = useApp((s) => s.selectedProviderId);
  const setSelectedProvider = useApp((s) => s.setSelectedProvider);
  const providerConfigs = useApp((s) => s.providerConfigs);
  const updateProviderConfig = useApp((s) => s.updateProviderConfig);
  const resetProviderConfig = useApp((s) => s.resetProviderConfig);

  const config = providerConfigs[selectedProviderId] ?? defaultConfigFor(selectedProviderId);
  const provider = useMemo(
    () => buildProvider(selectedProviderId, config),
    [selectedProviderId, config],
  );

  const [device, setDevice] = useState<DeviceInfo | null>(null);
  const [probing, setProbing] = useState(false);

  // Probe the active provider when the user lands on this screen or switches
  // device, then silently re-probe every 5s so unplug/plug events surface
  // automatically without the user clicking "Test device".
  useEffect(() => {
    let cancelled = false;
    let timeoutId: ReturnType<typeof setTimeout> | undefined;

    setProbing(true);
    setDevice(null);

    const tick = async (isInitial: boolean) => {
      if (cancelled) return;
      try {
        const d = await provider.info();
        if (!cancelled) setDevice(d);
      } catch {
        // Silent — keep last known state. The next tick will retry.
      } finally {
        if (!cancelled && isInitial) setProbing(false);
        if (!cancelled) timeoutId = setTimeout(() => tick(false), 5_000);
      }
    };

    void tick(true);

    return () => {
      cancelled = true;
      if (timeoutId) clearTimeout(timeoutId);
    };
  }, [provider]);

  // Local edit buffer for the config form so changes are explicit (Save / Reset),
  // mirroring the SettingsScreen pattern.
  const [draft, setDraft] = useState<ProviderConfig>(config);
  useEffect(() => setDraft(config), [config]);
  const dirty =
    draft.baseUrl !== config.baseUrl ||
    draft.captureTimeoutMs !== config.captureTimeoutMs ||
    draft.qualityThreshold !== config.qualityThreshold ||
    draft.multiCaptureCount !== config.multiCaptureCount;

  const saveConfig = () => {
    if (!/^https?:\/\//.test(draft.baseUrl.trim())) {
      toast.error(t("devices.errors.urlInvalid"));
      return;
    }
    updateProviderConfig(selectedProviderId, {
      baseUrl: draft.baseUrl.trim(),
      captureTimeoutMs: clampInt(draft.captureTimeoutMs, 1_000, 120_000),
      qualityThreshold: clampInt(draft.qualityThreshold, 0, 100),
      multiCaptureCount: clampInt(draft.multiCaptureCount, 1, 10),
    });
    toast.success(t("devices.saved"));
  };
  const resetConfig = () => {
    resetProviderConfig(selectedProviderId);
    toast.success(t("devices.resetDone"));
  };

  // Capture-test state (lives only on this screen).
  const [finger, setFinger] = useState<FingerPosition | null>(3); // Right Middle by default
  const [capturing, setCapturing] = useState(false);
  const [result, setResult] = useState<CaptureResult | null>(null);
  const [captureError, setCaptureError] = useState<string | null>(null);
  const ctrlRef = useRef<AbortController | null>(null);

  const runCapture = async () => {
    if (!finger) {
      toast.error(t("devices.errors.pickFinger"));
      return;
    }
    setCapturing(true);
    setCaptureError(null);
    setResult(null);
    const controller = new AbortController();
    ctrlRef.current = controller;
    try {
      const r = await provider.capture({
        fingerPosition: finger,
        signal: controller.signal,
      });
      setResult(r);
      toast.success(t("devices.captureDone", { quality: r.quality }));
    } catch (err) {
      if (err instanceof DOMException && err.name === "AbortError") {
        toast.info(t("devices.captureCancelled"));
      } else {
        const msg = err instanceof Error ? err.message : String(err);
        setCaptureError(msg);
        toast.error(t("devices.captureFailed", { error: msg }));
      }
    } finally {
      setCapturing(false);
      ctrlRef.current = null;
    }
  };
  const cancelCapture = () => ctrlRef.current?.abort();

  const probeDevice = async () => {
    setProbing(true);
    try {
      const d = await provider.info();
      setDevice(d);
      if (d.connected) toast.success(t("devices.deviceReady"));
      else toast.error(d.notes ?? t("devices.deviceOffline"));
    } finally {
      setProbing(false);
    }
  };

  return (
    <div className="max-w-6xl mx-auto p-8 space-y-5">
      <header>
        <h1 className="text-2xl font-extrabold text-ink flex items-center gap-2">
          <FpIcon className="h-6 w-6 text-moi-blue" />
          {t("devices.title")}
        </h1>
        <p className="text-sm text-ink-muted mt-1">{t("devices.subtitle")}</p>
      </header>

      {/* PROVIDER PICKER ----------------------------------------------------- */}
      <section className="card p-5">
        <div className="flex items-center gap-2 mb-3">
          <Layers className="h-4 w-4 text-moi-blue" />
          <h2 className="text-sm font-bold text-ink uppercase tracking-wider">
            {t("devices.providerSection")}
          </h2>
        </div>

        <div className="grid sm:grid-cols-2 gap-3">
          {PROVIDER_CATALOG.map((p) => {
            const active = p.id === selectedProviderId;
            return (
              <button
                key={p.id}
                type="button"
                onClick={() => setSelectedProvider(p.id)}
                className={cn(
                  "text-start rounded-xl border-2 p-4 transition flex items-start gap-3",
                  active
                    ? "border-moi-blue bg-moi-blue/5"
                    : "border-ink-faint bg-paper hover:border-moi-blue/50 hover:bg-moi-blue/5",
                )}
              >
                <div
                  className={cn(
                    "h-10 w-10 rounded-lg flex items-center justify-center flex-shrink-0",
                    active ? "bg-moi-blue text-white" : "bg-moi-blue/10 text-moi-blue",
                  )}
                >
                  <FpIcon className="h-5 w-5" />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <span className="font-bold text-ink text-sm">{p.displayName}</span>
                    {active && (
                      <span className="text-[10px] font-bold text-moi-blue uppercase tracking-wider">
                        · {t("devices.activeBadge")}
                      </span>
                    )}
                  </div>
                  <div className="text-[11px] text-ink-dim mt-0.5">{p.vendor}</div>
                  <div className="text-[11px] mt-1">
                    {p.hardwareReady ? (
                      <span className="inline-flex items-center gap-1 text-moi-green font-semibold">
                        <CheckCircle2 className="h-3 w-3" /> {t("devices.driverReady")}
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1 text-ink-muted">
                        <CircleDot className="h-3 w-3" /> {t("devices.comingSoon")}
                      </span>
                    )}
                  </div>
                </div>
              </button>
            );
          })}
        </div>
      </section>

      {/* CONNECTION ---------------------------------------------------------- */}
      <section className="card p-5 space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Plug className="h-4 w-4 text-moi-blue" />
            <h2 className="text-sm font-bold text-ink uppercase tracking-wider">
              {t("devices.connectionSection")}
            </h2>
          </div>
          <DeviceStatusPill device={device} probing={probing} />
        </div>

        <div className="grid md:grid-cols-2 gap-4">
          <Field
            label={t("devices.baseUrl")}
            icon={<Globe className="h-3.5 w-3.5" />}
            hint={t("devices.baseUrlHint")}
          >
            <input
              className="input font-mono text-sm"
              value={draft.baseUrl}
              onChange={(e) => setDraft({ ...draft, baseUrl: e.target.value })}
              spellCheck={false}
              dir="ltr"
              placeholder={getProviderInfo(selectedProviderId)?.defaultBaseUrl}
            />
          </Field>

          <Field
            label={t("devices.qualityThreshold")}
            icon={<Hash className="h-3.5 w-3.5" />}
            hint={t("devices.qualityThresholdHint")}
          >
            <input
              type="number"
              min={0}
              max={100}
              className="input font-mono text-sm"
              value={draft.qualityThreshold}
              onChange={(e) =>
                setDraft({ ...draft, qualityThreshold: Number(e.target.value) })
              }
            />
          </Field>

          <Field
            label={t("devices.captureTimeout")}
            icon={<Timer className="h-3.5 w-3.5" />}
            hint={t("devices.captureTimeoutHint")}
          >
            <input
              type="number"
              min={1000}
              max={120000}
              step={500}
              className="input font-mono text-sm"
              value={draft.captureTimeoutMs}
              onChange={(e) =>
                setDraft({ ...draft, captureTimeoutMs: Number(e.target.value) })
              }
            />
          </Field>

          <Field
            label={t("devices.multiCapture")}
            icon={<Layers className="h-3.5 w-3.5" />}
            hint={t("devices.multiCaptureHint")}
          >
            <input
              type="number"
              min={1}
              max={10}
              className="input font-mono text-sm"
              value={draft.multiCaptureCount}
              onChange={(e) =>
                setDraft({ ...draft, multiCaptureCount: Number(e.target.value) })
              }
            />
          </Field>
        </div>

        <div className="flex items-center justify-between pt-2 border-t border-ink-faint/60">
          <button
            type="button"
            className="btn-secondary"
            onClick={probeDevice}
            disabled={probing}
          >
            {probing ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin" /> {t("devices.testing")}
              </>
            ) : (
              <>
                <Server className="h-4 w-4" /> {t("devices.testDevice")}
              </>
            )}
          </button>
          <div className="flex items-center gap-2">
            {dirty && (
              <button type="button" onClick={resetConfig} className="btn-ghost">
                <RotateCcw className="h-4 w-4" /> {t("devices.reset")}
              </button>
            )}
            <button
              type="button"
              onClick={saveConfig}
              disabled={!dirty}
              className="btn-primary"
            >
              <Save className="h-4 w-4" /> {t("devices.save")}
            </button>
          </div>
        </div>
      </section>

      {/* CAPTURE TEST -------------------------------------------------------- */}
      <section className="card p-5">
        <div className="flex items-center gap-2 mb-3">
          <ScanLine className="h-4 w-4 text-moi-blue" />
          <h2 className="text-sm font-bold text-ink uppercase tracking-wider">
            {t("devices.captureSection")}
          </h2>
          <span className="text-[11px] text-ink-dim normal-case tracking-normal">
            · {t("devices.captureSectionHint")}
          </span>
        </div>

        <FingerHands selected={finger} onSelect={setFinger} />

        <div className="mt-4 grid lg:grid-cols-[1fr_auto] gap-4 items-start">
          <CapturePreview
            result={result}
            capturing={capturing}
            error={captureError}
            finger={finger}
          />

          <div className="lg:w-72 space-y-2">
            {!capturing ? (
              <button
                type="button"
                onClick={runCapture}
                disabled={!finger || (device !== null && !device.connected)}
                className="btn-primary w-full !py-3"
              >
                <ScanLine className="h-4 w-4" />
                {t("devices.captureNow")}
              </button>
            ) : (
              <button
                type="button"
                onClick={cancelCapture}
                className="btn w-full !py-3 bg-moi-red text-white hover:bg-moi-red/90"
              >
                <X className="h-4 w-4" />
                {t("devices.cancel")}
              </button>
            )}
            {!finger && (
              <p className="text-[11px] text-moi-red">· {t("devices.errors.pickFinger")}</p>
            )}
            {device && !device.connected && (
              <p className="text-[11px] text-moi-red">
                · {device.notes ?? t("devices.deviceOffline")}
              </p>
            )}

            {result && (
              <div className="pt-2 mt-2 border-t border-ink-faint/60">
                <div className="text-[11px] font-bold uppercase tracking-wider text-ink-muted mb-2">
                  {t("devices.exports")}
                </div>
                <div className="grid grid-cols-2 gap-2">
                  <ExportButton
                    icon={<ImageIcon className="h-3.5 w-3.5" />}
                    label="PNG"
                    onClick={() => exportPng(result)}
                  />
                  <ExportButton
                    icon={<FileText className="h-3.5 w-3.5" />}
                    label="WSQ"
                    onClick={() => exportWsq(result)}
                  />
                  <ExportButton
                    icon={<Clipboard className="h-3.5 w-3.5" />}
                    label="B64"
                    onClick={async () => {
                      const ok = await copyPngBase64ToClipboard(result);
                      ok
                        ? toast.success(t("devices.copiedPngB64"))
                        : toast.error(t("devices.copyFailed"));
                    }}
                  />
                  <ExportButton
                    icon={<Code2 className="h-3.5 w-3.5" />}
                    label="JSON"
                    onClick={() => exportJson(result, "")}
                  />
                </div>
                <button
                  type="button"
                  onClick={async () => {
                    const ok = await copyWsqBase64ToClipboard(result);
                    ok
                      ? toast.success(t("devices.copiedWsqB64"))
                      : toast.error(t("devices.copyFailed"));
                  }}
                  className="btn-ghost w-full mt-2 !text-xs"
                >
                  <Clipboard className="h-3.5 w-3.5" /> {t("devices.copyWsqBase64")}
                </button>
              </div>
            )}
          </div>
        </div>
      </section>
    </div>
  );
}

// ─── building blocks ───────────────────────────────────────────────────────

function Field({
  label,
  icon,
  hint,
  children,
}: {
  label: string;
  icon: React.ReactNode;
  hint: string;
  children: React.ReactNode;
}) {
  return (
    <div>
      <label className="label flex items-center gap-1.5">
        {icon} {label}
      </label>
      {children}
      <p className="text-[11px] text-ink-dim mt-1">{hint}</p>
    </div>
  );
}

function DeviceStatusPill({
  device,
  probing,
}: {
  device: DeviceInfo | null;
  probing: boolean;
}) {
  if (probing) {
    return (
      <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-paper-raised text-ink-muted text-xs font-semibold">
        <Loader2 className="h-3.5 w-3.5 animate-spin" /> Probing…
      </span>
    );
  }
  if (!device) {
    return (
      <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-paper-raised text-ink-dim text-xs font-semibold">
        <CircleDot className="h-3.5 w-3.5" /> Idle
      </span>
    );
  }
  if (device.connected) {
    return (
      <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-moi-green/10 border border-moi-green/30 text-moi-green text-xs font-semibold">
        <CheckCircle2 className="h-3.5 w-3.5" />
        {device.model ?? "Device"} ·{" "}
        {device.imageWidth && device.imageHeight
          ? `${device.imageWidth}×${device.imageHeight}`
          : ""}{" "}
        {device.imageDpi ? `@ ${device.imageDpi} DPI` : ""}
      </span>
    );
  }
  return (
    <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-moi-red/10 border border-moi-red/30 text-moi-red text-xs font-semibold">
      <AlertTriangle className="h-3.5 w-3.5" /> Offline
    </span>
  );
}

function CapturePreview({
  result,
  capturing,
  error,
  finger,
}: {
  result: CaptureResult | null;
  capturing: boolean;
  error: string | null;
  finger: FingerPosition | null;
}) {
  return (
    <div className="rounded-xl border border-ink-faint/70 bg-paper-raised p-4">
      <div className="flex items-start gap-4">
        <div className="h-40 w-32 rounded-lg border border-ink-faint bg-paper overflow-hidden flex items-center justify-center flex-shrink-0">
          {capturing ? (
            <div className="flex flex-col items-center gap-2 text-ink-muted text-xs">
              <Loader2 className="h-6 w-6 animate-spin text-moi-blue" />
              <span>Reading…</span>
            </div>
          ) : result ? (
            <img
              src={`data:image/png;base64,${result.pngBase64}`}
              alt="Captured fingerprint"
              className="w-full h-full object-cover"
            />
          ) : (
            <div className="text-ink-dim text-xs text-center px-2">
              Place finger on scanner
            </div>
          )}
        </div>

        <div className="flex-1 min-w-0 text-xs space-y-1.5">
          {error ? (
            <div className="flex items-start gap-2 text-moi-red">
              <AlertTriangle className="h-4 w-4 flex-shrink-0 mt-0.5" />
              <span className="break-words">{error}</span>
            </div>
          ) : result ? (
            <>
              <Row label="Finger">
                <span className="inline-flex items-center gap-1.5">
                  <span className="h-4 w-4 rounded-full bg-moi-gold text-white text-[10px] font-bold flex items-center justify-center">
                    {result.fingerPosition}
                  </span>
                  <span className="font-semibold text-ink">
                    {FINGER_LABELS[result.fingerPosition]}
                  </span>
                </span>
              </Row>
              <Row label="Quality">
                <span
                  className={cn(
                    "font-bold",
                    result.quality >= 70
                      ? "text-moi-green"
                      : result.quality >= 40
                        ? "text-moi-gold"
                        : "text-moi-red",
                  )}
                >
                  {result.quality}
                </span>
                <span className="text-ink-dim"> / 100</span>
              </Row>
              <Row label="Image">
                <span className="font-mono text-ink">
                  {result.width}×{result.height} @ {result.dpi} DPI
                </span>
              </Row>
              <Row label="WSQ">
                <span className="font-mono text-ink">
                  {result.wsqBase64.length.toLocaleString()} chars
                </span>
              </Row>
              <Row label="Time">
                <span className="font-mono text-ink-muted">
                  {new Date(result.capturedAt).toLocaleTimeString()}
                </span>
              </Row>
            </>
          ) : (
            <div className="text-ink-dim">
              {finger
                ? `Ready to capture ${FINGER_LABELS[finger]}.`
                : "Pick a finger above, then capture."}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function Row({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex items-center gap-2">
      <span className="font-bold text-ink-muted uppercase tracking-wider w-14 text-[10px]">
        {label}
      </span>
      <span>{children}</span>
    </div>
  );
}

function ExportButton({
  icon,
  label,
  onClick,
}: {
  icon: React.ReactNode;
  label: string;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="inline-flex items-center justify-center gap-1.5 px-2 py-1.5 rounded-lg border border-ink-faint bg-paper hover:border-moi-blue hover:text-moi-blue text-[11px] font-bold uppercase tracking-wider text-ink-muted transition"
    >
      {icon} {label}
    </button>
  );
}

function clampInt(n: number, lo: number, hi: number) {
  if (Number.isNaN(n)) return lo;
  return Math.max(lo, Math.min(hi, Math.round(n)));
}
