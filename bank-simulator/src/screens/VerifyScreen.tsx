import { useMemo, useRef, useState } from "react";
import { toast } from "sonner";
import { useTranslation } from "react-i18next";
import { Fingerprint, ScanLine, Loader2, IdCard, Info } from "lucide-react";
import { FINGER_LABELS, type FingerPosition, type VerifyResponse } from "../types";
import { FingerHands } from "../components/FingerHands";
import { FingerprintCapture } from "../components/Fingerprint";
import { Keypad } from "../components/Keypad";
import { TestIdentityPicker } from "../components/TestIdentityPicker";
import { VerifyingOverlay } from "../components/VerifyingOverlay";
import { SAMPLE_FINGERPRINT_BASE64 } from "../sampleFingerprint";
import { useShortcuts } from "../lib/useShortcuts";
import { useApp } from "../store";
import { verifyIdentity } from "../api";

type Props = {
  onResult: (
    r:
      | { ok: true; data: VerifyResponse }
      | { ok: false; status: number; message: string; body?: unknown },
  ) => void;
};

export function VerifyScreen({ onResult }: Props) {
  const creds = useApp((s) => s.credentials)!;
  const addHistory = useApp((s) => s.addHistory);
  const { t } = useTranslation();

  const [nationalNumber, setNationalNumber] = useState("");
  const [finger, setFinger] = useState<FingerPosition | null>(null);
  const [fpKind, setFpKind] = useState<"none" | "file" | "sample">("none");
  const [fpDataUrl, setFpDataUrl] = useState<string | null>(null);
  const [fpBase64, setFpBase64] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  // Strip everything except digits, then group into NNNN-NNNN-NNNN — the format
  // MoI's verify endpoint requires. Bank can paste either form; we always store
  // the dashed form because that's what we send on the wire.
  const formatNationalNumber = (raw: string): string => {
    const digits = raw.replace(/\D/g, "").slice(0, 12);
    const a = digits.slice(0, 4);
    const b = digits.slice(4, 8);
    const c = digits.slice(8, 12);
    return [a, b, c].filter(Boolean).join("-");
  };
  const digitCount = nationalNumber.replace(/\D/g, "").length;

  const canSubmit = useMemo(
    () =>
      /^\d{4}-\d{4}-\d{4}$/.test(nationalNumber) &&
      finger !== null &&
      fpBase64 !== null &&
      !submitting,
    [nationalNumber, finger, fpBase64, submitting],
  );

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!canSubmit) return;

    setSubmitting(true);
    // Biometric is required — canSubmit guarantees finger + fpBase64 are set.
    const biometrics = { fingerPosition: finger!, image: fpBase64! };
    const payload = { nationalNumber, biometrics };

    // For audit/history, truncate the base64 image to keep localStorage small.
    const requestForAudit = {
      ...payload,
      biometrics: {
        fingerPosition: biometrics.fingerPosition,
        image: `<base64 · ${biometrics.image.length} chars · ${biometrics.image.slice(0, 24)}…>`,
      },
    };

    const started = performance.now();
    // Run the API call in parallel with a minimum display time for the overlay
    // (900ms × 5 steps = 4.5s to show all stages; 2.5s minimum keeps it snappy
    // while still letting the user see the scan happen).
    const MIN_OVERLAY_MS = 2500;
    const [res] = await Promise.all([
      verifyIdentity(creds.baseUrl, creds.clientId, creds.clientSecret, payload),
      new Promise<void>((resolve) => setTimeout(resolve, MIN_OVERLAY_MS)),
    ]);
    const durationMs = Math.round(performance.now() - started);

    const now = new Date().toISOString();
    const endpoint = "POST /api/v1/verify/identity";

    if (res.ok) {
      addHistory({
        id: res.data.transaction?.id ?? crypto.randomUUID(),
        ts: now,
        nationalNumber,
        fingerPosition: finger ?? undefined,
        success: true,
        httpStatus: res.http,
        verificationStatus:
          res.data.result?.verification?.verification ??
          res.data.result?.verification?.status,
        endpoint,
        baseUrl: creds.baseUrl,
        requestBody: requestForAudit,
        responseBody: res.data,
        durationMs,
      });
      toast.success(t("verify.verificationComplete"));
      onResult({ ok: true, data: res.data });
    } else {
      addHistory({
        id: crypto.randomUUID(),
        ts: now,
        nationalNumber,
        fingerPosition: finger ?? undefined,
        success: false,
        httpStatus: res.status,
        error: res.message,
        endpoint,
        baseUrl: creds.baseUrl,
        requestBody: requestForAudit,
        responseBody: res.body,
        durationMs,
      });
      toast.error(t("verify.verificationFailed", { error: res.message }));
      onResult(res);
    }
    setSubmitting(false);
  };

  // Keyboard shortcuts: Enter → verify, Esc → clear, ⌘K/Ctrl+K → focus input
  useShortcuts([
    {
      key: "Enter",
      handler: (e) => {
        if (!submitting && canSubmit) {
          e.preventDefault();
          void submit(e as unknown as React.FormEvent);
        }
      },
    },
    {
      key: "Escape",
      handler: () => {
        if (!submitting) setNationalNumber("");
      },
    },
    {
      key: "k",
      ctrlOrMeta: true,
      handler: (e) => {
        e.preventDefault();
        inputRef.current?.focus();
        inputRef.current?.select();
      },
    },
  ]);

  const requestBody = {
    nationalNumber: nationalNumber || "…",
    ...(finger && fpBase64
      ? {
          biometrics: {
            fingerPosition: finger,
            image: fpBase64.slice(0, 28) + "…",
          },
        }
      : {}),
  };

  return (
    <div className="max-w-6xl mx-auto p-8 space-y-5">
      <header className="flex items-end justify-between">
        <div>
          <h1 className="text-2xl font-extrabold text-ink">{t("verify.title")}</h1>
          <p className="text-sm text-ink-muted mt-1">{t("verify.subtitle")}</p>
        </div>
        <div className="text-xs text-ink-dim">
          {t("verify.connectedTo")} <span className="font-mono text-ink">{creds.baseUrl}</span>
        </div>
      </header>

      <form onSubmit={submit} className="grid lg:grid-cols-3 gap-5">
        {/* Left column — inputs */}
        <div className="lg:col-span-2 space-y-5">
          {/* Identity */}
          <div className="card p-5">
            <div className="flex items-center gap-2 mb-3">
              <IdCard className="h-4 w-4 text-moi-blue" />
              <h2 className="text-sm font-bold text-ink uppercase tracking-wider">
                {t("verify.customerIdentity")}
              </h2>
            </div>
            <div className="grid md:grid-cols-[1fr_auto] gap-5 items-start">
              {/* Left: input + test IDs */}
              <div className="space-y-3">
                <div>
                  <label className="label">{t("verify.nationalNumber")}</label>
                  <input
                    ref={inputRef}
                    className="input text-lg font-mono tracking-wider"
                    value={nationalNumber}
                    onChange={(e) =>
                      setNationalNumber(formatNationalNumber(e.target.value))
                    }
                    placeholder="1234-5678-9012"
                    maxLength={14}
                    inputMode="numeric"
                    autoFocus
                    spellCheck={false}
                    dir="ltr"
                  />
                  <div className="mt-1.5 flex items-center gap-3 text-[11px]">
                    <div className="flex items-center gap-1.5 text-ink-muted">
                      <span className="font-semibold">{digitCount}</span>
                      <span className="text-ink-dim">/ 12 {t("verify.digitsUnit")}</span>
                    </div>
                    {digitCount > 0 && digitCount < 12 && (
                      <span className="text-moi-red">
                        {t("verify.digitsRemaining", {
                          n: 12 - digitCount,
                          count: 12 - digitCount,
                        })}
                      </span>
                    )}
                    {digitCount === 12 && (
                      <span className="text-moi-green font-semibold">
                        {t("verify.validId")}
                      </span>
                    )}
                    {digitCount > 0 && (
                      <button
                        type="button"
                        onClick={() => setNationalNumber("")}
                        className="text-moi-red font-semibold hover:underline ms-auto"
                      >
                        {t("verify.clear")}
                      </button>
                    )}
                  </div>
                </div>

                <TestIdentityPicker
                  currentValue={nationalNumber}
                  onPick={setNationalNumber}
                />
              </div>

              {/* Right: keypad */}
              <Keypad
                currentLength={nationalNumber.length}
                maxLength={12}
                onKey={(d) =>
                  setNationalNumber((v) => (v.length >= 12 ? v : v + d))
                }
                onBackspace={() => setNationalNumber((v) => v.slice(0, -1))}
              />
            </div>
          </div>

          {/* Biometric */}
          <div className="card p-5">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-2">
                <Fingerprint className="h-4 w-4 text-moi-blue" />
                <h2 className="text-sm font-bold text-ink uppercase tracking-wider">
                  {t("verify.biometric")}
                </h2>
                <span className="text-[11px] text-moi-red font-semibold normal-case tracking-normal">
                  · {t("verify.required")}
                </span>
              </div>
              {finger ? (
                <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-moi-gold/15 border border-moi-gold/40">
                  <span className="h-4 w-4 rounded-full bg-moi-gold text-white text-[10px] font-bold flex items-center justify-center">
                    {finger}
                  </span>
                  <span className="text-xs font-semibold text-moi-blue-dark">
                    {FINGER_LABELS[finger]}
                  </span>
                </div>
              ) : (
                <span className="text-xs text-ink-dim italic">
                  {t("verify.selectFinger")}
                </span>
              )}
            </div>

            <FingerHands selected={finger} onSelect={setFinger} />

            <div className="mt-4">
              <FingerprintCapture
                kind={fpKind}
                imageDataUrl={fpDataUrl}
                imageSizeChars={fpBase64?.length ?? 0}
                onFile={(d, b64) => {
                  setFpKind("file");
                  setFpDataUrl(d);
                  setFpBase64(b64);
                }}
                onSample={() => {
                  setFpKind("sample");
                  setFpDataUrl(null);
                  setFpBase64(SAMPLE_FINGERPRINT_BASE64);
                }}
                onClear={() => {
                  setFpKind("none");
                  setFpDataUrl(null);
                  setFpBase64(null);
                }}
                scanning={submitting}
              />
            </div>

            {finger && !fpBase64 && (
              <div className="mt-3 flex items-start gap-2 text-[11px] text-moi-red bg-moi-red/5 border border-moi-red/30 rounded-lg px-3 py-2">
                <Info className="h-3.5 w-3.5 flex-shrink-0 mt-0.5" />
                <span>{t("verify.fingerprintRequired")}</span>
              </div>
            )}
            {!finger && (
              <div className="mt-3 flex items-start gap-2 text-[11px] text-ink-muted bg-paper-raised rounded-lg px-3 py-2">
                <Info className="h-3.5 w-3.5 text-moi-blue flex-shrink-0 mt-0.5" />
                <span>{t("verify.selectFingerFirst")}</span>
              </div>
            )}
          </div>
        </div>

        {/* Right column — preview & submit */}
        <aside className="lg:col-span-1">
          <div className="card p-5 sticky top-8 space-y-4">
            <div className="flex items-center gap-2">
              <div className="h-7 w-7 rounded-lg bg-moi-blue/10 flex items-center justify-center">
                <ScanLine className="h-4 w-4 text-moi-blue" />
              </div>
              <div>
                <div className="text-sm font-bold text-ink">{t("verify.requestPreview")}</div>
                <div className="text-[11px] text-ink-dim">
                  {t("verify.requestPreviewSubtitle")}
                </div>
              </div>
            </div>

            <pre className="text-[11px] font-mono bg-slate-900 text-slate-200 rounded-lg p-3 overflow-x-auto whitespace-pre-wrap break-all leading-relaxed">
              {JSON.stringify(requestBody, null, 2)}
            </pre>

            <div className="text-[11px] space-y-1.5 border-t border-ink-faint/60 pt-3">
              <div className="flex items-start gap-2">
                <span className="font-bold text-ink-muted uppercase tracking-wider min-w-[60px]">
                  {t("verify.endpoint")}
                </span>
                <span className="font-mono text-ink">POST /api/v1/verify/identity</span>
              </div>
              <div className="flex items-start gap-2">
                <span className="font-bold text-ink-muted uppercase tracking-wider min-w-[60px]">
                  {t("verify.auth")}
                </span>
                <span className="text-ink-muted">HTTP Basic</span>
              </div>
              <div className="flex items-start gap-2">
                <span className="font-bold text-ink-muted uppercase tracking-wider min-w-[60px]">
                  {t("verify.client")}
                </span>
                <span className="font-mono text-ink truncate" title={creds.clientId}>
                  {creds.clientId}
                </span>
              </div>
            </div>

            <button
              type="submit"
              className="btn-primary w-full !py-3"
              disabled={!canSubmit}
            >
              {submitting ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" /> {t("verify.verifying")}
                </>
              ) : (
                <>
                  <ScanLine className="h-4 w-4" /> {t("verify.verifyIdentity")}
                </>
              )}
            </button>

            {!canSubmit && !submitting && (
              <div className="text-[11px] text-moi-red space-y-0.5">
                {nationalNumber.length > 0 && nationalNumber.length < 12 && (
                  <p>· {t("verify.invalidId")}</p>
                )}
                {finger === null && (
                  <p>· {t("verify.mustSelectFinger")}</p>
                )}
                {finger !== null && fpBase64 === null && (
                  <p>· {t("verify.mustCaptureFingerprint")}</p>
                )}
              </div>
            )}
          </div>
        </aside>
      </form>

      <VerifyingOverlay open={submitting} />
    </div>
  );
}
