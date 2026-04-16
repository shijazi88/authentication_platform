import { Outlet } from "react-router-dom";
import { Sidebar } from "./Sidebar";
import { Topbar } from "./Topbar";

export function Layout() {
  return (
    <div className="min-h-screen flex bg-bg text-text relative">
      {/* Background decoration */}
      <div className="pointer-events-none fixed inset-0 bg-gradient-mesh opacity-60" />
      <div className="pointer-events-none fixed inset-0 bg-grid-pattern bg-grid-32 opacity-[0.4] [mask-image:radial-gradient(ellipse_at_top,black_30%,transparent_75%)]" />

      <Sidebar />

      <div className="flex-1 flex flex-col min-w-0 relative">
        <Topbar />
        <main className="flex-1 px-8 py-8 page-enter">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
