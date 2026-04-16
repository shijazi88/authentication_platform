import { Languages } from "lucide-react";
import {
  Menu,
  MenuButton,
  MenuItem,
  MenuItems,
  Transition,
} from "@headlessui/react";
import { Fragment } from "react";
import { useTranslation } from "react-i18next";
import { usePrefs, type Lang } from "@/lib/prefs";
import { cn } from "@/lib/cn";

const options: { value: Lang; label: string; native: string }[] = [
  { value: "en", label: "English", native: "English" },
  { value: "ar", label: "Arabic", native: "العربية" },
];

export function LangToggle({ className }: { className?: string }) {
  const { t } = useTranslation();
  const lang = usePrefs((s) => s.lang);
  const setLang = usePrefs((s) => s.setLang);
  const current = options.find((o) => o.value === lang);

  return (
    <Menu as="div" className={cn("relative", className)}>
      <MenuButton
        title={t("topbar.language.english")}
        className={cn(
          "h-9 px-3 rounded-lg flex items-center gap-2",
          "border border-border/20 bg-bg-elevated/40 text-text-muted",
          "hover:text-text hover:border-border/40 transition-colors text-xs font-medium",
        )}
      >
        <Languages className="h-4 w-4" />
        <span>{current?.native}</span>
      </MenuButton>
      <Transition
        as={Fragment}
        enter="transition ease-out duration-150"
        enterFrom="opacity-0 translate-y-1"
        enterTo="opacity-100 translate-y-0"
        leave="transition ease-in duration-100"
        leaveFrom="opacity-100"
        leaveTo="opacity-0"
      >
        <MenuItems
          anchor="bottom end"
          className="z-50 mt-1 min-w-[140px] glass rounded-lg shadow-card py-1 [--anchor-gap:6px]"
        >
          {options.map((opt) => (
            <MenuItem key={opt.value}>
              {({ focus }) => (
                <button
                  type="button"
                  onClick={() => setLang(opt.value)}
                  className={cn(
                    "w-full text-start px-3 py-2 text-xs flex items-center justify-between",
                    focus && "bg-bg-hover/60",
                    opt.value === lang && "text-accent-violet",
                  )}
                >
                  <span>{opt.native}</span>
                  {opt.value === lang && (
                    <span className="text-[10px] opacity-70">✓</span>
                  )}
                </button>
              )}
            </MenuItem>
          ))}
        </MenuItems>
      </Transition>
    </Menu>
  );
}
