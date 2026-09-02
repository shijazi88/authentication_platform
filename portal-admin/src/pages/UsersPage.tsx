import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";
import { Plus, Users as UsersIcon, Pencil, KeyRound, Power } from "lucide-react";
import {
  listUsers,
  createUser,
  updateUser,
  setUserStatus,
  resetUserPassword,
} from "@/api/users";
import { OFFERED_ROLES } from "@/lib/access";
import type { AdminRole, AdminUser } from "@/types/api";
import { PageHeader } from "@/components/ui/PageHeader";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Table, TBody, THead, Th, Td, Tr } from "@/components/ui/Table";
import { Badge } from "@/components/ui/Badge";
import { Dialog } from "@/components/ui/Dialog";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { Select } from "@/components/ui/Select";
import { EmptyState } from "@/components/ui/EmptyState";
import { PageLoader } from "@/components/ui/Spinner";
import { formatDate } from "@/lib/format";

const ROLE_TONE: Record<AdminRole, "violet" | "cyan" | "emerald" | "amber" | "rose" | "neutral"> = {
  SUPER_ADMIN: "violet",
  PLATFORM_OPS: "cyan",
  FINANCE: "emerald",
  SUPPORT: "amber",
  CENTRAL_BANK: "rose",
  AUDITOR: "neutral",
};

