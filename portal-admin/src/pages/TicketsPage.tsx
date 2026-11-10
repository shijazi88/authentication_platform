import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Inbox, Search, CornerDownLeft } from "lucide-react";
import { getTicketAssignees, getTicketCounts, listTickets } from "@/api/tickets";
import { listTenants } from "@/api/tenants";
import { PageHeader } from "@/components/ui/PageHeader";
import { Card } from "@/components/ui/Card";
import { Table, TBody, THead, Th, Td, Tr } from "@/components/ui/Table";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Select } from "@/components/ui/Select";
import { EmptyState } from "@/components/ui/EmptyState";
import { PageLoader } from "@/components/ui/Spinner";
import { TICKET_PRIORITY_TONE, TICKET_STATUS_TONE } from "@/components/TicketThread";
import { cn } from "@/lib/cn";
import { formatRelative } from "@/lib/format";
import { TICKET_STATUSES, type TicketStatus } from "@/types/api";

type Tab = TicketStatus | "ACTIVE" | "ALL";
const TABS: Tab[] = ["ACTIVE", ...TICKET_STATUSES, "ALL"];
const PAGE_SIZE = 25;

export function TicketsPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [tab, setTab] = useState<Tab>("ACTIVE");
  const [tenantId, setTenantId] = useState<string>("");
  const [q, setQ] = useState("");
  const [page, setPage] = useState(0);

  const countsQ = useQuery({ queryKey: ["ticket-counts"], queryFn: getTicketCounts, refetchInterval: 60_000 });
  const tenantsQ = useQuery({ queryKey: ["tenants"], queryFn: listTenants });
  const assigneesQ = useQuery({ queryKey: ["ticket-assignees"], queryFn: getTicketAssignees, staleTime: 5 * 60_000 });
  const nameOf = (email: string) => assigneesQ.data?.find((a) => a.email === email)?.displayName || email;
  const ticketsQ = useQuery({
    queryKey: ["tickets", tab, tenantId, q, page],
    queryFn: () => listTickets({ status: tab, tenantId: tenantId || undefined, q, page, size: PAGE_SIZE }),
    refetchInterval: 60_000,
  });

  const counts = countsQ.data;
  function countFor(tb: Tab): number | null {
    if (!counts) return null;
    if (tb === "ACTIVE") return counts.active;
    if (tb === "ALL") return null;
    return counts.byStatus[tb] ?? 0;
  }

  const total = ticketsQ.data?.totalElements ?? 0;
  const pages = Math.max(1, Math.ceil(total / PAGE_SIZE));

  return (
    <div>
      <PageHeader title={t("tickets.title")} description={t("tickets.subtitle")} />

      <div className="flex flex-wrap gap-1.5 mb-4">
        {TABS.map((tb) => {
          const n = countFor(tb);
          return (
            <button
              key={tb}
              onClick={() => {
                setTab(tb);
                setPage(0);
              }}
              className={cn(
                "h-8 px-3 rounded-md text-xs font-medium border transition-all flex items-center gap-1.5",
                tab === tb
                  ? "bg-bg-hover/60 border-border/25 text-text"
                  : "border-transparent text-text-muted hover:text-text hover:bg-bg-hover/40",
              )}
            >
              {t(`tickets.tabs.${tb}`)}
              {n !== null && n > 0 && <span className="text-[10px] text-text-dim">{n}</span>}
            </button>
          );
        })}
      </div>

      <div className="flex flex-wrap gap-3 mb-4">
        <div className="w-64">
          <Select<string>
            value={tenantId}
            onChange={(v) => {
              setTenantId(v);
              setPage(0);
            }}
            options={[
              { value: "", label: t("tickets.allClients") },
              ...(tenantsQ.data ?? []).map((tn) => ({ value: tn.id, label: tn.legalName })),
            ]}
          />
        </div>
        <div className="w-72">
          <Input
            leftIcon={<Search className="h-4 w-4" />}
            placeholder={t("tickets.searchPlaceholder")}
            value={q}
            onChange={(e) => {
              setQ(e.target.value);
              setPage(0);
            }}
          />
        </div>
      </div>

      <Card>
        {ticketsQ.isLoading ? (
          <PageLoader />
        ) : ticketsQ.data?.content.length ? (
          <>
            <Table>
              <THead>
                <Tr>
                  <Th>#</Th>
                  <Th>{t("tickets.fields.subject")}</Th>
                  <Th>{t("tickets.fields.client")}</Th>
                  <Th>{t("tickets.fields.priority")}</Th>
                  <Th>{t("common.status")}</Th>
                  <Th>{t("tickets.fields.lastActivity")}</Th>
                  <Th>{t("tickets.fields.assignedTo")}</Th>
                </Tr>
              </THead>
              <TBody>
                {ticketsQ.data.content.map((tk) => {
                  const clientWaiting =
                    tk.lastMessageBy === "CLIENT" && tk.status !== "RESOLVED" && tk.status !== "CLOSED";
                  return (
                    <Tr key={tk.id} className="cursor-pointer" onClick={() => navigate(`/tickets/${tk.id}`)}>
                      <Td className="font-mono text-text">{tk.ticketNo}</Td>
                      <Td>
                        <div dir="auto" className="text-text font-medium truncate max-w-[320px] text-left rtl:text-right">
                          {tk.subject}
                        </div>
                        <div className="text-xs text-text-dim">{t(`tickets.category.${tk.category}`)}</div>
                      </Td>
                      <Td className="text-text-muted">{tk.tenantName ?? "—"}</Td>
                      <Td>
                        <Badge tone={TICKET_PRIORITY_TONE[tk.priority]} className="whitespace-nowrap">{t(`tickets.priority.${tk.priority}`)}</Badge>
                      </Td>
                      <Td>
                        <Badge tone={TICKET_STATUS_TONE[tk.status]} className="whitespace-nowrap">{t(`tickets.status.${tk.status}`)}</Badge>
                      </Td>
                      <Td className="text-text-muted text-xs">
                        <div className="flex items-center gap-1.5">
                          {clientWaiting && (
                            <CornerDownLeft
                              className="h-3.5 w-3.5 text-accent-amber shrink-0"
                              aria-label={t("tickets.clientReplied")}
                            />
                          )}
                          <span title={tk.lastMessageAt}>{formatRelative(tk.lastMessageAt)}</span>
                        </div>
                      </Td>
                      <Td className="text-text-muted text-xs">{tk.assignedTo ? nameOf(tk.assignedTo) : t("tickets.unassigned")}</Td>
                    </Tr>
                  );
                })}
              </TBody>
            </Table>
            {pages > 1 && (
              <div className="flex items-center justify-end gap-2 px-4 py-3 border-t border-border/10 text-xs text-text-muted">
                <span>
                  {t("common.page")} {page + 1} {t("common.of")} {pages}
                </span>
                <Button size="sm" variant="secondary" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
                  {t("common.prev")}
                </Button>
                <Button
                  size="sm"
                  variant="secondary"
                  disabled={page + 1 >= pages}
                  onClick={() => setPage((p) => p + 1)}
                >
                  {t("common.next")}
                </Button>
              </div>
            )}
          </>
        ) : (
          <EmptyState
            icon={<Inbox className="h-5 w-5" />}
            title={t("tickets.empty.title")}
            description={t("tickets.empty.description")}
          />
        )}
      </Card>
    </div>
  );
}
