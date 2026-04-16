import { format, formatDistanceToNow, parseISO } from "date-fns";

/**
 * Active locale, derived from the <html lang="…"> attribute that the prefs
 * store keeps in sync. Centralized so every formatter follows the same source
 * of truth — switching the language toggle re-renders the page and the new
 * locale flows through automatically.
 */
function activeLocale(): string {
  if (typeof document === "undefined") return "en-US";
  const lang = document.documentElement.lang || "en";
  // Use ar-YE specifically — gives the correct Yemeni Rial symbol (ر.ي.‏)
  // and Eastern Arabic numerals where appropriate.
  return lang === "ar" ? "ar-YE" : "en-US";
}

/**
 * Format a minor-unit currency amount to a localized currency string.
 *
 * <p>Defaults to <b>YER</b> (Yemeni Rial). Yemeni Rials don't really circulate
 * fractional fils in practice, so YER amounts are rendered with no fraction
 * digits ({@code 5000 minor → "50 ر.ي.‏"} or {@code "YER 50"}).
 *
 * <p>For any other currency we keep the standard 2-fraction-digit display
 * ({@code 30 USD minor → "$0.30"}) so historical records still render correctly.
 */
export function formatMoneyMinor(amountMinor: number, currency = "YER") {
  const fractionDigits = currency === "YER" ? 0 : 2;
  return new Intl.NumberFormat(activeLocale(), {
    style: "currency",
    currency,
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits,
  }).format(amountMinor / 100);
}

export function formatNumber(n: number) {
  return new Intl.NumberFormat(activeLocale()).format(n);
}

export function formatDate(iso: string | Date | null | undefined) {
  if (!iso) return "—";
  const d = typeof iso === "string" ? parseISO(iso) : iso;
  return format(d, "yyyy-MM-dd HH:mm");
}

export function formatRelative(iso: string | Date | null | undefined) {
  if (!iso) return "—";
  const d = typeof iso === "string" ? parseISO(iso) : iso;
  return formatDistanceToNow(d, { addSuffix: true });
}

export function shortId(id: string | null | undefined, len = 8) {
  if (!id) return "—";
  return id.length > len ? id.slice(0, len) : id;
}

export function currentPeriod(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`;
}
