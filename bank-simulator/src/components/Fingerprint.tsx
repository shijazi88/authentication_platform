import { useRef } from "react";
import {
  Upload,
  X,
  Fingerprint as Fp,
  CheckCircle2,
  Sparkles,
  ScanLine,
  Loader2,
} from "lucide-react";
import { cn } from "../lib/cn";
import { SAMPLE_FINGERPRINT_BASE64, SAMPLE_FINGERPRINT_META } from "../sampleFingerprint";
import { FingerprintThumb } from "./FingerprintThumb";

type Props = {
  // kind tells us whether we have user-captured image data (dataUrl previewable)
  // or the bundled sample (no previewable data URL, so we draw the synthetic thumb).
  kind: "none" | "file" | "sample" | "device";
  imageDataUrl: string | null;
  imageSizeChars: number; // base64 length, for display
  onFile: (dataUrl: string, base64: string) => void;
  onSample: () => void;
  onClear: () => void;
  scanning?: boolean;
  // Device capture is optional. Wired up by VerifyScreen using the active
  // provider from the store. We stay vendor-agnostic — this component knows
  // nothing about Secugen/Dermalog/etc.
  onDeviceCapture?: () => Promise<void> | void;
  deviceCaptureLabel?: string;
  deviceCaptureDisabled?: boolean;
  deviceCaptureBusy?: boolean;
  deviceCaptureDisabledReason?: string;
  qualityScore?: number | null;
};

