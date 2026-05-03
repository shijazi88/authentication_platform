import { useEffect, useState } from "react";
import { Fingerprint } from "lucide-react";
import { useTranslation } from "react-i18next";

type Props = {
  open: boolean;
};

export function VerifyingOverlay({ open }: Props) {
  const { t } = useTranslation();
  const MESSAGES = [
    t("verify.overlayStep1"),
    t("verify.overlayStep2"),
    t("verify.overlayStep3"),
  ];
  const [messageIndex, setMessageIndex] = useState(0);

  useEffect(() => {
    if (!open) {
      setMessageIndex(0);
      return;
    }
    const tick = setInterval(() => {
      setMessageIndex((i) => (i + 1) % MESSAGES.length);
    }, 1400);
    return () => clearInterval(tick);
  }, [open, MESSAGES.length]);

  if (!open) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center animate-fade-in"
      aria-modal
      role="dialog"
      aria-label="Verifying identity"
    >
      {/* Backdrop */}
      <div className="absolute inset-0 bg-ink/60 backdrop-blur-sm" />

      {/* Card */}
      <div className="relative card p-8 w-[min(380px,90vw)] animate-slide-up flex flex-col items-center text-center">
        {/* Scanner */}
        <div className="relative h-32 w-32 rounded-full bg-gradient-to-br from-moi-blue/10 to-moi-gold/10 border border-moi-blue/20 flex items-center justify-center overflow-hidden">
          <span className="absolute inset-0 rounded-full ring-2 ring-moi-blue/30 animate-ping-slow" />
          <span className="absolute inset-4 rounded-full ring-2 ring-moi-blue/20 animate-ping-slower" />
          <Fingerprint className="relative z-10 h-14 w-14 text-moi-blue drop-shadow-sm" />
          <div className="absolute left-0 right-0 h-[2px] bg-gradient-to-r from-transparent via-moi-gold to-transparent animate-scanline shadow-[0_0_12px_rgba(212,160,23,0.8)]" />
        </div>

        <h2 className="mt-6 text-lg font-extrabold text-ink">{t("verify.overlayTitle")}</h2>

        <div className="mt-3 h-5 flex items-center justify-center gap-1.5">
          <span
            key={messageIndex}
            className="text-sm text-ink-muted animate-fade-in"
          >
            {MESSAGES[messageIndex]}
          </span>
          <span className="flex gap-0.5 ml-1">
            <span className="h-1 w-1 rounded-full bg-moi-blue animate-bounce-dot" />
            <span className="h-1 w-1 rounded-full bg-moi-blue animate-bounce-dot [animation-delay:150ms]" />
            <span className="h-1 w-1 rounded-full bg-moi-blue animate-bounce-dot [animation-delay:300ms]" />
          </span>
        </div>

        <p className="mt-4 text-xs text-ink-dim">{t("verify.overlayHint")}</p>
      </div>
    </div>
  );
}
