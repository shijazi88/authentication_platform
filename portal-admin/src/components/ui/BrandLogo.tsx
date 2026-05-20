import { cn } from "@/lib/cn";

/**
 * MOTABIQ brand mark — the "M" inside a gradient rounded square. Inline
 * SVG keeps the network cost ~0 vs the bundled PNG (which is reserved
 * for the marketing site's hero render).
 */
export function BrandLogo({
  size = 36,
  className,
}: {
  size?: number;
  className?: string;
}) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 44 44"
      xmlns="http://www.w3.org/2000/svg"
      role="img"
      aria-label="MOTABIQ"
      className={cn("shrink-0", className)}
    >
      <defs>
        <linearGradient id="motabiq-mark" x1="0" y1="0" x2="44" y2="44" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#24bda5" />
          <stop offset="100%" stopColor="#d8aa50" />
        </linearGradient>
      </defs>
      <rect width="44" height="44" rx="13" fill="url(#motabiq-mark)" />
      <text
        x="22"
        y="29"
        textAnchor="middle"
        fontFamily="Inter, system-ui, sans-serif"
        fontWeight="900"
        fontSize="22"
        fill="#06121e"
      >
        M
      </text>
    </svg>
  );
}
