/**
 * Decorative fingerprint thumbnail shown when the user has selected the
 * bundled sample. We can't render a real WSQ file as an image, so we draw
 * a stylized fingerprint instead — just for UI feedback.
 */
export function FingerprintThumb({ className }: { className?: string }) {
  return (
    <svg
      viewBox="0 0 80 96"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={className}
      aria-hidden
    >
      <rect width="80" height="96" rx="6" fill="#F1F5F9" />
      <g stroke="#1e4e8c" strokeWidth="1.4" strokeLinecap="round" fill="none">
        {/* Outer finger outline */}
        <path d="M22 84 C18 64 18 40 28 24 C38 12 54 14 60 28 C66 42 64 68 58 84" />
        {/* Inner whorl */}
        <path d="M28 74 C26 60 28 42 36 32 C44 24 54 28 56 40 C58 52 54 64 50 74" />
        <path d="M34 64 C32 54 34 44 40 38 C46 34 52 38 52 46 C52 54 50 60 46 66" />
        <path d="M40 54 C38 48 40 44 44 42 C48 40 50 44 50 48 C50 52 48 56 44 58" />
        <ellipse cx="44" cy="50" rx="3" ry="4" />
        {/* Little ridges / breaks */}
        <path d="M22 70 L26 70" opacity="0.5" />
        <path d="M56 70 L60 70" opacity="0.5" />
        <path d="M24 56 L28 56" opacity="0.5" />
        <path d="M56 56 L60 56" opacity="0.5" />
        <path d="M26 40 L30 40" opacity="0.5" />
        <path d="M54 40 L58 40" opacity="0.5" />
      </g>
    </svg>
  );
}
