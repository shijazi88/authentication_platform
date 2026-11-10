import { useTranslation } from "react-i18next";
import { Lock } from "lucide-react";
import { cn } from "@/lib/cn";
import { formatDate } from "@/lib/format";
import type { TicketMessage, TicketPriority, TicketStatus } from "@/types/api";

type Tone = "amber" | "cyan" | "violet" | "emerald" | "neutral" | "rose";

export const TICKET_STATUS_TONE: Record<TicketStatus, Tone> = {
  OPEN: "amber",
  IN_PROGRESS: "cyan",
  WAITING_ON_CLIENT: "violet",
  RESOLVED: "emerald",
  CLOSED: "neutral",
};

export const TICKET_PRIORITY_TONE: Record<TicketPriority, Tone> = {
  LOW: "neutral",
  NORMAL: "cyan",
  HIGH: "amber",
  URGENT: "rose",
};

/**
 * A ticket conversation, oldest first. {@code viewer} decides the labels: the
 * client sees "You" and "MOTABIQ Support"; staff see who wrote each message
 * and internal notes (amber, with a lock).
 */
export function TicketThread({
  messages,
  viewer,
}: {
  messages: TicketMessage[];
  viewer: "staff" | "client";
}) {
  const { t } = useTranslation();

  return (
    <div className="space-y-3">
      {messages.map((m) => {
        const mine = viewer === "client" ? m.authorType === "CLIENT" : m.authorType === "STAFF";
        const author =
          viewer === "client"
            ? m.authorType === "CLIENT"
              ? m.authorEmail ?? t("portal.tickets.you")
              : t("portal.tickets.supportTeam")
            : m.authorEmail ?? "—";
        return (
          <div key={m.id} className={cn("flex", mine ? "justify-end" : "justify-start")}>
            <div
              className={cn(
                "max-w-[85%] rounded-xl border px-4 py-3",
                m.internal
                  ? "bg-accent-amber/10 border-accent-amber/30"
                  : mine
                    ? "bg-accent-violet/10 border-accent-violet/25"
                    : "bg-bg-elevated/50 border-border/15",
              )}
            >
              <div className="flex items-center gap-2 text-[11px] text-text-muted mb-1.5">
                {m.internal && <Lock className="h-3 w-3 text-accent-amber" />}
                <span className="font-semibold text-text">{author}</span>
                {viewer === "staff" && (
                  <span className="text-text-dim">
                    {m.internal
                      ? t("tickets.thread.internal")
                      : m.authorType === "CLIENT"
                        ? t("tickets.thread.client")
                        : t("tickets.thread.staff")}
                  </span>
                )}
                <span className="text-text-dim">· {formatDate(m.createdAt)}</span>
              </div>
              <div dir="auto" className="text-sm whitespace-pre-wrap break-words leading-relaxed">
                {m.body}
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
}
