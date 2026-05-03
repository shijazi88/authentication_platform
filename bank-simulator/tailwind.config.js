/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  darkMode: "class",
  theme: {
    extend: {
      fontFamily: {
        sans: ["Inter", "system-ui", "sans-serif"],
        display: ["Inter", "system-ui", "sans-serif"],
        mono: ["JetBrains Mono", "ui-monospace", "monospace"],
      },
      colors: {
        // MOI Yemen palette
        moi: {
          blue: "#1e4e8c",
          "blue-dark": "#163c6b",
          "blue-light": "#2d6ab8",
          gold: "#d4a017",
          "gold-light": "#e4b432",
          green: "#1f7a4d",
          red: "#dc2626",
        },
        ink: {
          DEFAULT: "rgb(var(--c-ink) / <alpha-value>)",
          muted: "rgb(var(--c-ink-muted) / <alpha-value>)",
          dim: "rgb(var(--c-ink-dim) / <alpha-value>)",
          faint: "rgb(var(--c-ink-faint) / <alpha-value>)",
        },
        paper: {
          DEFAULT: "rgb(var(--c-paper) / <alpha-value>)",
          soft: "rgb(var(--c-paper-soft) / <alpha-value>)",
          raised: "rgb(var(--c-paper-raised) / <alpha-value>)",
        },
      },
      boxShadow: {
        soft: "0 1px 2px rgba(15,23,42,0.04), 0 4px 16px rgba(15,23,42,0.06)",
        elevated: "0 2px 4px rgba(15,23,42,0.06), 0 12px 32px rgba(15,23,42,0.08)",
        "glow-blue": "0 0 40px -8px rgba(30,78,140,0.45)",
      },
      animation: {
        "fade-in": "fadeIn 0.35s ease-out",
        "slide-up": "slideUp 0.5s cubic-bezier(0.16, 1, 0.3, 1)",
        scanline: "scanline 1.8s ease-in-out infinite",
        "ping-slow": "pingSlow 2.2s cubic-bezier(0, 0, 0.2, 1) infinite",
        "ping-slower": "pingSlow 3s cubic-bezier(0, 0, 0.2, 1) infinite",
        "pulse-soft": "pulseSoft 1.6s ease-in-out infinite",
        "bounce-dot": "bounceDot 1s ease-in-out infinite",
      },
      keyframes: {
        fadeIn: { "0%": { opacity: "0" }, "100%": { opacity: "1" } },
        slideUp: {
          "0%": { opacity: "0", transform: "translateY(10px)" },
          "100%": { opacity: "1", transform: "translateY(0)" },
        },
        scanline: {
          "0%": { top: "10%", opacity: "0" },
          "15%": { opacity: "1" },
          "85%": { opacity: "1" },
          "100%": { top: "90%", opacity: "0" },
        },
        pingSlow: {
          "0%": { transform: "scale(1)", opacity: "0.6" },
          "75%, 100%": { transform: "scale(1.6)", opacity: "0" },
        },
        pulseSoft: {
          "0%, 100%": { opacity: "1" },
          "50%": { opacity: "0.55" },
        },
        bounceDot: {
          "0%, 100%": { transform: "translateY(0)", opacity: "0.5" },
          "50%": { transform: "translateY(-3px)", opacity: "1" },
        },
      },
    },
  },
  plugins: [],
};
