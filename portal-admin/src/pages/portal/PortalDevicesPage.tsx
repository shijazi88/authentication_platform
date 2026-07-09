import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { Search } from "lucide-react";
import { listMyDevices } from "@/api/tenant";
import { Card, CardBody } from "@/components/ui/Card";
import { Table, TBody, THead, Th, Td, Tr } from "@/components/ui/Table";
import { Input } from "@/components/ui/Input";
import { PageLoader } from "@/components/ui/Spinner";

export function PortalDevicesPage() {
  const { t } = useTranslation();
  const [q, setQ] = useState("");
  const devicesQ = useQuery({ queryKey: ["t-devices"], queryFn: listMyDevices });

  const filtered = useMemo(() => {
    const needle = q.trim().toLowerCase();
    const all = devicesQ.data ?? [];
    if (!needle) return all;
    return all.filter((d) =>
      [d.name, d.model, d.type, d.serialNumber]
        .filter(Boolean)
        .some((v) => v!.toLowerCase().includes(needle)),
    );
  }, [devicesQ.data, q]);

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between gap-4 flex-wrap">
        <h1 className="text-2xl font-bold">{t("devices.title")}</h1>
        <div className="relative w-72 max-w-full">
          <Search className="absolute start-3 top-1/2 -translate-y-1/2 h-4 w-4 text-text-dim" />
          <Input
            className="ps-9"
            placeholder={t("portal.devices.search")}
            value={q}
            onChange={(e) => setQ(e.target.value)}
          />
        </div>
      </div>

      <Card>
        {devicesQ.isLoading ? (
          <PageLoader />
        ) : filtered.length ? (
          <Table>
            <THead>
              <Tr>
                <Th>{t("common.name")}</Th>
                <Th>{t("devices.fields.model")}</Th>
                <Th>{t("devices.fields.type")}</Th>
                <Th>{t("devices.fields.serial")}</Th>
              </Tr>
            </THead>
            <TBody>
              {filtered.map((d) => (
                <Tr key={d.id}>
                  <Td className="font-medium">{d.name}</Td>
                  <Td className="text-sm text-text-muted">{d.model ?? "—"}</Td>
                  <Td className="text-sm text-text-muted">{d.type ?? "—"}</Td>
                  <Td className="font-mono text-xs">{d.serialNumber}</Td>
                </Tr>
              ))}
            </TBody>
          </Table>
        ) : (
          <CardBody className="px-6 py-10 text-center text-sm text-text-muted">
            {q ? t("portal.devices.noMatch") : t("portal.devices.empty")}
          </CardBody>
        )}
      </Card>

      <p className="text-xs text-text-dim">{t("portal.devices.managedNote")}</p>
    </div>
  );
}
