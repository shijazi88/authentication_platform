import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { Search, Check, AlertTriangle, ShieldAlert, FileDown } from "lucide-react";
import { PageHeader } from "@/components/ui/PageHeader";
import { Card, CardBody } from "@/components/ui/Card";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { CopyButton } from "@/components/ui/CopyButton";
import { cn } from "@/lib/cn";
import {
  CATEGORIES,
  ENTRIES,
  UI,
  CHECKLIST,
  type Severity,
  type EscalateLevel,
} from "@/data/errorPlaybook";

type Lang = "en" | "ar";

type Tone = "emerald" | "amber" | "rose";
const SEV: Record<Severity, { tone: Tone; text: string; bar: string; dot: string; soft: string; label: { en: string; ar: string } }> = {
  guide: { tone: "emerald", text: "text-accent-emerald", bar: "bg-accent-emerald", dot: "bg-accent-emerald", soft: "bg-accent-emerald/10 border-accent-emerald/25", label: { en: "Guide the customer", ar: "أرشِد العميل" } },
  account: { tone: "amber", text: "text-accent-amber", bar: "bg-accent-amber", dot: "bg-accent-amber", soft: "bg-accent-amber/10 border-accent-amber/25", label: { en: "Account / config", ar: "الحساب/الإعداد" } },
  escalate: { tone: "rose", text: "text-accent-rose", bar: "bg-accent-rose", dot: "bg-accent-rose", soft: "bg-accent-rose/10 border-accent-rose/25", label: { en: "Escalate", ar: "تصعيد" } },
};

function EscalateMark({ level }: { level: EscalateLevel }) {
  if (level === "no") return <Check className="h-4 w-4 text-accent-emerald shrink-0" />;
  if (level === "yes") return <AlertTriangle className="h-4 w-4 text-accent-rose shrink-0" />;
  return <AlertTriangle className="h-4 w-4 text-accent-amber shrink-0" />;
}

const ENVELOPE = `{
  "timestamp": "2026-08-19T10:45:31Z",
  "errorCode": 1302,            // ← match this to a card
  "error":     "UNPROCESSABLE_ENTITY",
  "message":   "Fingerprint did not match the national ID",
  "requestId": "019fad7e-6726-7436-9b0a-64c2255a1477"
}`;

function Lbl({ children }: { children: React.ReactNode }) {
  return (
    <div className="text-[10.5px] font-mono font-semibold uppercase tracking-[0.13em] text-text-dim mb-1.5">
      {children}
    </div>
  );
}

