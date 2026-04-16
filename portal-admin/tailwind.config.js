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
        // MOI Yemen palette: deep blue (authority), gold (accent / seal), green (Arab identity).
        // Legacy aliases (`violet`, `cyan`, `emerald`) remap to the new palette so existing
        // gradient class names like `from-accent-violet to-accent-cyan` render as
        // blue→gold without per-component edits.
        accent: {
          DEFAULT: "#1e4e8c",
          // New semantic names — prefer these in new code:
          blue: "#1e4e8c",     // primary — deep authority blue
          gold: "#d4a017",     // accent — rich warm gold
          green: "#1f7a4d",    // success — forest green
          // Legacy aliases (point at MOI palette so existing class names work):
          violet: "#1e4e8c",   // → blue
          cyan: "#d4a017",     // → gold
          emerald: "#1f7a4d",  // → green
          // Unchanged signal colors:
          amber: "#f59e0b",    // warnings
          rose: "#dc2626",     // errors (slightly more authoritative red than f43f5e)
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
          "linear-gradient(135deg, #1e4e8c 0%, #d4a017 100%)",
      },
      backgroundSize: {
        "grid-32": "32px 32px",
      },
      boxShadow: {
        glow: "0 0 40px -10px rgba(30,78,140,0.5)",      // MOI blue glow
        "glow-cyan": "0 0 40px -10px rgba(212,160,23,0.5)", // MOI gold glow (legacy name)
        "glow-gold": "0 0 40px -10px rgba(212,160,23,0.5)",
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