export function FingerprintCapture({
  kind,
  imageDataUrl,
  imageSizeChars,
  onFile,
  onSample,
  onClear,
  scanning,
  onDeviceCapture,
  deviceCaptureLabel,
  deviceCaptureDisabled,
  deviceCaptureBusy,
  deviceCaptureDisabledReason,
  qualityScore,
}: Props) {
  const inputRef = useRef<HTMLInputElement>(null);

  const handleFile = async (file: File) => {
    const dataUrl = await new Promise<string>((resolve, reject) => {
      const r = new FileReader();
      r.onload = () => resolve(String(r.result));
      r.onerror = reject;
      r.readAsDataURL(file);
    });
    const base64 = dataUrl.replace(/^data:[^;]+;base64,/, "");
    onFile(dataUrl, base64);
  };

  if (kind !== "none") {
    const captionMap = {
      file: "Fingerprint captured",
      sample: "Sample fingerprint loaded",
      device: `Captured from ${deviceCaptureLabel ?? "device"}`,
    } as const;

    return (
      <div className="flex items-center gap-4 rounded-xl border border-ink-faint bg-paper p-3">
        <div
          className={cn(
            "h-20 w-20 rounded-lg border border-ink-faint bg-paper-raised overflow-hidden flex-shrink-0",
            scanning && "scan-pulse",
          )}
        >
          {(kind === "file" || kind === "device") && imageDataUrl ? (
            <img src={imageDataUrl} alt="fingerprint" className="w-full h-full object-cover" />
          ) : (
            <FingerprintThumb className="w-full h-full" />
          )}
        </div>

        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 text-moi-green font-semibold text-sm">
            <CheckCircle2 className="h-4 w-4" />
            {captionMap[kind]}
            {kind === "device" && qualityScore != null && (
              <span
                className={cn(
                  "text-xs font-bold px-1.5 py-0.5 rounded",
                  qualityScore >= 70
                    ? "bg-moi-green/10 text-moi-green"
                    : qualityScore >= 40
                      ? "bg-moi-gold/10 text-moi-gold"
                      : "bg-moi-red/10 text-moi-red",
                )}
              >
                Q {qualityScore}
              </span>
            )}
          </div>
          <div className="text-xs text-ink-dim mt-0.5">
            {kind === "sample" ? (
              <>
                {SAMPLE_FINGERPRINT_META.format} · {SAMPLE_FINGERPRINT_META.dimensions} ·{" "}
                {SAMPLE_FINGERPRINT_META.ppi} PPI · {imageSizeChars.toLocaleString()} chars
              </>
            ) : (
              <>{imageSizeChars.toLocaleString()} chars base64 · ready to send</>
            )}
          </div>
          <div className="mt-1 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs">
            {onDeviceCapture && (
              <button
                type="button"
                onClick={() => void onDeviceCapture()}
                disabled={deviceCaptureDisabled || deviceCaptureBusy}
                className="font-semibold text-moi-blue hover:underline disabled:text-ink-dim disabled:no-underline disabled:cursor-not-allowed"
              >
                {deviceCaptureBusy ? "Capturing…" : "Re-capture from device"}
              </button>
            )}
            <button
              type="button"
              onClick={() => inputRef.current?.click()}
              className="font-semibold text-moi-blue hover:underline"
            >
              Replace with file
            </button>
            {kind !== "sample" && (
              <button
                type="button"
                onClick={onSample}
                className="font-semibold text-moi-blue hover:underline"
              >
                Use sample
              </button>
            )}
          </div>
        </div>

        <button
          type="button"
          onClick={onClear}
          className="btn-ghost !p-2 self-start"
          aria-label="Remove fingerprint"
        >
          <X className="h-4 w-4" />
        </button>

        <input
          ref={inputRef}
          type="file"
          accept="image/*,.wsq,application/octet-stream"
          className="hidden"
          onChange={(e) => {
            const f = e.target.files?.[0];
            if (f) void handleFile(f);
            e.target.value = "";
          }}
        />
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {/* Featured: live capture from the configured device — only when wired up. */}
      {onDeviceCapture && (
        <button
          type="button"
          onClick={() => void onDeviceCapture()}
          disabled={deviceCaptureDisabled || deviceCaptureBusy}
          className={cn(
            "w-full rounded-xl border-2 transition p-4 flex items-center gap-3 text-start",
            deviceCaptureDisabled || deviceCaptureBusy
              ? "border-ink-faint bg-paper-raised cursor-not-allowed opacity-70"
              : "border-moi-blue bg-moi-blue/5 hover:bg-moi-blue/10 hover:border-moi-blue-dark",
          )}
        >
          <div
            className={cn(
              "h-11 w-11 rounded-lg flex items-center justify-center flex-shrink-0",
              deviceCaptureBusy ? "bg-moi-blue/10" : "bg-moi-blue text-white",
            )}
          >
            {deviceCaptureBusy ? (
              <Loader2 className="h-5 w-5 animate-spin text-moi-blue" />
            ) : (
              <ScanLine className="h-5 w-5" />
            )}
          </div>
          <div className="flex-1">
            <div className="font-semibold text-ink flex items-center gap-1.5 text-sm">
              {deviceCaptureBusy ? "Reading fingerprint…" : "Capture from device"}
            </div>
            <div className="text-[11px] text-ink-dim mt-0.5">
              {deviceCaptureDisabled
                ? (deviceCaptureDisabledReason ?? "Pick a finger above first.")
                : (deviceCaptureLabel ?? "Scan with the configured device")}
            </div>
          </div>
        </button>
      )}

      <div className="grid sm:grid-cols-2 gap-3">
        <button
          type="button"
          onClick={() => inputRef.current?.click()}
          className="rounded-xl border-2 border-dashed border-ink-faint bg-paper hover:border-moi-blue hover:bg-moi-blue/5 transition p-4 flex items-center gap-3 text-start"
        >
          <div className="h-11 w-11 rounded-lg bg-moi-blue/10 flex items-center justify-center flex-shrink-0">
            <Fp className="h-5 w-5 text-moi-blue" />
          </div>
          <div className="flex-1">
            <div className="font-semibold text-ink flex items-center gap-1.5 text-sm">
              <Upload className="h-3.5 w-3.5" /> Upload file
            </div>
            <div className="text-[11px] text-ink-dim mt-0.5">
              Select an image · PNG, JPG, WSQ
            </div>
          </div>
        </button>

        <button
          type="button"
          onClick={onSample}
          className="rounded-xl border-2 border-dashed border-moi-gold/40 bg-moi-gold/5 hover:border-moi-gold hover:bg-moi-gold/10 transition p-4 flex items-center gap-3 text-start"
        >
          <div className="h-11 w-11 rounded-lg bg-moi-gold/20 flex items-center justify-center flex-shrink-0">
            <Sparkles className="h-5 w-5 text-moi-gold" />
          </div>
          <div className="flex-1">
            <div className="font-semibold text-ink flex items-center gap-1.5 text-sm">
              Use sample fingerprint
            </div>
            <div className="text-[11px] text-ink-dim mt-0.5">
              Bundled WSQ payload · for testing only
            </div>
          </div>
        </button>
      </div>

      <input
        ref={inputRef}
        type="file"
        accept="image/*,.wsq,application/octet-stream"
        className="hidden"
        onChange={(e) => {
          const f = e.target.files?.[0];
          if (f) void handleFile(f);
          e.target.value = "";
        }}
      />
    </div>
  );
}

export { SAMPLE_FINGERPRINT_BASE64 };
