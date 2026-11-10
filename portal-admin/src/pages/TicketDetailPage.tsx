import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";
import { ArrowLeft, Lock, Send, UserCheck } from "lucide-react";
import { getTicket, replyToTicket, updateTicket } from "@/api/tickets";
import { Card, CardBody, CardHeader, CardTitle } from "@/components/ui/Card";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Label } from "@/components/ui/Label";
import { Select } from "@/components/ui/Select";
import { Textarea } from "@/components/ui/Textarea";
import { PageLoader } from "@/components/ui/Spinner";
import { TICKET_PRIORITY_TONE, TICKET_STATUS_TONE, TicketThread } from "@/components/TicketThread";
import { formatDate } from "@/lib/format";
import { useAuth } from "@/lib/auth";
import { canReplyTickets } from "@/lib/access";
import {
  TICKET_PRIORITIES,
  TICKET_STATUSES,
  type Ticket,
  type TicketPriority,
  type TicketStatus,
} from "@/types/api";

export function TicketDetailPage() {
  const { id = "" } = useParams();
  const { t } = useTranslation();
  const qc = useQueryClient();
  const role = useAuth((s) => s.role);
  const me = useAuth((s) => s.email);
  const writable = canReplyTickets(role);

  const ticketQ = useQuery({ queryKey: ["ticket", id], queryFn: () => getTicket(id), refetchInterval: 30_000 });

  const [message, setMessage] = useState("");
  const [internal, setInternal] = useState(false);
  const [nextStatus, setNextStatus] = useState<TicketStatus>("WAITING_ON_CLIENT");

  function onSaved(tk: Ticket) {
    qc.setQueryData(["ticket", id], tk);
    qc.invalidateQueries({ queryKey: ["tickets"] });
    qc.invalidateQueries({ queryKey: ["ticket-counts"] });
  }

  const replyMut = useMutation({
    mutationFn: () => replyToTicket(id, message, internal, internal ? undefined : nextStatus),
    onSuccess: (tk) => {
      onSaved(tk);
      setMessage("");
      toast.success(internal ? t("tickets.reply.noteAdded") : t("tickets.reply.sent"));
    },
  });

  const updateMut = useMutation({
    mutationFn: (patch: { status?: TicketStatus; priority?: TicketPriority; assignedTo?: string }) =>
      updateTicket(id, patch),
    onSuccess: (tk) => {
      onSaved(tk);
      toast.success(t("tickets.updated"));
    },
  });

  if (ticketQ.isLoading || !ticketQ.data) return <PageLoader />;
  const tk = ticketQ.data;
  const closed = tk.status === "CLOSED";

  return (
    <div className="space-y-4">
      <Link to="/tickets" className="inline-flex items-center gap-1.5 text-xs text-text-muted hover:text-text">
        <ArrowLeft className="h-3.5 w-3.5 rtl:rotate-180" />
        {t("tickets.backToList")}
      </Link>

      <div>
        <div className="flex flex-wrap items-center gap-2 mb-1">
          <span className="font-mono text-text-muted">#{tk.ticketNo}</span>
          <Badge tone={TICKET_STATUS_TONE[tk.status]}>{t(`tickets.status.${tk.status}`)}</Badge>
          <Badge tone={TICKET_PRIORITY_TONE[tk.priority]}>{t(`tickets.priority.${tk.priority}`)}</Badge>
          <Badge tone="neutral">{t(`tickets.category.${tk.category}`)}</Badge>
        </div>
        <h1 dir="auto" className="text-2xl font-bold tracking-tight text-left rtl:text-right">
          {tk.subject}
        </h1>
        <div className="text-sm text-text-muted mt-1">
          <Link to={`/tenants/${tk.tenantId}`} className="hover:text-text underline-offset-2 hover:underline">
            {tk.tenantName ?? tk.tenantId}
          </Link>
          {" · "}
          {tk.createdBy}
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-[1fr_300px] gap-4 items-start">
        <div className="space-y-4">
          <Card>
            <CardBody className="p-5">
              <TicketThread messages={tk.messages ?? []} viewer="staff" />
            </CardBody>
          </Card>

          {writable && (
            <Card>
              <CardBody className="p-5 space-y-3">
                {closed && !internal && (
                  <p className="text-xs text-accent-amber">{t("tickets.reply.closedHint")}</p>
                )}
                <Textarea
                  dir="auto"
                  rows={5}
                  value={message}
                  onChange={(e) => setMessage(e.target.value)}
                  placeholder={internal ? t("tickets.reply.notePlaceholder") : t("tickets.reply.placeholder")}
                  className={internal ? "border-accent-amber/40 bg-accent-amber/5" : undefined}
                />
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <label className="flex items-center gap-2 text-xs text-text-muted cursor-pointer select-none">
                    <input
                      type="checkbox"
                      checked={internal}
                      onChange={(e) => setInternal(e.target.checked)}
                      className="accent-amber-500"
                    />
                    <Lock className="h-3.5 w-3.5" />
                    {t("tickets.reply.internalToggle")}
                  </label>
                  <div className="flex items-center gap-2">
                    {!internal && (
                      <>
                        <span className="text-xs text-text-muted">{t("tickets.reply.thenSet")}</span>
                        <div className="w-48">
                          <Select<TicketStatus>
                            value={nextStatus}
                            onChange={setNextStatus}
                            options={TICKET_STATUSES.map((s) => ({ value: s, label: t(`tickets.status.${s}`) }))}
                          />
                        </div>
                      </>
                    )}
                    <Button
                      leftIcon={internal ? <Lock className="h-4 w-4" /> : <Send className="h-4 w-4 rtl:rotate-180" />}
                      loading={replyMut.isPending}
                      disabled={!message.trim() || (closed && !internal)}
                      onClick={() => replyMut.mutate()}
                    >
                      {internal ? t("tickets.reply.addNote") : t("tickets.reply.send")}
                    </Button>
                  </div>
                </div>
              </CardBody>
            </Card>
          )}
        </div>

        <Card>
          <CardHeader>
            <CardTitle>{t("tickets.details")}</CardTitle>
          </CardHeader>
          <CardBody className="space-y-4 text-sm">
            <div>
              <Label>{t("common.status")}</Label>
              <Select<TicketStatus>
                value={tk.status}
                disabled={!writable || updateMut.isPending}
                onChange={(s) => updateMut.mutate({ status: s })}
                options={TICKET_STATUSES.map((s) => ({ value: s, label: t(`tickets.status.${s}`) }))}
              />
            </div>
            <div>
              <Label>{t("tickets.fields.priority")}</Label>
              <Select<TicketPriority>
                value={tk.priority}
                disabled={!writable || updateMut.isPending}
                onChange={(p) => updateMut.mutate({ priority: p })}
                options={TICKET_PRIORITIES.map((p) => ({ value: p, label: t(`tickets.priority.${p}`) }))}
              />
            </div>
            <div>
              <Label>{t("tickets.fields.assignedTo")}</Label>
              <div className="text-text">{tk.assignedTo ?? t("tickets.unassigned")}</div>
              {writable && (
                <div className="flex gap-2 mt-2">
                  {tk.assignedTo !== me && me && (
                    <Button
                      size="sm"
                      variant="secondary"
                      leftIcon={<UserCheck className="h-3.5 w-3.5" />}
                      onClick={() => updateMut.mutate({ assignedTo: me })}
                    >
                      {t("tickets.assignToMe")}
                    </Button>
                  )}
                  {tk.assignedTo && (
                    <Button size="sm" variant="ghost" onClick={() => updateMut.mutate({ assignedTo: "" })}>
                      {t("tickets.unassign")}
                    </Button>
                  )}
                </div>
              )}
            </div>
            {tk.reference && (
              <div>
                <Label>{t("tickets.fields.reference")}</Label>
                <div className="font-mono text-xs break-all">{tk.reference}</div>
              </div>
            )}
            <div className="grid grid-cols-2 gap-3 text-xs">
              <div>
                <div className="text-text-dim mb-0.5">{t("common.createdAt")}</div>
                <div>{formatDate(tk.createdAt)}</div>
              </div>
              <div>
                <div className="text-text-dim mb-0.5">{t("tickets.fields.lastActivity")}</div>
                <div>{formatDate(tk.lastMessageAt)}</div>
              </div>
              {tk.resolvedAt && (
                <div>
                  <div className="text-text-dim mb-0.5">{t("tickets.fields.resolvedAt")}</div>
                  <div>{formatDate(tk.resolvedAt)}</div>
                </div>
              )}
              {tk.closedAt && (
                <div>
                  <div className="text-text-dim mb-0.5">{t("tickets.fields.closedAt")}</div>
                  <div>{formatDate(tk.closedAt)}</div>
                </div>
              )}
            </div>
          </CardBody>
        </Card>
      </div>
    </div>
  );
}
