import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Plus, Building2 } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { useTranslation } from "react-i18next";
import { listTenants, createTenant, setTenantStatus } from "@/api/tenants";
import { PageHeader } from "@/components/ui/PageHeader";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Table, TBody, THead, Th, Td, Tr } from "@/components/ui/Table";
import { Badge, statusTone } from "@/components/ui/Badge";
import { Dialog } from "@/components/ui/Dialog";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { EmptyState } from "@/components/ui/EmptyState";
import { PageLoader } from "@/components/ui/Spinner";
import { formatDate, shortId } from "@/lib/format";

const schema = z.object({
  code: z.string().min(2).max(64),
  legalName: z.string().min(2).max(255),
  contactEmail: z.string().email().optional().or(z.literal("")),
});
type FormValues = z.infer<typeof schema>;

export function TenantsPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const qc = useQueryClient();
  const [open, setOpen] = useState(false);

  const tenantsQ = useQuery({ queryKey: ["tenants"], queryFn: listTenants });

  const createMut = useMutation({
    mutationFn: createTenant,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["tenants"] });
      toast.success(t("common.create"));
      setOpen(false);
      reset();
    },
  });

  const activateMut = useMutation({
    mutationFn: (id: string) => setTenantStatus(id, "ACTIVE"),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["tenants"] });
      toast.success(t("tenants.activate"));
    },
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  return (
    <div>
      <PageHeader
        title={t("tenants.title")}
        description={t("tenants.subtitle")}
        actions={
          <Button
            leftIcon={<Plus className="h-4 w-4" />}
            onClick={() => setOpen(true)}
          >
            {t("tenants.newTenant")}
          </Button>
        }
      />

      <Card>
        {tenantsQ.isLoading ? (
          <PageLoader />
        ) : tenantsQ.data?.length ? (
          <Table>
            <THead>
              <Tr>
                <Th>{t("tenants.fields.code")}</Th>
                <Th>{t("tenants.fields.legalName")}</Th>
                <Th>{t("tenants.fields.contact")}</Th>
                <Th>{t("common.status")}</Th>
                <Th>{t("common.createdAt")}</Th>
                <Th>{t("common.id")}</Th>
                <Th />
              </Tr>
            </THead>
            <TBody>
              {tenantsQ.data.map((tenant) => (
                <Tr
                  key={tenant.id}
                  className="cursor-pointer"
                  onClick={() => navigate(`/tenants/${tenant.id}`)}
                >
                  <Td className="font-mono text-text">{tenant.code}</Td>
                  <Td>{tenant.legalName}</Td>
                  <Td className="text-text-muted">
                    {tenant.contactEmail ?? "—"}
                  </Td>
                  <Td>
                    <Badge tone={statusTone(tenant.status)}>
                      {t(`status.${tenant.status}`, tenant.status)}
                    </Badge>
                  </Td>
                  <Td className="text-text-muted">
                    {formatDate(tenant.createdAt)}
                  </Td>
                  <Td className="text-text-dim font-mono text-xs">
                    {shortId(tenant.id)}
                  </Td>
                  <Td onClick={(e) => e.stopPropagation()}>
                    {tenant.status !== "ACTIVE" && (
                      <Button
                        size="sm"
                        variant="secondary"
                        loading={activateMut.isPending}
                        onClick={() => activateMut.mutate(tenant.id)}
                      >
                        {t("tenants.activate")}
                      </Button>
                    )}
                  </Td>
                </Tr>
              ))}
            </TBody>
          </Table>
        ) : (
          <EmptyState
            icon={<Building2 className="h-5 w-5" />}
            title={t("tenants.empty.title")}
            description={t("tenants.empty.description")}
            action={
              <Button
                leftIcon={<Plus className="h-4 w-4" />}
                onClick={() => setOpen(true)}
              >
                {t("tenants.empty.cta")}
              </Button>
            }
          />
        )}
      </Card>

      <Dialog
        open={open}
        onClose={() => setOpen(false)}
        title={t("tenants.newTenant")}
        description={t("tenants.newTenantDescription")}
        footer={
          <>
            <Button variant="ghost" onClick={() => setOpen(false)}>
              {t("common.cancel")}
            </Button>
            <Button
              type="submit"
              form="create-tenant-form"
              loading={createMut.isPending}
            >
              {t("common.create")}
            </Button>
          </>
        }
      >
        <form
          id="create-tenant-form"
          onSubmit={handleSubmit((data) =>
            createMut.mutate({
              code: data.code,
              legalName: data.legalName,
              contactEmail: data.contactEmail || undefined,
            }),
          )}
          className="space-y-4"
        >
          <div>
            <Label htmlFor="code">{t("tenants.fields.code")}</Label>
            <Input
              id="code"
              placeholder="BANK_DEMO"
              autoComplete="off"
              {...register("code")}
            />
            {errors.code && (
              <p className="mt-1 text-xs text-accent-rose">
                {errors.code.message}
              </p>
            )}
          </div>
          <div>
            <Label htmlFor="legalName">{t("tenants.fields.legalName")}</Label>
            <Input
              id="legalName"
              placeholder="Demo Bank Ltd"
              {...register("legalName")}
            />
            {errors.legalName && (
              <p className="mt-1 text-xs text-accent-rose">
                {errors.legalName.message}
              </p>
            )}
          </div>
          <div>
            <Label htmlFor="contactEmail">
              {t("tenants.fields.contactEmail")}
            </Label>
            <Input
              id="contactEmail"
              type="email"
              placeholder="ops@demo.bank"
              {...register("contactEmail")}
            />
            {errors.contactEmail && (
              <p className="mt-1 text-xs text-accent-rose">
                {errors.contactEmail.message}
              </p>
            )}
          </div>
        </form>
      </Dialog>
    </div>
  );
}
