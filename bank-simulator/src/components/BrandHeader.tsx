export function BrandHeader({ subtitle }: { subtitle?: string }) {
  return (
    <div>
      <div className="text-lg font-extrabold tracking-tight text-ink leading-tight">
        Sanad Bank Simulator
      </div>
      <div className="text-xs text-ink-muted mt-0.5">
        {subtitle ?? "Identity verification · Test client"}
      </div>
    </div>
  );
}
