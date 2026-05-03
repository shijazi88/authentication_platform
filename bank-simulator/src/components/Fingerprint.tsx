import { useRef } from "react";
import { Upload, X, Fingerprint as Fp, CheckCircle2, Sparkles } from "lucide-react";
import { cn } from "../lib/cn";
import { SAMPLE_FINGERPRINT_BASE64, SAMPLE_FINGERPRINT_META } from "../sampleFingerprint";
import { FingerprintThumb } from "./FingerprintThumb";

type Props = {
  // kind tells us whether we have user-captured image data (dataUrl previewable)
  // or the bundled sample (no previewable data URL, so we draw the synthetic thumb).
  kind: "none" | "file" | "sample";
  imageDataUrl: string | null;
  imageSizeChars: number; // base64 length, for display
  onFile: (dataUrl: string, base64: string) => void;
  onSample: () => void;
  onClear: () => void;
  scanning?: boolean;
};

export function FingerprintCapture({
  kind,
  imageDataUrl,
  imageSizeChars,
  onFile,
  onSample,
  onClear,
  scanning,
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
    return (
      <div className="flex items-center gap-4 rounded-xl border border-ink-faint bg-paper p-3">
        <div
          className={cn(
            "h-20 w-20 rounded-lg border border-ink-faint bg-paper-raised overflow-hidden flex-shrink-0",
            scanning && "scan-pulse",
          )}
        >
          {kind === "file" && imageDataUrl ? (
            <img src={imageDataUrl} alt="fingerprint" className="w-full h-full object-cover" />
          ) : (
            <FingerprintThumb className="w-full h-full" />
          )}
        </div>

        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 text-moi-green font-semibold text-sm">
            <CheckCircle2 className="h-4 w-4" />
            {kind === "sample" ? "Sample fingerprint loaded" : "Fingerprint captured"}
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
          <div className="mt-1 flex items-center gap-3 text-xs">
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
            <Upload className="h-3.5 w-3.5" /> Capture fingerprint
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
