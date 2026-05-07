import { useEffect, useRef, useState } from "react";
import { Search, Building2, Receipt } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useQuery } from "@tanstack/react-query";
import { searchAll, type SearchResults } from "@/api/search";
import { Badge, statusTone } from "@/components/ui/Badge";
import { formatDate, shortId } from "@/lib/format";

const MIN_LEN = 2;
const DEBOUNCE_MS = 300;

export function GlobalSearch() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [raw, setRaw] = useState("");
  const [debounced, setDebounced] = useState("");
  const [open, setOpen] = useState(false);
  const wrapRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  // Debounce input → query
  useEffect(() => {
    const id = setTimeout(() => setDebounced(raw.trim()), DEBOUNCE_MS);
    return () => clearTimeout(id);
  }, [raw]);

  const enabled = debounced.length >= MIN_LEN;
  const q = useQuery<SearchResults>({
    queryKey: ["global-search", debounced],
    queryFn: () => searchAll(debounced),
    enabled,
    staleTime: 5_000,
  });

  // Close on click-away
  useEffect(() => {
    function onDocClick(e: MouseEvent) {
      if (!wrapRef.current) return;
      if (!wrapRef.current.contains(e.target as Node)) setOpen(false);
    }
    document.addEventListener("mousedown", onDocClick);
    return () => document.removeEventListener("mousedown", onDocClick);
  }, []);

  function navigateTo(path: string) {
    setOpen(false);
    setRaw("");
    setDebounced("");
    navigate(path);
  }

  function onKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    if (e.key === "Escape") {
      setOpen(false);
      inputRef.current?.blur();
    }
  }

  const tenants = q.data?.tenants ?? [];
  const txs = q.data?.transactions ?? [];
  const empty =
    enabled && !q.isLoading && tenants.length === 0 && txs.length === 0;

  return (
    <div ref={wrapRef} className="relative flex-1 max-w-md">
      <Search className="absolute start-3 top-1/2 -translate-y-1/2 h-4 w-4 text-text-dim pointer-events-none" />
      <input
        ref={inputRef}
        type="text"
        value={raw}
        onChange={(e) => {
          setRaw(e.target.value);
          setOpen(true);
        }}
        onFocus={() => raw && setOpen(true)}
        onKeyDown={onKeyDown}
        placeholder={t("topbar.searchPlaceholder")}
        className="w-full h-9 ps-9 pe-3 rounded-lg bg-bg-elevated/40 border border-border/15 text-sm placeholder:text-text-dim focus:outline-none focus:border-border/30"
      />

      {open && enabled && (
        <div className="absolute start-0 top-full mt-1.5 w-[28rem] max-w-[calc(100vw-2rem)] rounded-lg border border-border/15 bg-bg-elevated/95 backdrop-blur shadow-xl z-50 overflow-hidden">
          {q.isLoading && (
            <div className="px-4 py-3 text-xs text-text-muted">
              {t("common.loading")}
            </div>
          )}

          {q.isError && (
            <div className="px-4 py-3 text-xs text-accent-rose">
              {t("topbar.search.error")}
            </div>
          )}

          {empty && (
            <div className="px-4 py-3 text-xs text-text-muted italic">
              {t("topbar.search.noResults")}
            </div>
          )}

          {tenants.length > 0 && (
            <Section label={t("topbar.search.clients")}>
              {tenants.map((tn) => (
                <button
                  key={tn.id}
                  type="button"
                  onClick={() => navigateTo(`/tenants/${tn.id}`)}
                  className="w-full flex items-center gap-3 px-3 py-2 text-start hover:bg-bg-elevated/80 transition-colors"
                >
                  <Building2 className="h-4 w-4 text-text-muted shrink-0" />
                  <div className="flex-1 min-w-0">
                    <div className="text-sm text-text truncate">
                      {tn.legalName}
                    </div>
                    <div className="text-[11px] text-text-muted truncate font-mono">
                      {tn.code}
                      {tn.contactEmail ? ` · ${tn.contactEmail}` : ""}
                    </div>
                  </div>
                  <Badge tone={tn.status === "ACTIVE" ? "emerald" : "neutral"}>
                    {tn.status}
                  </Badge>
                </button>
              ))}
            </Section>
          )}

          {txs.length > 0 && (
            <Section label={t("topbar.search.transactions")}>
              {txs.map((tx) => (
                <button
                  key={tx.id}
                  type="button"
                  onClick={() => navigateTo(`/transactions/${tx.id}`)}
                  className="w-full flex items-center gap-3 px-3 py-2 text-start hover:bg-bg-elevated/80 transition-colors"
                >
                  <Receipt className="h-4 w-4 text-text-muted shrink-0" />
                  <div className="flex-1 min-w-0">
                    <div className="text-sm text-text truncate font-mono">
                      {shortId(tx.id, 14)}
                    </div>
                    <div className="text-[11px] text-text-muted truncate">
                      {formatDate(tx.createdAt)}
                      {tx.providerRequestId
                        ? ` · ${tx.providerRequestId}`
                        : ""}
                    </div>
                  </div>
                  <Badge tone={statusTone(tx.status)}>{tx.status}</Badge>
                </button>
              ))}
            </Section>
          )}
        </div>
      )}
    </div>
  );
}

function Section({
  label,
  children,
}: {
  label: string;
  children: React.ReactNode;
}) {
  return (
    <div>
      <div className="px-3 pt-2 pb-1 text-[10px] uppercase tracking-wider text-text-dim">
        {label}
      </div>
      <div className="pb-1">{children}</div>
    </div>
  );
}
