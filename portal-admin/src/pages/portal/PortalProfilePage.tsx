import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";
import { User, KeyRound } from "lucide-react";
import { getMe, updateProfile, changePassword } from "@/api/tenant";
import { Card, CardBody, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { PageLoader } from "@/components/ui/Spinner";

export function PortalProfilePage() {
  const { t } = useTranslation();
  const qc = useQueryClient();
  const meQ = useQuery({ queryKey: ["t-me"], queryFn: getMe });

  const [displayName, setDisplayName] = useState("");
  const [current, setCurrent] = useState("");
  const [next, setNext] = useState("");
  const [confirm, setConfirm] = useState("");

  // Seed the form once the profile loads.
  useEffect(() => {
    if (meQ.data) setDisplayName(meQ.data.displayName ?? "");
  }, [meQ.data]);

  const profileMut = useMutation({
    mutationFn: () => updateProfile(displayName.trim() || null),
    onSuccess: (me) => {
      qc.setQueryData(["t-me"], me);
      toast.success(t("portal.profile.saved"));
    },
    onError: (e: any) => toast.error(e?.response?.data?.message ?? t("common.error")),
  });

  const pwMut = useMutation({
    mutationFn: () => changePassword(current, next),
    onSuccess: () => {
      toast.success(t("portal.profile.passwordChanged"));
      setCurrent("");
      setNext("");
      setConfirm("");
    },
    onError: (e: any) =>
      toast.error(e?.response?.data?.message ?? t("portal.profile.passwordError")),
  });

  const pwValid = current.length > 0 && next.length >= 8 && next === confirm;

  if (meQ.isLoading) return <PageLoader />;

  return (
    <div className="space-y-4 max-w-2xl">
      <h1 className="text-2xl font-bold">{t("portal.nav.profile")}</h1>

      {/* Personal information */}
      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <User className="h-4 w-4 text-text-muted" />
            <CardTitle>{t("portal.profile.personalInfo")}</CardTitle>
          </div>
        </CardHeader>
        <CardBody className="space-y-4">
          <div>
            <Label htmlFor="displayName">{t("portal.profile.displayName")}</Label>
            <Input
              id="displayName"
              placeholder={t("portal.profile.displayNamePlaceholder")}
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
            />
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <Label>{t("portal.profile.email")}</Label>
              <Input value={meQ.data?.email ?? ""} disabled readOnly />
              <p className="mt-1 text-xs text-text-dim">{t("portal.profile.emailNote")}</p>
            </div>
            <div>
              <Label>{t("portal.profile.organization")}</Label>
              <Input value={meQ.data?.tenantName ?? ""} disabled readOnly />
            </div>
          </div>
          <div className="flex justify-end">
            <Button
              loading={profileMut.isPending}
              disabled={displayName.trim() === (meQ.data?.displayName ?? "")}
              onClick={() => profileMut.mutate()}
            >
              {t("common.save")}
            </Button>
          </div>
        </CardBody>
      </Card>

      {/* Change password */}
      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <KeyRound className="h-4 w-4 text-text-muted" />
            <CardTitle>{t("portal.profile.changePassword")}</CardTitle>
          </div>
        </CardHeader>
        <CardBody className="space-y-4">
          <div>
            <Label htmlFor="current">{t("portal.profile.currentPassword")}</Label>
            <Input
              id="current"
              type="password"
              autoComplete="current-password"
              value={current}
              onChange={(e) => setCurrent(e.target.value)}
            />
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <Label htmlFor="next">{t("portal.profile.newPassword")}</Label>
              <Input
                id="next"
                type="password"
                autoComplete="new-password"
                value={next}
                onChange={(e) => setNext(e.target.value)}
              />
              <p className="mt-1 text-xs text-text-dim">{t("portal.profile.passwordHint")}</p>
            </div>
            <div>
              <Label htmlFor="confirm">{t("portal.profile.confirmPassword")}</Label>
              <Input
                id="confirm"
                type="password"
                autoComplete="new-password"
                value={confirm}
                onChange={(e) => setConfirm(e.target.value)}
              />
              {confirm.length > 0 && next !== confirm && (
                <p className="mt-1 text-xs text-accent-rose">{t("portal.profile.passwordMismatch")}</p>
              )}
            </div>
          </div>
          <div className="flex justify-end">
            <Button
              loading={pwMut.isPending}
              disabled={!pwValid}
              onClick={() => pwMut.mutate()}
            >
              {t("portal.profile.changePassword")}
            </Button>
          </div>
        </CardBody>
      </Card>
    </div>
  );
}
