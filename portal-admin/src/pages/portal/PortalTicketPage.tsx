import { useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";
import { ArrowLeft, CheckCircle2, MessageSquareReply, Send } from "lucide-react";
import { confirmMyTicket, getMyTicket, replyToMyTicket } from "@/api/tenant";
import { Card, CardBody } from "@/components/ui/Card";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Textarea } from "@/components/ui/Textarea";
import { PageLoader } from "@/components/ui/Spinner";
import { TICKET_STATUS_TONE, TicketThread } from "@/components/TicketThread";
import { formatDate } from "@/lib/format";
import type { Ticket } from "@/types/api";

export function PortalTicketPage() {
  const { id = "" } = useParams();
  const { t } = useTranslation();
  const qc = useQueryClient();
  const ticketQ = useQuery({ queryKey: ["t-ticket", id], queryFn: () => getMyTicket(id), refetchInterval: 30_000 });
  const [message, setMessage] = useState("");
  const replyRef = useRef<HTMLTextAreaElement>(null);

  function onSaved(tk: Ticket) {
    qc.setQueryData(["t-ticket", id], tk);
    qc.invalidateQueries({ queryKey: ["t-tickets"] });
  }

  const replyMut = useMutation({
    mutationFn: () => replyToMyTicket(id, message.trim()),
    onSuccess: (tk) => {
      onSaved(tk);
      setMessage("");
      toast.success(t("portal.tickets.replySent"));
    },
  });

  const confirmMut = useMutation({
    mutationFn: () => confirmMyTicket(id),
    onSuccess: (tk) => {
      onSaved(tk);
      toast.success(t("portal.tickets.confirmedToast"));
    },
  });

  if (ticketQ.isLoading || !ticketQ.data) return <PageLoader />;
  const tk = ticketQ.data;
  const closed = tk.status === "CLOSED";
  const solved = tk.status === "RESOLVED";

  return (
    <div className="space-y-4 max-w-4xl">
      <Link to="/portal/support" className="inline-flex items-center gap-1.5 text-xs text-text-muted hover:text-text">
        <ArrowLeft className="h-3.5 w-3.5 rtl:rotate-180" />
        {t("portal.tickets.backToList")}
      </Link>

      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <div className="flex items-center gap-2 mb-1">
            <span className="font-mono text-text-muted">#{tk.ticketNo}</span>
            <Badge tone={TICKET_STATUS_TONE[tk.status]}>{t(`portal.tickets.status.${tk.status}`)}</Badge>
          </div>
          <h1 dir="auto" className="text-2xl font-bold text-left rtl:text-right">
            {tk.subject}
          </h1>
          <div className="text-xs text-text-muted mt-1">
            {t(`portal.tickets.category.${tk.category}`)} · {t("portal.tickets.opened")} {formatDate(tk.createdAt)}
            {tk.reference ? ` · ${tk.reference}` : ""}
          </div>
        </div>
        {!closed && !solved && (
          <Button
            variant="secondary"
            leftIcon={<CheckCircle2 className="h-4 w-4" />}
            loading={confirmMut.isPending}
            onClick={() => confirmMut.mutate()}
          >
            {t("portal.tickets.markSolved")}
          </Button>
        )}
      </div>

      <Card>
        <CardBody className="p-5">
          <TicketThread messages={tk.messages ?? []} viewer="client" />
        </CardBody>
      </Card>

      {solved && (
        <Card className="border-accent-emerald/30">
          <CardBody className="p-5 space-y-3">
            <div className="font-semibold">{t("portal.tickets.confirm.title")}</div>
            <p className="text-xs text-text-muted">
              {tk.autoCloseAt
                ? t("portal.tickets.confirm.autoClose", { date: formatDate(tk.autoCloseAt) })
                : t("portal.tickets.confirm.hint")}
            </p>
            <div className="flex flex-wrap gap-2">
              <Button
                leftIcon={<CheckCircle2 className="h-4 w-4" />}
                loading={confirmMut.isPending}
                onClick={() => confirmMut.mutate()}
              >
                {t("portal.tickets.confirm.yes")}
              </Button>
              <Button
                variant="secondary"
                leftIcon={<MessageSquareReply className="h-4 w-4" />}
                onClick={() => replyRef.current?.focus()}
              >
                {t("portal.tickets.confirm.no")}
              </Button>
            </div>
          </CardBody>
        </Card>
      )}

      {closed ? (
        <p className="text-sm text-text-muted">
          {t(`portal.tickets.closedNote.${tk.closedBy ?? "STAFF"}`)} {t("portal.tickets.closedNewTicket")}
        </p>
      ) : (
        <Card>
          <CardBody className="p-5 space-y-3">
            <Textarea
              ref={replyRef}
              dir="auto"
              rows={4}
              maxLength={10000}
              placeholder={solved ? t("portal.tickets.stillNeedHelpPlaceholder") : t("portal.tickets.replyPlaceholder")}
              value={message}
              onChange={(e) => setMessage(e.target.value)}
            />
            <div className="flex justify-end">
              <Button
                leftIcon={<Send className="h-4 w-4 rtl:rotate-180" />}
                loading={replyMut.isPending}
                disabled={!message.trim()}
                onClick={() => replyMut.mutate()}
              >
                {t("portal.tickets.sendReply")}
              </Button>
            </div>
          </CardBody>
        </Card>
      )}
    </div>
  );
}