export function UsersPage() {
  const { t } = useTranslation();
  const qc = useQueryClient();
  const roleLabel = (r: AdminRole) => t(`roles.${r}`, r);

  const usersQ = useQuery({ queryKey: ["users"], queryFn: listUsers });

  // Dialog state
  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState<AdminUser | null>(null);
  const [pwdTarget, setPwdTarget] = useState<AdminUser | null>(null);

  // Form fields
  const [email, setEmail] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState<AdminRole>("PLATFORM_OPS");

  const roleOptions = OFFERED_ROLES.map((r) => ({ value: r, label: roleLabel(r) }));

  function invalidate() {
    qc.invalidateQueries({ queryKey: ["users"] });
  }

  const createMut = useMutation({
    mutationFn: () =>
      createUser({ email, password, displayName: displayName || undefined, role }),
    onSuccess: () => {
      invalidate();
      toast.success(t("users.created"));
      setCreateOpen(false);
    },
    onError: (e: any) =>
      toast.error(e?.response?.data?.message ?? t("users.saveError")),
  });

  const updateMut = useMutation({
    mutationFn: () =>
      updateUser(editing!.id, { displayName: displayName || undefined, role }),
    onSuccess: () => {
      invalidate();
      toast.success(t("users.updated"));
      setEditing(null);
    },
    onError: (e: any) =>
      toast.error(e?.response?.data?.message ?? t("users.saveError")),
  });

  const statusMut = useMutation({
    mutationFn: (u: AdminUser) => setUserStatus(u.id, !u.active),
    onSuccess: () => {
      invalidate();
      toast.success(t("users.statusChanged"));
    },
    onError: (e: any) =>
      toast.error(e?.response?.data?.message ?? t("users.saveError")),
  });

  const pwdMut = useMutation({
    mutationFn: () => resetUserPassword(pwdTarget!.id, password),
    onSuccess: () => {
      toast.success(t("users.passwordReset"));
      setPwdTarget(null);
    },
    onError: (e: any) =>
      toast.error(e?.response?.data?.message ?? t("users.saveError")),
  });

  function openCreate() {
    setEmail("");
    setDisplayName("");
    setPassword("");
    setRole("PLATFORM_OPS");
    setCreateOpen(true);
  }

  function openEdit(u: AdminUser) {
    setDisplayName(u.displayName ?? "");
    setRole(OFFERED_ROLES.includes(u.role) ? u.role : "PLATFORM_OPS");
    setEditing(u);
  }

  function openPwd(u: AdminUser) {
    setPassword("");
    setPwdTarget(u);
  }

  return (
    <div>
      <PageHeader
        title={t("users.title")}
        description={t("users.subtitle")}
        actions={
          <Button leftIcon={<Plus className="h-4 w-4" />} onClick={openCreate}>
            {t("users.newUser")}
          </Button>
        }
      />

      <Card>
        {usersQ.isLoading ? (
          <PageLoader />
        ) : usersQ.data?.length ? (
          <Table>
            <THead>
              <Tr>
                <Th>{t("common.name")}</Th>
                <Th>{t("users.fields.email")}</Th>
                <Th>{t("users.fields.role")}</Th>
                <Th>{t("common.status")}</Th>
                <Th>{t("users.fields.lastLogin")}</Th>
                <Th>{t("common.actions")}</Th>
              </Tr>
            </THead>
            <TBody>
              {usersQ.data.map((u) => (
                <Tr key={u.id}>
                  <Td className="font-medium">{u.displayName ?? "—"}</Td>
                  <Td className="font-mono text-xs">{u.email}</Td>
                  <Td>
                    <Badge tone={ROLE_TONE[u.role]}>{roleLabel(u.role)}</Badge>
                  </Td>
                  <Td>
                    <Badge tone={u.active ? "emerald" : "neutral"}>
                      {u.active ? t("common.active") : t("common.inactive")}
                    </Badge>
                  </Td>
                  <Td className="text-xs text-text-muted">
                    {u.lastLoginAt ? formatDate(u.lastLoginAt) : t("users.never")}
                  </Td>
                  <Td>
                    <div className="flex items-center gap-1">
                      <Button
                        variant="ghost"
                        size="sm"
                        leftIcon={<Pencil className="h-3.5 w-3.5" />}
                        onClick={() => openEdit(u)}
                      >
                        {t("common.edit")}
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        leftIcon={<KeyRound className="h-3.5 w-3.5" />}
                        onClick={() => openPwd(u)}
                      >
                        {t("users.resetPassword")}
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        leftIcon={<Power className="h-3.5 w-3.5" />}
                        loading={statusMut.isPending && statusMut.variables?.id === u.id}
                        onClick={() => statusMut.mutate(u)}
                      >
                        {u.active ? t("users.deactivate") : t("users.activate")}
                      </Button>
                    </div>
                  </Td>
                </Tr>
              ))}
            </TBody>
          </Table>
        ) : (
          <EmptyState
            icon={<UsersIcon className="h-5 w-5" />}
            title={t("users.empty.title")}
            description={t("users.empty.description")}
            action={
              <Button leftIcon={<Plus className="h-4 w-4" />} onClick={openCreate}>
                {t("users.newUser")}
              </Button>
            }
          />
        )}
      </Card>

      {/* Create */}
      <Dialog
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        title={t("users.newUser")}
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
            <Label htmlFor="email">{t("users.fields.email")}</Label>
            <Input
              id="email"
              type="email"
              placeholder="name@promatrix.ai"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </div>
          <div>
            <Label htmlFor="displayName">{t("common.name")}</Label>
            <Input
              id="displayName"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
            />
          </div>
          <div>
            <Label htmlFor="password">{t("users.fields.password")}</Label>
            <Input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
            <p className="mt-1 text-xs text-text-dim">{t("users.passwordHint")}</p>
          </div>
          <div>
            <Label>{t("users.fields.role")}</Label>
            <Select<AdminRole>
              value={role}
              onChange={setRole}
              options={roleOptions}
            />
          </div>
        </div>
      </Dialog>

      {/* Edit */}
      <Dialog
        open={editing !== null}
        onClose={() => setEditing(null)}
        title={t("users.editUser")}
        size="lg"
        footer={
          <>
            <Button variant="ghost" onClick={() => setEditing(null)}>
              {t("common.cancel")}
            </Button>
            <Button loading={updateMut.isPending} onClick={() => updateMut.mutate()}>
              {t("common.save")}
            </Button>
          </>
        }
      >
        <div className="space-y-4">
          <div>
            <Label>{t("users.fields.email")}</Label>
            <div className="font-mono text-sm text-text-muted">{editing?.email}</div>
          </div>
          <div>
            <Label htmlFor="editName">{t("common.name")}</Label>
            <Input
              id="editName"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
            />
          </div>
          <div>
            <Label>{t("users.fields.role")}</Label>
            <Select<AdminRole>
              value={role}
              onChange={setRole}
              options={roleOptions}
            />
          </div>
        </div>
      </Dialog>

      {/* Reset password */}
      <Dialog
        open={pwdTarget !== null}
        onClose={() => setPwdTarget(null)}
        title={t("users.resetPasswordFor", { email: pwdTarget?.email ?? "" })}
        footer={
          <>
            <Button variant="ghost" onClick={() => setPwdTarget(null)}>
              {t("common.cancel")}
            </Button>
            <Button
              loading={pwdMut.isPending}
              disabled={password.length < 8}
              onClick={() => pwdMut.mutate()}
            >
              {t("users.resetPassword")}
            </Button>
          </>
        }
      >
        <div>
          <Label htmlFor="newPwd">{t("users.fields.newPassword")}</Label>
          <Input
            id="newPwd"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          <p className="mt-1 text-xs text-text-dim">{t("users.passwordHint")}</p>
        </div>
      </Dialog>
    </div>
  );
}
