import { useState, useEffect } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Plus } from "lucide-react";
import { toast } from "sonner";
import { useTranslation } from "react-i18next";
import {
  listSubscriptionsByTenant,
  createSubscription,
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

export function SubscriptionsPage() {
  const { t } = useTranslation();
  const qc = useQueryClient();
  const tenantsQ = useQuery({ queryKey: ["tenants"], queryFn: listTenants });
  const plansQ = useQuery({ queryKey: ["plans"], queryFn: listPlans });

  const [tenantId, setTenantId] = useState<string | null>(null);
  useEffect(() => {
    if (!tenantId && tenantsQ.data?.length) {
      setTenantId(tenantsQ.data[0].id);
    }
  }, [tenantId, tenantsQ.data]);

  const subsQ = useQuery({
    queryKey: ["subscriptions", tenantId],
    queryFn: () => listSubscriptionsByTenant(tenantId!),
    enabled: !!tenantId,
  });

  const [open, setOpen] = useState(false);
  const [planId, setPlanId] = useState<string | null>(null);
  const [startDate, setStartDate] = useState<string>(
    new Date().toISOString().slice(0, 10),
  );

  const createMut = useMutation({
    mutationFn: () =>
      createSubscription({
        tenantId: tenantId!,
        planId: planId!,
        startDate,
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["subscriptions", tenantId] });
      toast.success(t("common.create"));
      setOpen(false);
    },
  });

  return (
    <div>
      <PageHeader
        title={t("subscriptions.title")}
        description={t("subscriptions.subtitle")}
        actions={
          <Button
            leftIcon={<Plus className="h-4 w-4" />}
            disabled={!tenantId || !plansQ.data?.length}
            onClick={() => {
              setPlanId(plansQ.data?.[0]?.id ?? null);
              setOpen(true);
            }}
          >
            {t("subscriptions.newSubscription")}
          </Button>
        }
      />

      <Card>
        <CardHeader>
          <CardTitle>{t("common.filter")}</CardTitle>
          <div className="w-72">
            <Select
              value={tenantId}
              onChange={(v) => setTenantId(v)}
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
        open={open}
        onClose={() => setOpen(false)}
        title={t("subscriptions.newSubscription")}
        size="lg"
        footer={
          <>
            <Button variant="ghost" onClick={() => setOpen(false)}>
              {t("common.cancel")}
            </Button>
            <Button
              loading={createMut.isPending}
              disabled={!planId || !tenantId}
              onClick={() => createMut.mutate()}
            >
              {t("common.create")}
            </Button>
          </>
        }
      >
        <div className="space-y-4">
          <div>
            <Label>{t("subscriptions.fields.tenant")}</Label>
            <div className="px-3 py-2 rounded-lg bg-bg-elevated/40 border border-border/15 text-sm">
              {tenantsQ.data?.find((tenant) => tenant.id === tenantId)
                ?.legalName ?? "—"}
            </div>
          </div>
          <div>
            <Label>{t("subscriptions.fields.plan")}</Label>
            <Select
              value={planId}
              onChange={setPlanId}
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
          <div>
            <Label htmlFor="startDate">
              {t("subscriptions.fields.startDate")}
            </Label>
            <Input
              id="startDate"
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
            />
          </div>
        </div>
      </Dialog>
    </div>
  );
}
