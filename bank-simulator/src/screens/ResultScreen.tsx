import { useState } from "react";
import {
  CheckCircle2,
  XCircle,
  Copy,
  ChevronDown,
  ChevronUp,
  User,
  CreditCard,
  AlertTriangle,
  MapPin,
  Fingerprint,
  ShieldQuestion,
} from "lucide-react";
import { toast } from "sonner";
import type {
  VerifyResponse,
  VerificationVerdict,
  VerificationBlock,
  NameParts,
  Address,
} from "../types";

type Props = {
  result:
    | { ok: true; data: VerifyResponse }
    | { ok: false; status: number; message: string; body?: unknown };
  onBack: () => void;
};

/**
 * Read the verification verdict from either the real-MOI shape
 * ({@code verification.verification}) or the older wiremock fixture
 * ({@code verification.status}). MOI uses three core verdicts:
 *
 *   MATCH                       — biometric match confirmed
 *   NO_MATCH                    — fingerprint did not match the record
 *   NO_VERIFICATION_POSSIBLE    — record found but no biometric to compare
 */
function readVerdict(v: VerificationBlock | undefined): VerificationVerdict | null {
  return (v?.verification ?? v?.status) ?? null;
}

/** Read the biometric similarity score from either response shape. */
function readBiometricScore(v: VerificationBlock | undefined): number | null {
  const s = v?.biometrics?.score ?? v?.biometric?.score;
  return typeof s === "number" ? s : null;
}

/** True iff MOI says biometric data exists for this person. */
function hasStoredBiometrics(v: VerificationBlock | undefined): boolean | null {
  if (typeof v?.biometrics?.exists === "boolean") return v.biometrics.exists;
  if (typeof v?.biometric?.status === "boolean") return v.biometric.status;
  return null;
}

/** Verdict → tone for the verdict card and badge. */
function verdictTone(verdict: VerificationVerdict | null): {
  label: string;
  border: string;
  gradient: string;
  icon: typeof CheckCircle2;
  iconColor: string;
} {
  switch (verdict) {
    case "MATCH":
      return {
        label: "Identity verified",
        border: "border-moi-green",
        gradient: "from-moi-green/5",
        icon: CheckCircle2,
        iconColor: "text-moi-green",
      };
    case "NO_MATCH":
      return {
        label: "No biometric match",
        border: "border-moi-red",
        gradient: "from-moi-red/5",
        icon: XCircle,
        iconColor: "text-moi-red",
      };
    case "NO_VERIFICATION_POSSIBLE":
      return {
        label: "Person found · no biometric on file",
        border: "border-moi-gold",
        gradient: "from-moi-gold/5",
        icon: ShieldQuestion,
        iconColor: "text-moi-gold",
      };
    default:
      return {
        label: verdict || "Unknown verdict",
        border: "border-ink-faint",
        gradient: "from-paper-raised",
        icon: AlertTriangle,
        iconColor: "text-ink-muted",
      };
  }
}

/** Joins the name parts that exist into a single display string. */
function joinName(n: NameParts | undefined): string | undefined {
  if (!n) return undefined;
  if (n.full) return n.full;
  return [n.first, n.second, n.third, n.fourth, n.last]
    .filter((p): p is string => !!p && p.trim().length > 0)
    .join(" ") || undefined;
}

/** Pull anything non-empty from an Address into a single line. */
function joinAddress(a: Address | undefined): string | undefined {
  if (!a) return undefined;
  const parts = [
    a.village,
    a.locality,
    a.city,
    a.state,
    a.country?.text || a.country?.id,
  ];
  const out = parts.filter((p): p is string => !!p && p.trim().length > 0).join(" · ");
  return out.length ? out : undefined;
}

/**
 * Best-effort error decoding: when MOI returns a 4xx, our middleware now
 * forwards the upstream body. Surface its X-Error-Code / X-Error-Message
 * if the bank server passed them through (under e.g. {@code upstream}).
 */
function describeError(body: unknown): { code?: string; detail?: string } {
  if (!body || typeof body !== "object") return {};
  const b = body as Record<string, unknown>;
  return {
    code: typeof b.errorCode === "number" ? String(b.errorCode) : undefined,
    detail: typeof b.message === "string" ? b.message : undefined,
  };
}

