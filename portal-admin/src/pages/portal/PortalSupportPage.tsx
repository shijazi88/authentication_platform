import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";
import { LifeBuoy, Plus, MessageSquareReply } from "lucide-react";
import { createTicket, listMyTickets } from "@/api/tenant";
import { Card, CardBody } from "@/components/ui/Card";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Dialog } from "@/components/ui/Dialog";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { Select } from "@/components/ui/Select";
import { Textarea } from "@/components/ui/Textarea";
import { EmptyState } from "@/components/ui/EmptyState";
import { PageLoader } from "@/components/ui/Spinner";
import { TICKET_STATUS_TONE } from "@/components/TicketThread";
import { formatDate } from "@/lib/format";
import {
  TICKET_CATEGORIES,
  TICKET_PRIORITIES,
  type TicketCategory,
  type TicketPriority,
} from "@/types/api";

export function PortalSupportPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const qc = useQueryClient();
  const ticketsQ = useQuery({ queryKey: ["t-tickets"], queryFn: listMyTickets, refetchInterval: 60_000 });

  const [open, setOpen] = useState(false);
  const [subject, setSubject] = useState("");
  const [category, setCategory] = useState<TicketCategory>("VERIFICATION");
  const [priority, setPriority] = useState<TicketPriority>("NORMAL");
  const [reference, setReference] = useState("");
  const [message, setMessage] = useState("");

  const createMut = useMutation({
    mutationFn: () =>
      createTicket({
        subject: subject.trim(),
        category,
        priority,
        reference: reference.trim() || undefined,
        message: message.trim(),
      }),
    onSuccess: (tk) => {
      qc.invalidateQueries({ queryKey: ["t-tickets"] });
      toast.success(t("portal.tickets.created", { no: tk.ticketNo }));
      setOpen(false);
      setSubject("");
      setReference("");
      setMessage("");
      setPriority("NORMAL");
      navigate(`/portal/support/${tk.id}`);
    },
  });

  return (
    <div className="space-y-4">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold">{t("portal.nav.support")}</h1>
          <p className="text-sm text-text-muted mt-1">{t("portal.tickets.subtitle")}</p>
        </div>
        <Button leftIcon={<Plus className="h-4 w-4" />} onClick={() => setOpen(true)}>
          {t("portal.tickets.new")}
        </Button>
      </div>

      <Card>
        {ticketsQ.isLoading ? (
          <div className="p-6">
            <PageLoader />
          </div>
        ) : ticketsQ.data?.length ? (
          <CardBody className="p-0">
            <div className="divide-y divide-border/10">
              {ticketsQ.data.map((tk) => {
                const awaitingYou = tk.status === "WAITING_ON_CLIENT";
                return (
                  <button
                    key={tk.id}
                    onClick={() => navigate(`/portal/support/${tk.id}`)}
                    className="w-full text-start flex items-center justify-between gap-3 px-4 py-3 hover:bg-bg-hover/40 transition-colors"
                  >
                    <div className="min-w-0">
                      <div className="flex items-center gap-2">
                        <span className="font-mono text-xs text-text-dim">#{tk.ticketNo}</span>
                        <span dir="auto" className="text-sm font-medium truncate">
                          {tk.subject}
                        </span>
                      </div>
                      <div className="text-xs text-text-muted mt-0.5 flex items-center gap-1.5">
                        {awaitingYou && <MessageSquareReply className="h-3.5 w-3.5 text-accent-violet" />}
                        {t(`portal.tickets.category.${tk.category}`)} · {t("portal.tickets.updated")}{" "}
                        {formatDate(tk.lastMessageAt)}
                      </div>
                    </div>
                    <Badge tone={TICKET_STATUS_TONE[tk.status]} className="whitespace-nowrap shrink-0">{t(`portal.tickets.status.${tk.status}`)}</Badge>
                  </button>
                );
              })}
            </div>
          </CardBody>
        ) : (
          <EmptyState
            icon={<LifeBuoy className="h-5 w-5" />}
            title={t("portal.tickets.empty.title")}
            description={t("portal.tickets.empty.description")}
            action={
              <Button leftIcon={<Plus className="h-4 w-4" />} onClick={() => setOpen(true)}>
                {t("portal.tickets.new")}
              </Button>
            }
          />
        )}
      </Card>

      <Dialog
        open={open}
        onClose={() => setOpen(false)}
        title={t("portal.tickets.new")}
        description={t("portal.tickets.dialogHint")}
        size="lg"
        footer={
          <>
            <Button variant="ghost" onClick={() => setOpen(false)}>
              {t("common.cancel")}
            </Button>
            <Button
              loading={createMut.isPending}
              disabled={!subject.trim() || !message.trim()}
              onClick={() => createMut.mutate()}
            >
              {t("portal.tickets.submit")}
            </Button>
          </>
        }
      >
        <div className="space-y-4">
          <div>
            <Label htmlFor="tk-subject">{t("portal.tickets.fields.subject")}</Label>
            <Input
              id="tk-subject"
              dir="auto"
              maxLength={200}
              placeholder={t("portal.tickets.fields.subjectPlaceholder")}
              value={subject}
              onChange={(e) => setSubject(e.target.value)}
            />
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <Label>{t("portal.tickets.fields.category")}</Label>
              <Select<TicketCategory>
                value={category}
                onChange={setCategory}
                options={TICKET_CATEGORIES.map((c) => ({ value: c, label: t(`portal.tickets.category.${c}`) }))}
              />
            </div>
            <div>
              <Label>{t("portal.tickets.fields.priority")}</Label>
              <Select<TicketPriority>
                value={priority}
                onChange={setPriority}
                options={TICKET_PRIORITIES.map((p) => ({
                  value: p,
                  label: t(`portal.tickets.priority.${p}`),
                }))}
              />
            </div>
          </div>
          <div>
            <Label htmlFor="tk-ref">
              {t("portal.tickets.fields.reference")}{" "}
              <span className="text-text-dim font-normal">({t("common.optional")})</span>
            </Label>
            <Input
              id="tk-ref"
              maxLength={128}
              placeholder={t("portal.tickets.fields.referencePlaceholder")}
              value={reference}
              onChange={(e) => setReference(e.target.value)}
            />
          </div>
          <div>
            <Label htmlFor="tk-msg">{t("portal.tickets.fields.message")}</Label>
            <Textarea
              id="tk-msg"
              dir="auto"
              rows={6}
              maxLength={10000}
              placeholder={t("portal.tickets.fields.messagePlaceholder")}
              value={message}
              onChange={(e) => setMessage(e.target.value)}
            />
          </div>
        </div>
      </Dialog>
    </div>
  );
}
