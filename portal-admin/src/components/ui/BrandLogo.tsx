import { cn } from "@/lib/cn";

/**
 * MOTABIQ brand mark — the official navy diamond from
 * /public/motabiq-primary.svg. Single source of truth for the in-app logo
 * (sidebar, login, landing header all render this).
 */
export function BrandLogo({
  size = 36,
  className,
}: {
  size?: number;
  className?: string;
}) {
  return (
    <img
      src="/motabiq-primary.svg"
      alt="MOTABIQ"
      width={size}
      height={size}
      className={cn("shrink-0", className)}
    />
  );
}
