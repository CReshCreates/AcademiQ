
import React from "react";
import ReactDOM from "react-dom/client";

import {
  BrowserRouter,
  Navigate,
  Route,
  Routes,
} from "react-router-dom";

import type { ReactNode } from "react";

import PortalApp from "./App";
import Login from "./auth/Login";
import Signup from "./auth/Signup";
import WorkspaceSelector from "./WorkspaceSelector";

import "./index.css";
import "./responsive.css";
import "./landing-timeline.css";

type PortalRole = "STUDENT" | "SUPERVISOR";

type AuthSession = {
  token: string;
  email: string;
  fullName?: string;
  role: string;
};

type SelectedWorkspace = {
  workspaceId: number;
  workspaceName: string;
  workspaceSlug: string;
  workspaceType: "ACADEMIC" | "ORGANIZATIONAL";
  role: string;
};

function getAuthSession(): AuthSession | null {
  try {
    const stored = sessionStorage.getItem("academiq_auth");
    if (!stored) return null;

    const session = JSON.parse(stored) as AuthSession;

    if (
      typeof session.token !== "string" ||
      !session.token.trim() ||
      typeof session.email !== "string" ||
      !session.email.trim()
    ) {
      return null;
    }

    return session;
  } catch {
    return null;
  }
}

function getSelectedWorkspace(): SelectedWorkspace | null {
  try {
    const stored = sessionStorage.getItem(
      "academiq_workspace"
    );

    if (!stored) return null;

    const workspace = JSON.parse(stored) as SelectedWorkspace;

    if (
      !Number.isInteger(workspace.workspaceId) ||
      workspace.workspaceId <= 0 ||
      !workspace.role ||
      !workspace.workspaceType
    ) {
      return null;
    }

    return workspace;
  } catch {
    return null;
  }
}

function ProtectedRoute({
  allowedRole,
  children,
}: {
  allowedRole: PortalRole;
  children: ReactNode;
}) {
  const session = getAuthSession();
  const workspace = getSelectedWorkspace();

  if (!session) {
    return <Navigate to="/login" replace />;
  }

  if (!workspace) {
    return <Navigate to="/workspaces/select" replace />;
  }

  if (
    workspace.workspaceType !== "ACADEMIC" ||
    workspace.role !== allowedRole
  ) {
    return <Navigate to="/workspaces/select" replace />;
  }

  // This is frontend navigation protection only.
  // Spring Boot must authorize each API request independently.
  return <>{children}</>;
}

function WorkspaceSelectionRoute() {
  if (!getAuthSession()) {
    return <Navigate to="/login" replace />;
  }

  return <WorkspaceSelector />;
}

ReactDOM.createRoot(
  document.getElementById("root")!
).render(
  <React.StrictMode>
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Login />} />
        <Route path="/login" element={<Login />} />
        <Route path="/signup" element={<Signup />} />

        <Route
          path="/workspaces/select"
          element={<WorkspaceSelectionRoute />}
        />

        <Route
          path="/student/*"
          element={
            <ProtectedRoute allowedRole="STUDENT">
              <PortalApp />
            </ProtectedRoute>
          }
        />

        <Route
          path="/supervisor/*"
          element={
            <ProtectedRoute allowedRole="SUPERVISOR">
              <PortalApp />
            </ProtectedRoute>
          }
        />

        <Route
          path="*"
          element={<Navigate to="/" replace />}
        />
      </Routes>
    </BrowserRouter>
  </React.StrictMode>
);