export function ResultScreen({ result, onBack }: Props) {
  const [showRaw, setShowRaw] = useState(false);

  const copy = (text: string) => {
    navigator.clipboard.writeText(text);
    toast.success("Copied");
  };

  // ── Error path ────────────────────────────────────────────────────────────
  if (!result.ok) {
    const { code, detail } = describeError(result.body);
    return (
      <div className="max-w-3xl mx-auto p-8">
        <div className="card p-8 space-y-4">
          <div className="flex items-start gap-4">
            <div className="h-14 w-14 rounded-full bg-moi-red/10 grid place-items-center flex-shrink-0">
              <AlertTriangle className="h-7 w-7 text-moi-red" />
            </div>
            <div className="flex-1">
              <h1 className="text-2xl font-extrabold text-ink">
                Verification failed
              </h1>
              <p className="text-sm text-ink-muted mt-1">
                HTTP {result.status || "network error"}
                {code ? ` · error ${code}` : ""}
              </p>
            </div>
            <button onClick={onBack} className="btn-secondary">
              Try again
            </button>
          </div>

          <div className="card bg-paper-raised p-4 space-y-3">
            <div>
              <div className="text-xs font-bold text-ink-muted uppercase tracking-wider mb-1">
                Message
              </div>
              <pre className="text-sm text-ink whitespace-pre-wrap break-words">
                {detail || result.message}
              </pre>
            </div>
            {result.body ? (
              <div>
                <div className="text-xs font-bold text-ink-muted uppercase tracking-wider mb-1">
                  Response body
                </div>
                <pre className="text-[11px] font-mono text-ink-muted whitespace-pre-wrap break-all">
                  {JSON.stringify(result.body, null, 2)}
                </pre>
              </div>
            ) : null}
          </div>
        </div>
      </div>
    );
  }

  // ── Success path ──────────────────────────────────────────────────────────
  const data = result.data;
  const verification = data.result?.verification;
  const verdict = readVerdict(verification);
  const score = readBiometricScore(verification);
  const stored = hasStoredBiometrics(verification);
  const tone = verdictTone(verdict);
  const VerdictIcon = tone.icon;

  const person = data.result?.person;
  const demo = person?.demographics;
  const names = demo?.names;
  const addrs = demo?.addresses;

  const arabicFull = joinName(names?.arabic);
  const latinFull = joinName(names?.latin);

  return (
    <div className="max-w-5xl mx-auto p-8 space-y-6">
      {/* Verdict card */}
      <div
        className={`card p-8 border-2 ${tone.border} bg-gradient-to-br ${tone.gradient} to-transparent`}
      >
        <div className="flex items-start gap-5">
          <VerdictIcon className={`h-14 w-14 ${tone.iconColor} flex-shrink-0`} />
          <div className="flex-1">
            <h1 className="text-3xl font-extrabold text-ink">{tone.label}</h1>
            <p className="text-sm text-ink-muted mt-1">
              Transaction{" "}
              <button
                onClick={() => copy(data.transaction.id)}
                className="font-mono text-xs text-moi-blue hover:underline"
              >
                {data.transaction.id}
              </button>{" "}
              · {new Date(data.transaction.timestamp).toLocaleString()}
            </p>

            {/* Biometric strip — show score (if any) and stored-biometrics state */}
            <div className="mt-4 flex flex-wrap items-center gap-3">
              {score != null && (
                <div className="inline-flex items-center gap-3 bg-paper rounded-lg px-4 py-2 border border-ink-faint">
                  <Fingerprint className="h-4 w-4 text-moi-blue" />
                  <span className="text-xs font-bold text-ink-muted uppercase">
                    Match score
                  </span>
                  <span className="text-lg font-extrabold text-moi-blue">
                    {score}%
                  </span>
                  <div className="w-32 h-2 bg-paper-raised rounded-full overflow-hidden">
                    <div
                      className={`h-full ${
                        score >= 80
                          ? "bg-moi-green"
                          : score >= 50
                            ? "bg-moi-gold"
                            : "bg-moi-red"
                      }`}
                      style={{ width: `${Math.min(100, Math.max(0, score))}%` }}
                    />
                  </div>
                </div>
              )}
              {stored != null && (
                <div className="inline-flex items-center gap-2 bg-paper rounded-lg px-3 py-2 border border-ink-faint text-xs">
                  <span className="font-bold text-ink-muted uppercase">
                    Biometric on file
                  </span>
                  <span className={stored ? "text-moi-green font-semibold" : "text-ink-dim"}>
                    {stored ? "Yes" : "No"}
                  </span>
                </div>
              )}
              {verdict === "NO_VERIFICATION_POSSIBLE" && (
                <p className="text-xs text-ink-muted italic">
                  The record was found, but no biometric template is enrolled
                  on the MOI side — a match could not be computed.
                </p>
              )}
            </div>
          </div>
          <button onClick={onBack} className="btn-secondary self-start">
            New verification
          </button>
        </div>
      </div>

      {/* Demographics */}
      {demo && (
        <div className="card p-6">
          <div className="flex items-center gap-2 mb-4">
            <User className="h-5 w-5 text-moi-blue" />
            <h2 className="text-base font-bold text-ink">Person details</h2>
          </div>

          <div className="grid md:grid-cols-2 gap-5">
            {arabicFull && (
              <Field label="Name (Arabic)" value={arabicFull} dir="rtl" />
            )}
            {latinFull && <Field label="Name (Latin)" value={latinFull} />}
            <Field
              label="National number"
              value={person?.nationalNumber}
              mono
            />
            <Field
              label="Date of birth"
              value={
                demo.dateOfBirth
                  ? new Date(demo.dateOfBirth).toLocaleDateString()
                  : undefined
              }
            />
            <Field label="Nationality" value={demo.nationality?.id ?? undefined} />
            <Field label="Gender" value={demo.gender?.text} />
            <Field label="Marital status" value={demo.maritalStatus} />
            <Field label="Profession" value={demo.profession} />
            <Field label="Education" value={demo.educationLevel} />
            <Field label="Phone" value={demo.phone} mono />
            <Field label="Email" value={demo.email} mono />
          </div>
        </div>
      )}

      {/* Name breakdown — useful when banks need a specific name part */}
      {names && (names.arabic || names.latin) && (
        <div className="card p-6">
          <div className="flex items-center gap-2 mb-4">
            <User className="h-5 w-5 text-moi-blue" />
            <h2 className="text-base font-bold text-ink">Name breakdown</h2>
            <span className="text-[11px] text-ink-dim italic ms-auto">
              Yemeni naming uses up to 4 parts (first / second / third / fourth)
              — last name is rare in MOI records.
            </span>
          </div>
          <div className="grid md:grid-cols-2 gap-5">
            {names.arabic && (
              <NameTable label="Arabic" parts={names.arabic} dir="rtl" />
            )}
            {names.latin && <NameTable label="Latin" parts={names.latin} />}
          </div>
        </div>
      )}

      {/* Addresses */}
      {addrs && (addrs.birth || addrs.home || addrs.work) && (
        <div className="card p-6">
          <div className="flex items-center gap-2 mb-4">
            <MapPin className="h-5 w-5 text-moi-blue" />
            <h2 className="text-base font-bold text-ink">Addresses</h2>
          </div>
          <div className="grid md:grid-cols-3 gap-4">
            <AddressBlock label="Birth" address={addrs.birth} />
            <AddressBlock label="Home" address={addrs.home} />
            <AddressBlock label="Work" address={addrs.work} />
          </div>
        </div>
      )}

      {/* Cards */}
      {person?.cards && person.cards.length > 0 && (
        <div className="card p-6">
          <div className="flex items-center gap-2 mb-4">
            <CreditCard className="h-5 w-5 text-moi-blue" />
            <h2 className="text-base font-bold text-ink">Identity documents</h2>
          </div>

          <div className="space-y-3">
            {person.cards.map((c, i) => (
              <div
                key={i}
                className="card bg-paper-raised p-4 grid md:grid-cols-2 gap-3"
              >
                <Field label="Type" value={c.type} />
                <Field label="Status" value={c.status} />
                <Field label="Document number" value={c.documentNumber} mono />
                <Field label="Serial number" value={c.serialNumber} mono />
                <Field
                  label="Issued"
                  value={
                    c.issue ? new Date(c.issue).toLocaleDateString() : undefined
                  }
                />
                <Field
                  label="Expires"
                  value={
                    c.expiry
                      ? new Date(c.expiry).toLocaleDateString()
                      : undefined
                  }
                />
                <Field label="Issuing authority" value={c.issuingAuthority} />
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Raw JSON */}
      <div className="card p-6">
        <button
          onClick={() => setShowRaw((v) => !v)}
          className="flex items-center justify-between w-full text-ink-muted hover:text-ink"
        >
          <span className="text-sm font-semibold">
            Raw API response (for developers)
          </span>
          {showRaw ? (
            <ChevronUp className="h-4 w-4" />
          ) : (
            <ChevronDown className="h-4 w-4" />
          )}
        </button>
        {showRaw && (
          <div className="mt-4">
            <div className="flex items-center justify-end mb-2">
              <button
                onClick={() => copy(JSON.stringify(data, null, 2))}
                className="btn-ghost !text-xs"
              >
                <Copy className="h-3 w-3" /> Copy
              </button>
            </div>
            <pre className="text-[11px] font-mono bg-paper-raised rounded-lg p-4 overflow-auto max-h-96 text-ink whitespace-pre-wrap break-all">
              {JSON.stringify(data, null, 2)}
            </pre>
          </div>
        )}
      </div>
    </div>
  );
}

// ── Local helpers ───────────────────────────────────────────────────────────

function Field({
  label,
  value,
  mono,
  dir,
}: {
  label: string;
  value?: string | null;
  mono?: boolean;
  dir?: "rtl" | "ltr";
}) {
  return (
    <div>
      <div className="text-[11px] font-bold uppercase tracking-wider text-ink-dim">
        {label}
      </div>
      <div
        className={`mt-0.5 text-sm ${mono ? "font-mono" : ""} ${value ? "text-ink" : "text-ink-dim italic"}`}
        dir={dir}
      >
        {value || "—"}
      </div>
    </div>
  );
}

function NameTable({
  label,
  parts,
  dir,
}: {
  label: string;
  parts: NameParts;
  dir?: "rtl" | "ltr";
}) {
  const rows: Array<[string, string | null | undefined]> = [
    ["First", parts.first],
    ["Second", parts.second],
    ["Third", parts.third],
    ["Fourth", parts.fourth],
    ["Last", parts.last],
  ];
  return (
    <div className="card bg-paper-raised p-4">
      <div className="text-[11px] font-bold uppercase tracking-wider text-ink-dim mb-2">
        {label}
      </div>
      <div className="space-y-1.5" dir={dir}>
        {rows.map(([k, v]) => (
          <div key={k} className="flex items-center justify-between gap-3">
            <span className="text-[11px] text-ink-dim uppercase tracking-wide">
              {k}
            </span>
            <span
              className={`text-sm ${v ? "text-ink" : "text-ink-dim italic"}`}
            >
              {v || "—"}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}

function AddressBlock({
  label,
  address,
}: {
  label: string;
  address?: Address;
}) {
  const oneLine = joinAddress(address);
  return (
    <div className="card bg-paper-raised p-4 space-y-2">
      <div className="text-[11px] font-bold uppercase tracking-wider text-ink-dim">
        {label}
      </div>
      {oneLine ? (
        <>
          <div className="text-sm text-ink leading-relaxed">{oneLine}</div>
          <div className="grid grid-cols-2 gap-x-2 gap-y-1 pt-2 border-t border-ink-faint/60">
            {address?.village && (
              <KV k="Village" v={address.village} />
            )}
            {address?.locality && <KV k="Locality" v={address.locality} />}
            {address?.city && <KV k="City" v={address.city} />}
            {address?.state && <KV k="State" v={address.state} />}
            {address?.country?.id && (
              <KV
                k="Country"
                v={address.country.text || address.country.id}
              />
            )}
            {address?.address && <KV k="Detail" v={address.address} />}
            {address?.extra && <KV k="Extra" v={address.extra} />}
            {address?.placeOfWork && (
              <KV k="Workplace" v={address.placeOfWork} />
            )}
          </div>
        </>
      ) : (
        <div className="text-xs text-ink-dim italic">Not on file</div>
      )}
    </div>
  );
}

function KV({ k, v }: { k: string; v: string }) {
  return (
    <div className="text-[11px]">
      <span className="text-ink-dim">{k}: </span>
      <span className="text-ink">{v}</span>
    </div>
  );
}
