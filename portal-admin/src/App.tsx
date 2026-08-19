import { Navigate, Route, Routes } from "react-router-dom";
import { Layout } from "@/components/layout/Layout";
import { ProtectedRoute } from "@/components/layout/ProtectedRoute";
import { LandingPage } from "@/pages/LandingPage";
import { LoginPage } from "@/pages/LoginPage";
import { DashboardPage } from "@/pages/DashboardPage";
import { TenantsPage } from "@/pages/TenantsPage";
import { TenantDetailPage } from "@/pages/TenantDetailPage";
import { PlansPage } from "@/pages/PlansPage";
import { PlanDetailPage } from "@/pages/PlanDetailPage";
import { SubscriptionsPage } from "@/pages/SubscriptionsPage";
import { CatalogPage } from "@/pages/CatalogPage";
import { TransactionsPage } from "@/pages/TransactionsPage";
import { TransactionDetailPage } from "@/pages/TransactionDetailPage";
import { BillingPage } from "@/pages/BillingPage";
import { ReportsPage } from "@/pages/ReportsPage";
import { UsersPage } from "@/pages/UsersPage";
import { SupportPage } from "@/pages/SupportPage";
import { PinGate } from "@/components/PinGate";
import { PortalLayout } from "@/pages/portal/PortalLayout";
import { PortalLoginPage } from "@/pages/portal/PortalLoginPage";
import { PortalDashboardPage } from "@/pages/portal/PortalDashboardPage";
import { PortalTransactionsPage } from "@/pages/portal/PortalTransactionsPage";
import { PortalSubscriptionsPage } from "@/pages/portal/PortalSubscriptionsPage";
import { PortalWalletPage } from "@/pages/portal/PortalWalletPage";
import { PortalApiKeysPage } from "@/pages/portal/PortalApiKeysPage";
import { PortalDevicesPage } from "@/pages/portal/PortalDevicesPage";
import { PortalProfilePage } from "@/pages/portal/PortalProfilePage";

export default function App() {
  return (
    <Routes>
      {/* Public marketing/landing page — anyone can see it. */}
      <Route path="/" element={<LandingPage />} />

      <Route path="/login" element={<LoginPage />} />

      <Route
        element={
          <ProtectedRoute>
            <Layout />
          </ProtectedRoute>
        }
      >
        <Route path="dashboard" element={<DashboardPage />} />
        <Route path="tenants" element={<TenantsPage />} />
        <Route path="tenants/:id" element={<TenantDetailPage />} />
        <Route path="plans" element={<PlansPage />} />
        <Route path="plans/:id" element={<PlanDetailPage />} />
        <Route path="subscriptions" element={<SubscriptionsPage />} />
        <Route path="catalog" element={<CatalogPage />} />
        <Route
          path="transactions"
          element={
            <PinGate>
              <TransactionsPage />
            </PinGate>
          }
        />
        <Route
          path="transactions/:id"
          element={
            <PinGate>
              <TransactionDetailPage />
            </PinGate>
          }
        />
        <Route path="reports" element={<ReportsPage />} />
        <Route path="billing" element={<BillingPage />} />
        <Route path="users" element={<UsersPage />} />
        <Route path="support" element={<SupportPage />} />
      </Route>

      {/* Tenant-facing portal — separate auth, tenant-scoped data. */}
      <Route path="/portal/login" element={<PortalLoginPage />} />
      <Route path="/portal" element={<PortalLayout />}>
        <Route index element={<PortalDashboardPage />} />
        <Route path="transactions" element={<PortalTransactionsPage />} />
        <Route path="subscriptions" element={<PortalSubscriptionsPage />} />
        <Route path="wallet" element={<PortalWalletPage />} />
        <Route path="api-keys" element={<PortalApiKeysPage />} />
        <Route path="devices" element={<PortalDevicesPage />} />
        <Route path="profile" element={<PortalProfilePage />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
