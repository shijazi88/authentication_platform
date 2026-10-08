import { ReportsView } from "@/pages/ReportsPage";

/** The bank's own reports — same summary / details reports and exports as the admin, scoped to this bank. */
export function PortalReportsPage() {
  return <ReportsView scope="tenant" />;
}