export function SupportPage() {
  const { i18n } = useTranslation();
  const lang: Lang = i18n.language?.startsWith("ar") ? "ar" : "en";
  const [query, setQuery] = useState("");
  const q = query.trim().toLowerCase();

  const filtered = useMemo(
    () =>
      ENTRIES.filter((e) => {
        if (!q) return true;
        return `${e.code} ${e.enumName} ${e.keywords} ${e.name.en} ${e.name.ar}`
          .toLowerCase()
          .includes(q);
      }),
    [q],
  );

  function downloadPdf() {
    const a = document.createElement("a");
    a.href = `/motabiq-support-playbook-${lang}.pdf`;
    a.download = `MOTABIQ-Support-Error-Playbook-${lang.toUpperCase()}.pdf`;
    document.body.appendChild(a);
    a.click();
    a.remove();
  }

  return (
    <div>
      <PageHeader
        title={UI.title[lang]}
        description={UI.subtitle[lang]}
        actions={
          <Button
            variant="secondary"
            size="sm"
            leftIcon={<FileDown className="h-4 w-4" />}
            onClick={downloadPdf}
          >
            {UI.exportPdf[lang]}
          </Button>
        }
      />

      {/* Search */}
      <div className="no-print mb-6 flex items-center gap-3 flex-wrap">
        <div className="relative flex-1 min-w-[240px] max-w-lg">
          <Search className="pointer-events-none absolute start-3 top-1/2 -translate-y-1/2 h-4 w-4 text-text-dim" />
          <Input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder={UI.search[lang]}
            className="ps-9"
            aria-label={UI.search[lang]}
          />
        </div>
        <span className="font-mono text-xs text-text-dim tabular-nums">
          {filtered.length} {UI.codes[lang]}
        </span>
      </div>

      {/* Legend */}
      <div className="mb-6 flex flex-wrap gap-2">
        {([
          ["guide", UI.legendGuide[lang]],
          ["account", UI.legendAccount[lang]],
          ["escalate", UI.legendEscalate[lang]],
        ] as [Severity, string][]).map(([sev, label]) => (
          <span
            key={sev}
            className="inline-flex items-center gap-2 rounded-full border border-border/15 bg-bg-elevated/40 px-3 py-1.5 text-[13px] font-medium text-text-muted"
          >
            <span className={cn("h-2 w-2 rounded-full", SEV[sev].dot)} />
            {label}
          </span>
        ))}
      </div>

      {/* Primer */}
      <div className="mb-8 grid gap-4 lg:grid-cols-2">
        <Card>
          <CardBody className="p-5">
            <h3 className="mb-3 text-sm font-semibold text-text">{UI.envelopeTitle[lang]}</h3>
            <pre
              dir="ltr"
              className="overflow-x-auto rounded-lg border border-border/10 bg-bg-elevated/40 p-4 font-mono text-[12.5px] leading-relaxed text-text"
            >
              {ENVELOPE}
            </pre>
          </CardBody>
        </Card>
        <Card>
          <CardBody className="p-5">
            <h3 className="mb-2 text-sm font-semibold text-text">{UI.rulesTitle[lang]}</h3>
            {[UI.rule1[lang], UI.rule2[lang], UI.rule3[lang]].map((r, i) => (
              <div key={i} className="flex gap-3 border-t border-border/10 py-2.5 first:border-t-0">
                <span className="font-mono text-sm font-bold text-accent-cyan">{i + 1}</span>
                <p className="m-0 text-[13.5px] leading-snug text-text-muted">{r}</p>
              </div>
            ))}
          </CardBody>
        </Card>
      </div>

      {/* Error cards grouped by category */}
      <div className="space-y-8">
        {CATEGORIES.map((cat) => {
          const items = filtered.filter((e) => e.category === cat.key);
          if (!items.length) return null;
          return (
            <section key={cat.key}>
              <div className="mb-3 flex items-baseline gap-3">
                <h2 className="text-xl font-bold tracking-tight text-text">{cat.title[lang]}</h2>
                <span className="font-mono text-xs text-text-dim">{cat.hint[lang]}</span>
              </div>
              <div className="space-y-3.5">
                {items.map((e) => {
                  const s = SEV[e.severity];
                  return (
                    <Card key={e.code} className="print-avoid relative overflow-hidden">
                      <span className={cn("absolute inset-y-0 start-0 w-1", s.bar)} aria-hidden="true" />
                      {/* head */}
                      <div className="flex flex-wrap items-center gap-4 px-5 py-4">
                        <span className={cn("font-mono text-3xl font-bold leading-none tabular-nums", s.text)}>
                          {e.code}
                        </span>
                        <div className="me-auto flex min-w-[180px] flex-col gap-0.5">
                          <span className="text-[15.5px] font-semibold text-text">{e.name[lang]}</span>
                          <span className="font-mono text-[11.5px] uppercase tracking-wide text-text-dim">
                            {e.enumName}
                          </span>
                        </div>
                        <div className="flex flex-wrap items-center gap-2">
                          <span className="rounded-md border border-border/15 bg-bg-elevated/40 px-2 py-1 font-mono text-[11.5px] font-semibold text-text">
                            HTTP {e.http}
                          </span>
                          <Badge tone={s.tone}>{s.label[lang]}</Badge>
                        </div>
                      </div>
                      {/* body */}
                      <div className="grid border-t border-border/10 sm:grid-cols-2">
                        <div className="border-border/10 p-4 sm:border-e">
                          <Lbl>{UI.meaning[lang]}</Lbl>
                          <p className="m-0 text-[14px] leading-relaxed text-text">{e.meaning[lang]}</p>
                        </div>
                        <div className="border-t border-border/10 p-4 sm:border-t-0">
                          <Lbl>{UI.action[lang]}</Lbl>
                          <p className="m-0 text-[14px] leading-relaxed text-text">{e.action[lang]}</p>
                        </div>
                        <div className="border-t border-border/10 p-4 sm:col-span-2">
                          <Lbl>{UI.reply[lang]}</Lbl>
                          <div className={cn("relative rounded-lg border p-3 pe-10", s.soft)}>
                            <p className="m-0 text-[14px] leading-relaxed text-text">“{e.reply[lang]}”</p>
                            <span className="no-print absolute end-2 top-2">
                              <CopyButton value={e.reply[lang]} />
                            </span>
                          </div>
                        </div>
                        <div className="flex items-center gap-2 border-t border-border/10 p-4 sm:col-span-2">
                          <Lbl>{UI.escalate[lang]}</Lbl>
                          <span className="inline-flex items-center gap-2 text-[13.5px] font-semibold text-text-muted">
                            <EscalateMark level={e.escalateLevel} />
                            {e.escalate[lang]}
                          </span>
                        </div>
                      </div>
                    </Card>
                  );
                })}
              </div>
            </section>
          );
        })}

        {filtered.length === 0 && (
          <div className="py-16 text-center text-sm text-text-muted">{UI.noResult[lang]}</div>
        )}
      </div>

      {/* Escalation checklist */}
      <section className="mt-10">
        <div className="mb-3 flex items-baseline gap-3">
          <h2 className="text-xl font-bold tracking-tight text-text">{UI.escalationTitle[lang]}</h2>
        </div>
        <Card>
          <CardBody className="p-5">
            <p className="mb-3 text-sm text-text">{UI.escalationLead[lang]}</p>
            <ul className="grid gap-x-8 sm:grid-cols-2">
              {CHECKLIST.map((item, i) => (
                <li key={i} className="flex items-start gap-2.5 border-t border-border/10 py-2.5 text-[14px] text-text">
                  <Check className="mt-0.5 h-4 w-4 shrink-0 text-accent-cyan" />
                  <span>{item[lang]}</span>
                </li>
              ))}
            </ul>
            <div className="mt-4 flex items-start gap-2.5 rounded-lg border border-accent-rose/25 bg-accent-rose/10 p-3.5 text-[13.5px] text-text">
              <ShieldAlert className="mt-0.5 h-4 w-4 shrink-0 text-accent-rose" />
              <span>
                <b className="text-accent-rose">{UI.privacyTitle[lang]}</b> {UI.privacyBody[lang]}
              </span>
            </div>
          </CardBody>
        </Card>
      </section>
    </div>
  );
}
