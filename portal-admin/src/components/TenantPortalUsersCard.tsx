import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";
import { Users as UsersIcon, Plus, KeyRound, Power } from "lucide-react";
import {
  listTenantUsers,
  createTenantUser,
  setTenantUserStatus,
  resetTenantUserPassword,
} from "@/api/tenantUsers";
import { useAuth } from "@/lib/auth";
import { canWrite } from "@/lib/access";
import type { TenantPortalUser } from "@/types/api";
import { Card, CardBody, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Badge } from "@/components/ui/Badge";
import { Dialog } from "@/components/ui/Dialog";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { formatDate } from "@/lib/format";

/** Admin management of a tenant's portal (client) login users. */
export function TenantPortalUsersCard({ tenantId }: { tenantId: string }) {
  const { t } = useTranslation();
  const qc = useQueryClient();
  const writable = canWrite(useAuth((s) => s.role));

  const usersQ = useQuery({
    queryKey: ["tenant-portal-users", tenantId],
    queryFn: () => listTenantUsers(tenantId),
  });

  const [createOpen, setCreateOpen] = useState(false);
  const [email, setEmail] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [password, setPassword] = useState("");
  const [pwdTarget, setPwdTarget] = useState<TenantPortalUser | null>(null);
  const [newPwd, setNewPwd] = useState("");

  const invalidate = () =>
    qc.invalidateQueries({ queryKey: ["tenant-portal-users", tenantId] });
  const onErr = (e: any) =>
    toast.error(e?.response?.data?.message ?? t("tenantUsers.error"));

  const createMut = useMutation({
    mutationFn: () =>
      createTenantUser(tenantId, { email, password, displayName: displayName || undefined }),
    onSuccess: () => {
      invalidate();
      toast.success(t("tenantUsers.created"));
      setCreateOpen(false);
      setEmail("");
      setDisplayName("");
      setPassword("");
    },
    onError: onErr,
  });

  const statusMut = useMutation({
    mutationFn: (u: TenantPortalUser) => setTenantUserStatus(tenantId, u.id, !u.active),
    onSuccess: () => {
      invalidate();
      toast.success(t("tenantUsers.statusChanged"));
    },
    onError: onErr,
  });

  const pwdMut = useMutation({
    mutationFn: () => resetTenantUserPassword(tenantId, pwdTarget!.id, newPwd),
    onSuccess: () => {
      toast.success(t("tenantUsers.passwordReset"));
      setPwdTarget(null);
      setNewPwd("");
    },
    onError: onErr,
  });

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center gap-2">
          <UsersIcon className="h-4 w-4 text-text-muted" />
          <CardTitle>{t("tenantUsers.title")}</CardTitle>
        </div>
        {writable && (
          <Button
            variant="ghost"
            size="sm"
            leftIcon={<Plus className="h-3.5 w-3.5" />}
            onClick={() => setCreateOpen(true)}
          >
            {t("tenantUsers.newUser")}
          </Button>
        )}
      </CardHeader>
      <CardBody className="p-0">
        {usersQ.data?.length ? (
          <div className="divide-y divide-border/10">
            {usersQ.data.map((u) => (
              <div key={u.id} className="flex items-center justify-between px-4 py-3 gap-2">
                <div className="min-w-0">
                  <div className="text-sm font-medium truncate">
                    {u.displayName ?? u.email}
                  </div>
                  <div className="text-xs text-text-muted truncate">
                    {u.email} ·{" "}
                    {u.lastLoginAt
                      ? `${t("tenantUsers.lastLogin")}: ${formatDate(u.lastLoginAt)}`
                      : t("tenantUsers.never")}
                  </div>
                </div>
                <div className="flex items-center gap-1 shrink-0">
                  <Badge tone={u.active ? "emerald" : "neutral"}>
                    {u.active ? t("common.active") : t("common.inactive")}
                  </Badge>
                  {writable && (
                    <>
                      <Button
                        variant="ghost"
                        size="sm"
                        leftIcon={<KeyRound className="h-3.5 w-3.5" />}
                        onClick={() => setPwdTarget(u)}
                      >
                        {t("tenantUsers.resetPassword")}
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        leftIcon={<Power className="h-3.5 w-3.5" />}
                        loading={statusMut.isPending && statusMut.variables?.id === u.id}
                        onClick={() => statusMut.mutate(u)}
                      >
                        {u.active ? t("tenantUsers.deactivate") : t("tenantUsers.activate")}
                      </Button>
                    </>
                  )}
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="px-6 py-8 text-center text-xs text-text-muted">
            {t("tenantUsers.empty")}
          </div>
        )}
      </CardBody>

      <Dialog
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        title={t("tenantUsers.newUser")}
        size="lg"
        footer={
          <>
            <Button variant="ghost" onClick={() => setCreateOpen(false)}>
              {t("common.cancel")}
            </Button>
            <Button
              loading={createMut.isPending}
              disabled={!email || password.length < 8}
              onClick={() => createMut.mutate()}
            >
              {t("common.create")}
            </Button>
          </>
        }
      >
        <div className="space-y-4">
          <div>
            <Label htmlFor="tu-email">{t("users.fields.email")}</Label>
            <Input id="tu-email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
          </div>
          <div>
            <Label htmlFor="tu-name">{t("common.name")}</Label>
            <Input id="tu-name" value={displayName} onChange={(e) => setDisplayName(e.target.value)} />
          </div>
          <div>
            <Label htmlFor="tu-pwd">{t("users.fields.password")}</Label>
            <Input id="tu-pwd" type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
            <p className="mt-1 text-xs text-text-dim">{t("users.passwordHint")}</p>
          </div>
        </div>
      </Dialog>

      <Dialog
        open={pwdTarget !== null}
        onClose={() => setPwdTarget(null)}
        title={t("tenantUsers.resetPassword")}
        footer={
          <>
            <Button variant="ghost" onClick={() => setPwdTarget(null)}>
              {t("common.cancel")}
            </Button>
            <Button loading={pwdMut.isPending} disabled={newPwd.length < 8} onClick={() => pwdMut.mutate()}>
              {t("tenantUsers.resetPassword")}
            </Button>
          </>
        }
      >
        <div>
          <Label htmlFor="tu-newpwd">{t("users.fields.newPassword")}</Label>
          <Input id="tu-newpwd" type="password" value={newPwd} onChange={(e) => setNewPwd(e.target.value)} />
          <p className="mt-1 text-xs text-text-dim">{t("users.passwordHint")}</p>
        </div>
      </Dialog>
    </Card>
  );
}
