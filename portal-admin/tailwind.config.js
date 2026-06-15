/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  darkMode: "class",
  theme: {
    extend: {
      fontFamily: {
        sans: [
          "var(--font-sans)",
          "ui-sans-serif",
          "system-ui",
          "-apple-system",
          "Segoe UI",
          "sans-serif",
        ],
        mono: [
          "JetBrains Mono",
          "ui-monospace",
          "SFMono-Regular",
          "Menlo",
          "monospace",
        ],
      },
      // Theme-aware colors backed by CSS variables defined in index.css.
      // Both light and dark modes update the variables in :root / .dark.
      colors: {
        bg: {
          DEFAULT: "rgb(var(--bg) / <alpha-value>)",
          surface: "rgb(var(--bg-surface) / <alpha-value>)",
          elevated: "rgb(var(--bg-elevated) / <alpha-value>)",
          hover: "rgb(var(--bg-hover) / <alpha-value>)",
        },
        border: {
          DEFAULT: "rgb(var(--border) / <alpha-value>)",
          strong: "rgb(var(--border-strong) / <alpha-value>)",
        },
        // MOTABIQ palette: navy (primary), azure (accent), deep navy ground.
        // Legacy aliases (`violet`, `cyan`, `emerald`) remap to the new palette so
        // existing gradient class names like `from-accent-violet to-accent-cyan`
        // render as navy→azure without per-component edits.
        accent: {
          DEFAULT: "#003b73",
          // New semantic names — prefer these in new code:
          navy: "#003b73",     // primary — MOTABIQ navy (brand mark color)
          azure: "#2f7fc9",    // accent — brighter blue for gradients / CTAs
          sky: "#7fb3e6",      // lightest blue — for eyebrows / highlights
          deep: "#06192e",     // deepest ground (dark mode)
          green: "#1f7a4d",    // success — kept for semantic statuses
          // Legacy aliases (point at MOTABIQ palette so existing class names work):
          teal: "#003b73",     // → navy
          gold: "#2f7fc9",     // → azure
          mint: "#7fb3e6",     // → sky
          violet: "#003b73",   // → navy
          cyan: "#2f7fc9",     // → azure
          blue: "#003b73",     // → navy
          emerald: "#1f7a4d",  // → green
          // Unchanged signal colors:
          amber: "#f59e0b",    // warnings
          rose: "#dc2626",     // errors
        },
        text: {
          DEFAULT: "rgb(var(--text) / <alpha-value>)",
          muted: "rgb(var(--text-muted) / <alpha-value>)",
          dim: "rgb(var(--text-dim) / <alpha-value>)",
        },
      },
      backgroundImage: {
        "gradient-mesh": "var(--gradient-mesh)",
        "grid-pattern": "var(--grid-pattern)",
        "accent-gradient":
          "linear-gradient(135deg, #003b73 0%, #2f7fc9 100%)",
      },
      backgroundSize: {
        "grid-32": "32px 32px",
      },
      boxShadow: {
        glow: "0 0 40px -10px rgba(0,59,115,0.5)",          // MOTABIQ navy glow
        "glow-cyan": "0 0 40px -10px rgba(47,127,201,0.5)", // MOTABIQ azure glow (legacy name)
        "glow-gold": "0 0 40px -10px rgba(47,127,201,0.5)",
        "glow-azure": "0 0 40px -10px rgba(47,127,201,0.5)",
        "glow-green": "0 0 40px -10px rgba(31,122,77,0.5)",
        card: "var(--shadow-card)",
      },
      animation: {
        "fade-in": "fadeIn 0.4s ease-out",
        "slide-up": "slideUp 0.5s cubic-bezier(0.16, 1, 0.3, 1)",
        shimmer: "shimmer 2s linear infinite",
        "pulse-soft": "pulseSoft 2.5s ease-in-out infinite",
      },
      keyframes: {
        fadeIn: {
          "0%": { opacity: "0" },
          "100%": { opacity: "1" },
        },
        slideUp: {
          "0%": { opacity: "0", transform: "translateY(8px)" },
          "100%": { opacity: "1", transform: "translateY(0)" },
        },
        shimmer: {
          "0%": { backgroundPosition: "-200% 0" },
          "100%": { backgroundPosition: "200% 0" },
        },
        pulseSoft: {
          "0%, 100%": { opacity: "1" },
          "50%": { opacity: "0.6" },
        },
      },
    },
  },
  plugins: [],
};
