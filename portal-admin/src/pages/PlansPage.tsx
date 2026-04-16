import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { useTranslation } from "react-i18next";
import { Plus, Layers } from "lucide-react";
import { listPlans, createPlan } from "@/api/plans";
import { PageHeader } from "@/components/ui/PageHeader";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Table, TBody, THead, Th, Td, Tr } from "@/components/ui/Table";
import { Badge } from "@/components/ui/Badge";
import { Dialog } from "@/components/ui/Dialog";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { EmptyState } from "@/components/ui/EmptyState";
import { PageLoader } from "@/components/ui/Spinner";
import { formatMoneyMinor, shortId } from "@/lib/format";

const schema = z.object({
  code: z.string().min(2).max(64),
  name: z.string().min(2).max(255),
  description: z.string().optional(),
  baseFeeMinor: z.coerce.number().int().min(0),
  currency: z.string().length(3).toUpperCase(),
});
type FormValues = z.infer<typeof schema>;

export function PlansPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const qc = useQueryClient();
  const [open, setOpen] = useState(false);

  const plansQ = useQuery({ queryKey: ["plans"], queryFn: listPlans });

  const createMut = useMutation({
    mutationFn: createPlan,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["plans"] });
      toast.success(t("common.create"));
      setOpen(false);
      reset();
    },
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { currency: "YER", baseFeeMinor: 0 },
  });

  return (
    <div>
      <PageHeader
        title={t("plans.title")}
        description={t("plans.subtitle")}
        actions={
          <Button
            leftIcon={<Plus className="h-4 w-4" />}
            onClick={() => setOpen(true)}
          >
            {t("plans.newPlan")}
          </Button>
        }
      />

      <Card>
        {plansQ.isLoading ? (
          <PageLoader />
        ) : plansQ.data?.length ? (
          <Table>
            <THead>
              <Tr>
                <Th>{t("common.code")}</Th>
                <Th>{t("common.name")}</Th>
                <Th>{t("plans.fields.baseFee")}</Th>
                <Th>{t("common.currency")}</Th>
                <Th>{t("common.status")}</Th>
                <Th>{t("common.id")}</Th>
              </Tr>
            </THead>
            <TBody>
              {plansQ.data.map((p) => (
                <Tr
                  key={p.id}
                  className="cursor-pointer"
                  onClick={() => navigate(`/plans/${p.id}`)}
                >
                  <Td className="font-mono">{p.code}</Td>
                  <Td>
                    <div className="font-medium">{p.name}</div>
                    {p.description && (
                      <div className="text-xs text-text-muted mt-0.5 line-clamp-1">
                        {p.description}
                      </div>
                    )}
                  </Td>
                  <Td>{formatMoneyMinor(p.baseFeeMinor, p.currency)}</Td>
                  <Td className="text-text-muted">{p.currency}</Td>
                  <Td>
                    <Badge tone={p.active ? "emerald" : "neutral"}>
                      {p.active ? t("common.active") : t("common.inactive")}
                    </Badge>
                  </Td>
                  <Td className="font-mono text-xs text-text-dim">
                    {shortId(p.id)}
                  </Td>
                </Tr>
              ))}
            </TBody>
          </Table>
        ) : (
          <EmptyState
            icon={<Layers className="h-5 w-5" />}
            title={t("plans.empty.title")}
            description={t("plans.empty.description")}
            action={
              <Button
                leftIcon={<Plus className="h-4 w-4" />}
                onClick={() => setOpen(true)}
              >
                {t("plans.empty.cta")}
              </Button>
            }
          />
        )}
      </Card>

      <Dialog
        open={open}
        onClose={() => setOpen(false)}
        title={t("plans.newPlan")}
        size="lg"
        footer={
          <>
            <Button variant="ghost" onClick={() => setOpen(false)}>
              {t("common.cancel")}
            </Button>
            <Button
              type="submit"
              form="create-plan-form"
              loading={createMut.isPending}
            >
              {t("common.create")}
            </Button>
          </>
        }
      >
        <form
          id="create-plan-form"
          onSubmit={handleSubmit((data) =>
            createMut.mutate({
              code: data.code,
              name: data.name,
              description: data.description || undefined,
              baseFeeMinor: data.baseFeeMinor,
              currency: data.currency,
            }),
          )}
          className="space-y-4"
        >
          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="code">{t("common.code")}</Label>
              <Input id="code" placeholder="YEMEN_ID_BASIC" {...register("code")} />
              {errors.code && (
                <p className="mt-1 text-xs text-accent-rose">
                  {errors.code.message}
                </p>
              )}
            </div>
            <div>
              <Label htmlFor="currency">{t("common.currency")}</Label>
              <Input
                id="currency"
                placeholder="YER"
                maxLength={3}
                {...register("currency")}
              />
              {errors.currency && (
                <p className="mt-1 text-xs text-accent-rose">
                  {errors.currency.message}
                </p>
              )}
            </div>
          </div>
          <div>
            <Label htmlFor="name">{t("common.name")}</Label>
            <Input id="name" placeholder="Yemen ID — Basic" {...register("name")} />
            {errors.name && (
              <p className="mt-1 text-xs text-accent-rose">
                {errors.name.message}
              </p>
            )}
          </div>
          <div>
            <Label htmlFor="description">{t("common.description")}</Label>
            <Input
              id="description"
              {...register("description")}
            />
          </div>
          <div>
            <Label htmlFor="baseFeeMinor">{t("plans.fields.baseFeeMinor")}</Label>
            <Input
              id="baseFeeMinor"
              type="number"
              min={0}
              {...register("baseFeeMinor")}
            />
            <p className="mt-1 text-xs text-text-dim">
              {t("plans.fields.baseFeeHelp")}
            </p>
          </div>
        </form>
      </Dialog>
    </div>
  );
}
