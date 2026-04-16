import { useQuery } from "@tanstack/react-query";
import { Library, Zap } from "lucide-react";
import { useTranslation } from "react-i18next";
import { listFields, listOperations, listServices } from "@/api/catalog";
import { PageHeader } from "@/components/ui/PageHeader";
import {
  Card,
  CardBody,
  CardHeader,
  CardTitle,
} from "@/components/ui/Card";
import { Badge } from "@/components/ui/Badge";
import { PageLoader } from "@/components/ui/Spinner";
import { formatMoneyMinor } from "@/lib/format";

export function CatalogPage() {
  const { t } = useTranslation();
  const servicesQ = useQuery({ queryKey: ["services"], queryFn: listServices });

  return (
    <div>
      <PageHeader
        title={t("catalog.title")}
        description={t("catalog.subtitle")}
      />

      {servicesQ.isLoading ? (
        <PageLoader />
      ) : servicesQ.data?.length ? (
        <div className="space-y-4">
          {servicesQ.data.map((service) => (
            <ServiceBlock
              key={service.id}
              serviceId={service.id}
              serviceName={service.name}
              serviceCode={service.code}
              description={service.description}
              connectorKey={service.connectorKey}
              active={service.active}
            />
          ))}
        </div>
      ) : (
        <Card>
          <CardBody className="text-center text-xs text-text-muted py-12">
            {t("catalog.empty")}
          </CardBody>
        </Card>
      )}
    </div>
  );
}

function ServiceBlock({
  serviceId,
  serviceName,
  serviceCode,
  description,
  connectorKey,
  active,
}: {
  serviceId: string;
  serviceName: string;
  serviceCode: string;
  description: string | null;
  connectorKey: string;
  active: boolean;
}) {
  const { t } = useTranslation();
  const operationsQ = useQuery({
    queryKey: ["operations", serviceId],
    queryFn: () => listOperations(serviceId),
  });

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center gap-3 min-w-0">
          <div className="h-10 w-10 rounded-lg bg-gradient-to-br from-accent-violet to-accent-cyan flex items-center justify-center shadow-glow shrink-0">
            <Library className="h-5 w-5 text-white" />
          </div>
          <div className="min-w-0">
            <CardTitle className="flex items-center gap-2">
              {serviceName}
              <Badge tone={active ? "emerald" : "neutral"}>
                {active ? t("common.active") : t("common.inactive")}
              </Badge>
            </CardTitle>
            <div className="text-xs text-text-muted mt-0.5 truncate">
              <span className="font-mono">{serviceCode}</span>
              {description && <span> · {description}</span>}
            </div>
          </div>
        </div>
        <div className="text-[10px] uppercase tracking-wider text-text-dim">
          {t("catalog.connector")}:{" "}
          <span className="text-accent-cyan font-mono">{connectorKey}</span>
        </div>
      </CardHeader>
      <CardBody>
        {operationsQ.isLoading ? (
          <PageLoader />
        ) : (
          <div className="space-y-3">
            {operationsQ.data?.map((op) => (
              <OperationBlock
                key={op.id}
                operationId={op.id}
                code={op.code}
                name={op.name}
                priceMinor={op.defaultUnitPriceMinor}
                currency={op.currency}
                description={op.description}
              />
            ))}
          </div>
        )}
      </CardBody>
    </Card>
  );
}

function OperationBlock({
  operationId,
  code,
  name,
  priceMinor,
  currency,
  description,
}: {
  operationId: string;
  code: string;
  name: string;
  priceMinor: number;
  currency: string;
  description: string | null;
}) {
  const { t } = useTranslation();
  const fieldsQ = useQuery({
    queryKey: ["fields", operationId],
    queryFn: () => listFields(operationId),
  });

  return (
    <div className="rounded-lg border border-border/10 bg-bg-elevated/30 p-4">
      <div className="flex items-center justify-between gap-4">
        <div className="min-w-0">
          <div className="flex items-center gap-2">
            <Zap className="h-4 w-4 text-accent-cyan" />
            <span className="font-mono text-sm text-text">{code}</span>
            <span className="text-text-dim">·</span>
            <span className="text-sm text-text-muted">{name}</span>
          </div>
          {description && (
            <div className="text-xs text-text-muted mt-1.5">{description}</div>
          )}
        </div>
        <Badge tone="violet">{formatMoneyMinor(priceMinor, currency)}</Badge>
      </div>

      {fieldsQ.data && fieldsQ.data.length > 0 && (
        <div className="mt-3 pt-3 border-t border-border/10">
          <div className="text-[10px] uppercase tracking-wider text-text-dim mb-2">
            {t("catalog.responseFields", { value: fieldsQ.data.length })}
          </div>
          <div className="flex flex-wrap gap-1.5">
            {fieldsQ.data.map((f) => (
              <code
                key={f.id}
                className="px-2 py-0.5 rounded bg-bg-elevated/60 border border-border/10 text-[11px] font-mono text-text-muted"
                title={f.description ?? undefined}
              >
                {f.path}
              </code>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
