import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Sparkles, ShieldCheck, Zap } from "lucide-react";
import { login } from "@/api/auth";
import { useAuth } from "@/lib/auth";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { ThemeToggle } from "@/components/ui/ThemeToggle";
import { LangToggle } from "@/components/ui/LangToggle";

const schema = z.object({
  email: z.string().email(),
  password: z.string().min(4),
});
type FormValues = z.infer<typeof schema>;

export function LoginPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const setSession = useAuth((s) => s.setSession);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      email: "admin@middleware.local",
      password: "admin123",
    },
  });

  const mutation = useMutation({
    mutationFn: login,
    onSuccess: (data, vars) => {
      setSession({
        token: data.accessToken,
        email: vars.email,
        role: data.role,
        expiresInSeconds: data.expiresInSeconds,
      });
      navigate("/");
    },
  });

  return (
    <div className="min-h-screen flex bg-bg text-text relative overflow-hidden">
      {/* Background mesh */}
      <div className="pointer-events-none fixed inset-0 bg-gradient-mesh opacity-90" />
      <div className="pointer-events-none fixed inset-0 bg-grid-pattern bg-grid-32 opacity-[0.5] [mask-image:radial-gradient(ellipse_at_center,black_30%,transparent_70%)]" />
      <div className="pointer-events-none fixed -top-40 -end-40 h-[500px] w-[500px] rounded-full bg-accent-violet/20 blur-3xl" />
      <div className="pointer-events-none fixed -bottom-40 -start-40 h-[500px] w-[500px] rounded-full bg-accent-cyan/20 blur-3xl" />

      {/* Top-end controls (theme + language) */}
      <div className="absolute top-5 end-5 z-20 flex items-center gap-2">
        <LangToggle />
        <ThemeToggle />
      </div>

      <div className="relative flex-1 flex items-center justify-center p-6">
        <div className="grid lg:grid-cols-2 gap-12 max-w-5xl w-full items-center">
          {/* Brand panel */}
          <div className="hidden lg:block animate-slide-up">
            <div className="flex items-center gap-3 mb-8">
              <div className="h-12 w-12 rounded-xl bg-gradient-to-br from-accent-violet to-accent-cyan flex items-center justify-center shadow-glow">
                <Sparkles className="h-6 w-6 text-white" />
              </div>
              <div>
                <div className="text-xl font-bold text-gradient">
                  {t("brand.name")}
                </div>
                <div className="text-xs text-text-muted">{t("brand.tagline")}</div>
              </div>
            </div>
            <h1 className="text-5xl font-extrabold tracking-tight leading-[1.05] text-gradient">
              {t("auth.headline")}
            </h1>
            <p className="mt-5 text-text-muted text-base max-w-md leading-relaxed">
              {t("auth.subheadline")}
            </p>

            <div className="mt-10 grid gap-3 max-w-md">
              <FeatureRow
                icon={<Zap className="h-4 w-4" />}
                text={t("auth.feature1")}
              />
              <FeatureRow
                icon={<Sparkles className="h-4 w-4" />}
                text={t("auth.feature2")}
              />
              <FeatureRow
                icon={<ShieldCheck className="h-4 w-4" />}
                text={t("auth.feature3")}
              />
            </div>
          </div>

          {/* Login card */}
          <div className="animate-slide-up" style={{ animationDelay: "120ms" }}>
            <div className="glass rounded-2xl p-8 shadow-card border-gradient">
              <div className="lg:hidden flex items-center gap-2 mb-6">
                <div className="h-9 w-9 rounded-xl bg-gradient-to-br from-accent-violet to-accent-cyan flex items-center justify-center shadow-glow">
                  <Sparkles className="h-5 w-5 text-white" />
                </div>
                <div className="text-base font-bold text-gradient">
                  {t("brand.name")}
                </div>
              </div>

              <h2 className="text-2xl font-bold text-text">
                {t("auth.signIn")}
              </h2>
              <p className="text-sm text-text-muted mt-1">
                {t("auth.useCredentials")}
              </p>

              <form
                onSubmit={handleSubmit((data) => mutation.mutate(data))}
                className="mt-6 space-y-4"
              >
                <div>
                  <Label htmlFor="email">{t("auth.email")}</Label>
                  <Input
                    id="email"
                    type="email"
                    autoComplete="username"
                    placeholder="admin@middleware.local"
                    {...register("email")}
                  />
                  {errors.email && (
                    <p className="mt-1.5 text-xs text-accent-rose">
                      {t("auth.errors.emailRequired")}
                    </p>
                  )}
                </div>

                <div>
                  <Label htmlFor="password">{t("auth.password")}</Label>
                  <Input
                    id="password"
                    type="password"
                    autoComplete="current-password"
                    placeholder="••••••••"
                    {...register("password")}
                  />
                  {errors.password && (
                    <p className="mt-1.5 text-xs text-accent-rose">
                      {t("auth.errors.passwordRequired")}
                    </p>
                  )}
                </div>

                {mutation.isError && (
                  <div className="px-3 py-2 rounded-lg bg-accent-rose/10 border border-accent-rose/20 text-xs text-accent-rose">
                    {t("auth.invalidCredentials")}
                  </div>
                )}

                <Button
                  type="submit"
                  className="w-full"
                  size="lg"
                  loading={mutation.isPending}
                >
                  {t("auth.signIn")}
                </Button>
              </form>

              <div className="mt-6 pt-5 border-t border-border/15">
                <div className="text-[10px] uppercase tracking-wider text-text-dim mb-2">
                  {t("auth.devDefaults")}
                </div>
                <div className="text-xs text-text-muted font-mono">
                  admin@middleware.local · admin123
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function FeatureRow({ icon, text }: { icon: React.ReactNode; text: string }) {
  return (
    <div className="flex items-center gap-3 text-sm text-text-muted">
      <div className="h-7 w-7 rounded-md glass flex items-center justify-center text-accent-cyan">
        {icon}
      </div>
      {text}
    </div>
  );
}
