
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import "./WorkspaceSelector.css";

type WorkspaceRole =
  | "ADMIN"
  | "STUDENT"
  | "SUPERVISOR"
  | "MANAGER"
  | "MEMBER";

type WorkspaceType = "ACADEMIC" | "ORGANIZATIONAL";

type Workspace = {
  workspaceId: number;
  workspaceName: string;
  workspaceSlug: string;
  workspaceType: WorkspaceType;
  role: WorkspaceRole;
};

type AuthSession = {
  token: string;
  email: string;
  fullName?: string;
  role: string;
};

const avatarColors = [
  "#4f46e5",
  "#b45309",
  "#0f766e",
  "#be185d",
  "#7c3aed",
  "#0369a1",
];

function getWorkspaceInitials(name: string): string {
  const words = name.trim().split(/\s+/).filter(Boolean);

  if (words.length === 0) return "AQ";

  if (words.length === 1) {
    return words[0].slice(0, 2).toUpperCase();
  }

  return (
    words[0][0] + words[words.length - 1][0]
  ).toUpperCase();
}

function getWorkspaceColor(workspaceId: number): string {
  return avatarColors[
    Math.abs(workspaceId) % avatarColors.length
  ];
}

export default function WorkspaceSelector() {
  const navigate = useNavigate();

  const [workspaces, setWorkspaces] = useState<Workspace[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectingId, setSelectingId] = useState<number | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;

    async function loadWorkspaces() {
      try {
        const stored = sessionStorage.getItem("academiq_auth");

        if (!stored) {
          navigate("/login", { replace: true });
          return;
        }

        const session = JSON.parse(stored) as AuthSession;

        const response = await fetch("/workspaces/my", {
          headers: {
            Authorization: `Bearer ${session.token}`,
          },
        });

        if (!response.ok) {
          throw new Error(
            response.status === 401 || response.status === 403
              ? "Your session could not be verified. Please sign in again."
              : `Unable to load workspaces (HTTP ${response.status}).`
          );
        }

        const data: Workspace[] = await response.json();

        if (!Array.isArray(data)) {
          throw new Error("Invalid workspace response.");
        }

        if (!cancelled) {
          setWorkspaces(data);
        }
      } catch (err) {
        if (!cancelled) {
          setError(
            err instanceof Error
              ? err.message
              : "Unable to load workspaces."
          );
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    loadWorkspaces();

    return () => {
      cancelled = true;
    };
  }, [navigate]);

  async function selectWorkspace(workspaceId: number) {
    if (selectingId !== null) return;

    setSelectingId(workspaceId);
    setError("");

    try {
      const stored = sessionStorage.getItem("academiq_auth");

      if (!stored) {
        navigate("/login", { replace: true });
        return;
      }

      const session = JSON.parse(stored) as AuthSession;

      const response = await fetch(
        `/workspaces/${workspaceId}/selection`,
        {
          headers: {
            Authorization: `Bearer ${session.token}`,
          },
        }
      );

      if (!response.ok) {
        throw new Error(
          `Unable to select workspace (HTTP ${response.status}).`
        );
      }

      const selected: Workspace = await response.json();

      if (selected.workspaceId !== workspaceId) {
        throw new Error("Workspace selection response is invalid.");
      }

      if (
        selected.workspaceType === "ACADEMIC" &&
        (selected.role === "STUDENT" ||
          selected.role === "SUPERVISOR")
      ) {
        sessionStorage.setItem(
          "academiq_workspace",
          JSON.stringify(selected)
        );

        navigate(
          selected.role === "STUDENT"
            ? "/student"
            : "/supervisor",
          { replace: true }
        );
      } else {
        setError(
          "This workspace is available, but its portal " +
            "is not implemented in the current MVP."
        );
      }
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Workspace selection failed."
      );
    } finally {
      setSelectingId(null);
    }
  }

  function logout() {
    sessionStorage.removeItem("academiq_auth");
    sessionStorage.removeItem("academiq_workspace");

    navigate("/login", { replace: true });
  }

  return (
    <main className="workspace-select-page">
      <header className="workspace-select-header">
        <div className="workspace-brand">
          <span className="workspace-brand-mark">A</span>
          <span>AcademiQ</span>
        </div>
      </header>

      <section className="workspace-select-content">
        <h1 className="workspace-select-title">
            Choose your workspace
      </h1>

      <p className="workspace-select-subtitle">
            Where would you like to work today?
       </p>

        {loading && (
          <p className="workspace-select-message">
            Loading your workspaces...
          </p>
        )}

        {error && (
          <div
            role="alert"
            className="workspace-select-error"
          >
            {error}
          </div>
        )}

        {!loading && workspaces.length === 0 && (
          <div className="workspace-select-empty">
            <p>You don't have an active workspace yet.</p>
            <span>
              Ask a workspace Admin to invite you, or create
              your own workspace.
            </span>
          </div>
        )}

        {!loading && workspaces.length > 0 && (
          <div className="workspace-profile-grid">
            {workspaces.map((workspace) => {
              const isSelecting =
                selectingId === workspace.workspaceId;

              return (
                <button
                  key={workspace.workspaceId}
                  type="button"
                  className="workspace-profile"
                  disabled={selectingId !== null}
                  onClick={() =>
                    selectWorkspace(workspace.workspaceId)
                  }
                  aria-label={`Enter ${workspace.workspaceName} as ${workspace.role}`}
                >
                  <div
                    className="workspace-profile-avatar"
                    style={{
                      backgroundColor: getWorkspaceColor(
                        workspace.workspaceId
                      ),
                    }}
                  >
                    <span className="workspace-profile-initials">
                      {getWorkspaceInitials(
                        workspace.workspaceName
                      )}
                    </span>

                    {isSelecting && (
                      <span className="workspace-profile-loading">
                        Opening...
                      </span>
                    )}
                  </div>

                  <span className="workspace-profile-name">
                    {workspace.workspaceName}
                  </span>

                  <span className="workspace-profile-role">
                    {workspace.role}
                  </span>
                </button>
              );
            })}
          </div>
        )}

        <button
          type="button"
          className="workspace-select-logout"
          onClick={logout}
        >
          Sign out
        </button>
      </section>

      <footer className="workspace-select-footer">
        AcademiQ Project Hub
      </footer>
    </main>
  );
}
