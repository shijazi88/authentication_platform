import { useEffect } from "react";

export type Shortcut = {
  key: string;        // e.g. "Enter", "Escape", "k"
  ctrlOrMeta?: boolean;
  shift?: boolean;
  handler: (e: KeyboardEvent) => void;
};

/**
 * Simple keyboard shortcut registration. Shortcuts fire on `keydown` unless
 * the target is an editable element (input/textarea/contenteditable) — except
 * for modifier combos like ⌘K which work anywhere.
 */
export function useShortcuts(shortcuts: Shortcut[]) {
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      const target = e.target as HTMLElement | null;
      const inEditable =
        !!target &&
        (target.tagName === "INPUT" ||
          target.tagName === "TEXTAREA" ||
          target.isContentEditable);

      for (const s of shortcuts) {
        const hasMod = e.ctrlKey || e.metaKey;
        if (s.ctrlOrMeta && !hasMod) continue;
        if (!s.ctrlOrMeta && hasMod) continue;
        if ((s.shift ?? false) !== e.shiftKey) continue;
        if (e.key.toLowerCase() !== s.key.toLowerCase()) continue;

        // If the user is typing in a text field, only allow modified combos
        // (e.g. ⌘K) or explicit Enter/Escape handlers.
        if (inEditable && !s.ctrlOrMeta && !["Enter", "Escape"].includes(s.key)) {
          continue;
        }
        s.handler(e);
        return;
      }
    };
    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, [shortcuts]);
}
