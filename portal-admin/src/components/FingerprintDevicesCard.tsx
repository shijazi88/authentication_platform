import { useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";
import {
  Fingerprint,
  Plus,
  Pencil,
  Trash2,
  Upload,
  Download,
} from "lucide-react";
import {
  listDevices,
  createDevice,
  updateDevice,
  deleteDevice,
  importDevices,
  downloadDeviceTemplate,
} from "@/api/devices";
import { useAuth } from "@/lib/auth";
import { canWrite } from "@/lib/access";
import type { DeviceImportResult, FingerprintDevice } from "@/types/api";
import { Card, CardBody, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Table, TBody, THead, Th, Td, Tr } from "@/components/ui/Table";
import { Dialog } from "@/components/ui/Dialog";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { formatDate } from "@/lib/format";

const EMPTY = { name: "", model: "", type: "", serialNumber: "" };

/** Admin management of a tenant's fingerprint devices (CRUD + Excel import). */
export function FingerprintDevicesCard({ tenantId }: { tenantId: string }) {
  const { t } = useTranslation();
  const qc = useQueryClient();
  const writable = canWrite(useAuth((s) => s.role));
  const fileRef = useRef<HTMLInputElement>(null);

  const devicesQ = useQuery({
    queryKey: ["devices", tenantId],
    queryFn: () => listDevices(tenantId),
  });

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<FingerprintDevice | null>(null);
  const [form, setForm] = useState(EMPTY);
  const [importResult, setImportResult] = useState<DeviceImportResult | null>(null);

  const invalidate = () => qc.invalidateQueries({ queryKey: ["devices", tenantId] });
  const onErr = (e: any) => toast.error(e?.response?.data?.message ?? t("devices.error"));
  const set = (k: keyof typeof EMPTY) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm((f) => ({ ...f, [k]: e.target.value }));

  const saveMut = useMutation({
    mutationFn: () => {
      const body = {
        name: form.name,
        model: form.model || undefined,
        type: form.type || undefined,
        serialNumber: form.serialNumber,
      };
      return editing
        ? updateDevice(tenantId, editing.id, body)
        : createDevice(tenantId, body);
    },
    onSuccess: () => {
      invalidate();
      toast.success(editing ? t("devices.updated") : t("devices.created"));
      setDialogOpen(false);
    },
    onError: onErr,
  });

  const deleteMut = useMutation({
    mutationFn: (id: string) => deleteDevice(tenantId, id),
    onSuccess: () => {
      invalidate();
      toast.success(t("devices.deleted"));
    },
    onError: onErr,
  });

  const importMut = useMutation({
    mutationFn: (file: File) => importDevices(tenantId, file),
    onSuccess: (res) => {
      invalidate();
      setImportResult(res);
      toast.success(t("devices.importDone", { created: res.created, skipped: res.skipped }));
    },
    onError: onErr,
  });

  function openCreate() {
    setEditing(null);
    setForm(EMPTY);
    setDialogOpen(true);
  }
  function openEdit(d: FingerprintDevice) {
    setEditing(d);
    setForm({ name: d.name, model: d.model ?? "", type: d.type ?? "", serialNumber: d.serialNumber });
    setDialogOpen(true);
  }
  function onFilePicked(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (file) importMut.mutate(file);
    e.target.value = "";
  }

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center gap-2">
          <Fingerprint className="h-4 w-4 text-text-muted" />
          <CardTitle>{t("devices.title")}</CardTitle>
        </div>
        {writable && (
          <div className="flex items-center gap-1">
            <Button
              variant="ghost"
              size="sm"
              leftIcon={<Download className="h-3.5 w-3.5" />}
              onClick={() => downloadDeviceTemplate(tenantId).catch(onErr)}
            >
              {t("devices.template")}
            </Button>
            <Button
              variant="ghost"
              size="sm"
              leftIcon={<Upload className="h-3.5 w-3.5" />}
              loading={importMut.isPending}
              onClick={() => fileRef.current?.click()}
            >
              {t("devices.import")}
            </Button>
            <input
              ref={fileRef}
              type="file"
              accept=".xlsx"
              className="hidden"
              onChange={onFilePicked}
            />
            <Button
              variant="ghost"
              size="sm"
              leftIcon={<Plus className="h-3.5 w-3.5" />}
              onClick={openCreate}
            >
              {t("devices.newDevice")}
            </Button>
          </div>
        )}
      </CardHeader>
      <CardBody className="p-0">
        {devicesQ.data?.length ? (
          <Table>
            <THead>
              <Tr>
                <Th>{t("common.name")}</Th>
                <Th>{t("devices.fields.model")}</Th>
                <Th>{t("devices.fields.type")}</Th>
                <Th>{t("devices.fields.serial")}</Th>
                {writable && <Th>{t("common.actions")}</Th>}
              </Tr>
            </THead>
            <TBody>
              {devicesQ.data.map((d) => (
                <Tr key={d.id}>
                  <Td className="font-medium">{d.name}</Td>
                  <Td className="text-sm text-text-muted">{d.model ?? "—"}</Td>
                  <Td className="text-sm text-text-muted">{d.type ?? "—"}</Td>
                  <Td className="font-mono text-xs">{d.serialNumber}</Td>
                  {writable && (
                    <Td>
                      <div className="flex items-center gap-1">
                        <Button
                          variant="ghost"
                          size="sm"
                          leftIcon={<Pencil className="h-3.5 w-3.5" />}
                          onClick={() => openEdit(d)}
                        >
                          {t("common.edit")}
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          leftIcon={<Trash2 className="h-3.5 w-3.5" />}
                          loading={deleteMut.isPending && deleteMut.variables === d.id}
                          onClick={() => {
                            if (confirm(t("devices.deleteConfirm", { name: d.name })))
                              deleteMut.mutate(d.id);
                          }}
                        >
                          {t("common.delete")}
                        </Button>
                      </div>
                    </Td>
                  )}
                </Tr>
              ))}
            </TBody>
          </Table>
        ) : (
          <div className="px-6 py-8 text-center text-xs text-text-muted">
            {t("devices.empty")}
          </div>
        )}
      </CardBody>

      {/* Create / edit */}
      <Dialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        title={editing ? t("devices.editDevice") : t("devices.newDevice")}
        size="lg"
        footer={
          <>
            <Button variant="ghost" onClick={() => setDialogOpen(false)}>
              {t("common.cancel")}
            </Button>
            <Button
              loading={saveMut.isPending}
              disabled={!form.name || !form.serialNumber}
              onClick={() => saveMut.mutate()}
            >
              {editing ? t("common.save") : t("common.create")}
            </Button>
          </>
        }
      >
        <div className="grid grid-cols-2 gap-4">
          <div>
            <Label htmlFor="d-name">{t("common.name")}</Label>
            <Input id="d-name" value={form.name} onChange={set("name")} />
          </div>
          <div>
            <Label htmlFor="d-serial">{t("devices.fields.serial")}</Label>
            <Input id="d-serial" value={form.serialNumber} onChange={set("serialNumber")} />
          </div>
          <div>
            <Label htmlFor="d-model">{t("devices.fields.model")}</Label>
            <Input id="d-model" value={form.model} onChange={set("model")} />
          </div>
          <div>
            <Label htmlFor="d-type">{t("devices.fields.type")}</Label>
            <Input id="d-type" placeholder={t("devices.typePlaceholder")} value={form.type} onChange={set("type")} />
          </div>
        </div>
      </Dialog>

      {/* Import result */}
      <Dialog
        open={importResult !== null}
        onClose={() => setImportResult(null)}
        title={t("devices.importResult")}
        footer={<Button onClick={() => setImportResult(null)}>{t("common.done")}</Button>}
      >
        {importResult && (
          <div className="space-y-3 text-sm">
            <div className="flex gap-4">
              <span className="text-accent-emerald">
                {t("devices.importCreated", { value: importResult.created })}
              </span>
              <span className="text-text-muted">
                {t("devices.importSkipped", { value: importResult.skipped })}
              </span>
            </div>
            {importResult.errors.length > 0 && (
              <div className="max-h-56 overflow-auto rounded-lg border border-border/10 divide-y divide-border/10">
                {importResult.errors.map((er, i) => (
                  <div key={i} className="px-3 py-1.5 text-xs">
                    <span className="font-mono text-text-muted">
                      {t("devices.rowLabel", { row: er.row })}
                    </span>{" "}
                    <span className="text-accent-rose">{er.message}</span>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </Dialog>
    </Card>
  );
}
