import { useState, useEffect } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Pencil, Plus } from "lucide-react";
import { toast } from "sonner";
import { useTranslation } from "react-i18next";
import {
  listSubscriptionsByTenant,
  createSubscription,
  updateSubscription,
} from "@/api/subscriptions";
import { listTenants } from "@/api/tenants";
import { listPlans } from "@/api/plans";
import { PageHeader } from "@/components/ui/PageHeader";
import { Button } from "@/components/ui/Button";
import {
  Card,
  CardBody,
  CardHeader,
  CardTitle,
} from "@/components/ui/Card";
import { Table, TBody, THead, Th, Td, Tr } from "@/components/ui/Table";
import { Badge, statusTone } from "@/components/ui/Badge";
import { Dialog } from "@/components/ui/Dialog";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { Select } from "@/components/ui/Select";
import { PageLoader } from "@/components/ui/Spinner";
import { formatDate } from "@/lib/format";
import { useAuth } from "@/lib/auth";
import { canWrite } from "@/lib/access";
import type { Subscription } from "@/types/api";

type DialogState =
  | { mode: "create" }
  | { mode: "edit"; subscription: Subscription };

const today = () => new Date().toISOString().slice(0, 10);

export function SubscriptionsPage() {
  const { t } = useTranslation();
  const qc = useQueryClient();
  const role = useAuth((s) => s.role);
  const writable = canWrite(role);
  const tenantsQ = useQuery({ queryKey: ["tenants"], queryFn: listTenants });
  const plansQ = useQuery({ queryKey: ["plans"], queryFn: listPlans });

  // Filter on the list — which tenant's subscriptions to show
  const [filterTenantId, setFilterTenantId] = useState<string | null>(null);
  useEffect(() => {
    if (!filterTenantId && tenantsQ.data?.length) {
      setFilterTenantId(tenantsQ.data[0].id);
    }
  }, [filterTenantId, tenantsQ.data]);

  const subsQ = useQuery({
    queryKey: ["subscriptions", filterTenantId],
    queryFn: () => listSubscriptionsByTenant(filterTenantId!),
    enabled: !!filterTenantId,
  });

  // Dialog state — null = closed
  const [dialog, setDialog] = useState<DialogState | null>(null);
  const [formTenantId, setFormTenantId] = useState<string | null>(null);
  const [formPlanId, setFormPlanId] = useState<string | null>(null);
  const [formStartDate, setFormStartDate] = useState<string>(today());
  const [formEndDate, setFormEndDate] = useState<string>("");

  const isEdit = dialog?.mode === "edit";

  function openCreate() {
    setFormTenantId(filterTenantId);
    setFormPlanId(plansQ.data?.[0]?.id ?? null);
    setFormStartDate(today());
    setFormEndDate("");
    setDialog({ mode: "create" });
  }

  function openEdit(s: Subscription) {
    setFormTenantId(s.tenantId);
    setFormPlanId(s.planId);
    setFormStartDate(s.startDate);
    setFormEndDate(s.endDate ?? "");
    setDialog({ mode: "edit", subscription: s });
  }

  function close() {
    setDialog(null);
  }

  const createMut = useMutation({
    mutationFn: () =>
      createSubscription({
        tenantId: formTenantId!,
        planId: formPlanId!,
        startDate: formStartDate,
        endDate: formEndDate || undefined,
      }),
    onSuccess: (created) => {
      qc.invalidateQueries({ queryKey: ["subscriptions"] });
      // If we created a sub for a different tenant than the filter, switch the filter
      if (created.tenantId !== filterTenantId) {
        setFilterTenantId(created.tenantId);
      }
      toast.success(t("subscriptions.toasts.created"));
      close();
    },
  });

  const updateMut = useMutation({
    mutationFn: () => {
      if (dialog?.mode !== "edit") return Promise.reject();
      return updateSubscription(dialog.subscription.id, {
        planId: formPlanId!,
        startDate: formStartDate,
        endDate: formEndDate || null,
      });
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["subscriptions"] });
      toast.success(t("subscriptions.toasts.updated"));
      close();
    },
  });

  const submitting = createMut.isPending || updateMut.isPending;
  const canSubmit = !!formTenantId && !!formPlanId && !!formStartDate;

  return (
    <div>
      <PageHeader
        title={t("subscriptions.title")}
        description={t("subscriptions.subtitle")}
        actions={
          writable ? (
            <Button
              leftIcon={<Plus className="h-4 w-4" />}
              disabled={!plansQ.data?.length || !tenantsQ.data?.length}
              onClick={openCreate}
            >
              {t("subscriptions.newSubscription")}
            </Button>
          ) : undefined
        }
      />

      <Card>
        <CardHeader>
          <CardTitle>{t("common.filter")}</CardTitle>
          <div className="w-72">
            <Select
              value={filterTenantId}
              onChange={(v) => setFilterTenantId(v)}
              placeholder={t("common.selectTenant")}
              options={
                tenantsQ.data?.map((tenant) => ({
                  value: tenant.id,
                  label: tenant.legalName,
                  description: tenant.code,
                })) ?? []
              }
            />
          </div>
        </CardHeader>
        <CardBody className="p-0">
          {subsQ.isLoading ? (
            <PageLoader />
          ) : subsQ.data?.length ? (
            <Table>
              <THead>
                <Tr>
                  <Th>{t("subscriptions.fields.plan")}</Th>
                  <Th>{t("subscriptions.fields.startDate")}</Th>
                  <Th>{t("subscriptions.fields.endDate")}</Th>
                  <Th>{t("common.status")}</Th>
                  <Th>{t("common.createdAt")}</Th>
                  <Th className="text-end">{t("common.actions")}</Th>
                </Tr>
              </THead>
              <TBody>
                {subsQ.data.map((s) => {
                  const plan = plansQ.data?.find((p) => p.id === s.planId);
                  return (
                    <Tr key={s.id}>
                      <Td>
                        <div className="font-medium">{plan?.name ?? "—"}</div>
                        <div className="text-xs text-text-muted font-mono">
                          {plan?.code ?? s.planId.slice(0, 8)}
                        </div>
                      </Td>
                      <Td>{s.startDate}</Td>
                      <Td>{s.endDate ?? "—"}</Td>
                      <Td>
                        <Badge tone={statusTone(s.status)}>
                          {t(`status.${s.status}`, s.status)}
                        </Badge>
                      </Td>
                      <Td className="text-text-muted">
                        {formatDate(s.createdAt)}
                      </Td>
                      <Td className="text-end">
                        <Button
                          variant="ghost"
                          size="sm"
                          leftIcon={<Pencil className="h-3.5 w-3.5" />}
                          onClick={() => openEdit(s)}
                        >
                          {t("common.edit")}
                        </Button>
                      </Td>
                    </Tr>
                  );
                })}
              </TBody>
            </Table>
          ) : (
            <div className="text-xs text-text-muted py-12 text-center">
              {t("subscriptions.empty")}
            </div>
          )}
        </CardBody>
      </Card>

      <Dialog
        open={dialog !== null}
        onClose={close}
        title={
          isEdit
            ? t("subscriptions.editSubscription")
            : t("subscriptions.newSubscription")
        }
        size="lg"
        footer={
          <>
            <Button variant="ghost" onClick={close}>
              {t("common.cancel")}
            </Button>
            <Button
              loading={submitting}
              disabled={!canSubmit}
              onClick={() =>
                isEdit ? updateMut.mutate() : createMut.mutate()
              }
            >
              {isEdit ? t("common.save") : t("common.create")}
            </Button>
          </>
        }
      >
        <div className="space-y-4">
          <div>
            <Label>{t("subscriptions.fields.tenant")}</Label>
            <Select
              value={formTenantId}
              onChange={setFormTenantId}
              placeholder={t("common.selectTenant")}
              disabled={isEdit}
              options={
                tenantsQ.data?.map((tenant) => ({
                  value: tenant.id,
                  label: tenant.legalName,
                  description: tenant.code,
                })) ?? []
              }
            />
            {isEdit && (
              <div className="mt-1 text-[11px] text-text-muted">
                {t("subscriptions.tenantImmutable")}
              </div>
            )}
          </div>
          <div>
            <Label>{t("subscriptions.fields.plan")}</Label>
            <Select
              value={formPlanId}
              onChange={setFormPlanId}
              placeholder={t("common.selectPlan")}
              options={
                plansQ.data?.map((p) => ({
                  value: p.id,
                  label: p.name,
                  description: p.code,
                })) ?? []
              }
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <Label htmlFor="startDate">
                {t("subscriptions.fields.startDate")}
              </Label>
              <Input
                id="startDate"
                type="date"
                value={formStartDate}
                onChange={(e) => setFormStartDate(e.target.value)}
              />
            </div>
            <div>
              <Label htmlFor="endDate">
                {t("subscriptions.fields.endDate")}{" "}
                <span className="text-text-dim">({t("common.optional")})</span>
              </Label>
              <Input
                id="endDate"
                type="date"
                value={formEndDate}
                min={formStartDate || undefined}
                onChange={(e) => setFormEndDate(e.target.value)}
              />
            </div>
          </div>
        </div>
      </Dialog>
    </div>
  );
}
