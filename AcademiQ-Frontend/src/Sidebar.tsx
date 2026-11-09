
import { useNavigate } from "react-router-dom";

import {
  SquaresIcon,
  FolderIcon,
  ChecklistIcon,
  FlagIcon,
  UploadIcon,
  ChatIcon,
  UsersIcon,
  LogoIcon,
  BellIcon,
  DocumentIcon,
} from "./icons";

const studentNavItems = [
  { id: "dashboard", label: "Dashboard", icon: SquaresIcon },
  { id: "projects", label: "My Projects", icon: FolderIcon },
  { id: "tasks", label: "Tasks", icon: ChecklistIcon },
  { id: "milestones", label: "Milestones", icon: FlagIcon },
  { id: "submissions", label: "Submissions", icon: UploadIcon },
  { id: "feedback", label: "Supervisor Feedback", icon: ChatIcon },
  { id: "team", label: "Team", icon: UsersIcon },
  { id: "discussion", label: "Discussion", icon: ChatIcon },
  { id: "notifications", label: "Notifications", icon: BellIcon },
];

const supervisorNavItems = [
  { id: "dashboard", label: "Dashboard", icon: SquaresIcon },
  { id: "projects", label: "Supervised Projects", icon: FolderIcon },
  { id: "reviews", label: "Review Queue", icon: DocumentIcon },
  { id: "milestones", label: "Milestone Tracking", icon: FlagIcon },
  { id: "students", label: "Students", icon: UsersIcon },
  { id: "feedback", label: "Evaluations", icon: ChatIcon },
  { id: "notifications", label: "Notifications", icon: BellIcon },
];

interface SidebarProps {
  active: string;
  setActive: (id: string) => void;
  role?: "student" | "supervisor";
}

interface AuthSession {
  fullName?: string;
  email?: string;
  role?: string;
  token?: string;
}

function getCurrentUser(): AuthSession | null {
  try {
    const stored = sessionStorage.getItem("academiq_auth");

    if (!stored) {
      return null;
    }

    const parsed: unknown = JSON.parse(stored);

    if (typeof parsed !== "object" || parsed === null) {
      return null;
    }

    return parsed as AuthSession;
  } catch {
    return null;
  }
}

function getInitials(name: string): string {
  const parts = name
    .trim()
    .split(/\s+/)
    .filter(Boolean);

  if (parts.length === 0) {
    return "AQ";
  }

  if (parts.length === 1) {
    return parts[0].slice(0, 2).toUpperCase();
  }

  return (
    parts[0][0] + parts[parts.length - 1][0]
  ).toUpperCase();
}

export default function Sidebar({
  active,
  setActive,
  role = "student",
}: SidebarProps) {
  const navigate = useNavigate();

  const session = getCurrentUser();

  const fullName =
    session?.fullName?.trim() ||
    session?.email?.split("@")[0] ||
    "AcademiQ User";

  const initials = getInitials(fullName);

  const roleLabel =
    role === "supervisor"
      ? "Academic Supervisor"
      : "Student";

  // ==========================================
  // LOGOUT
  // ==========================================
  const handleLogout = () => {
    // Remove the authenticated user's JWT and details.
    sessionStorage.removeItem("academiq_auth");

    // Remove the workspace selected by this user.
    sessionStorage.removeItem("academiq_workspace");

    // Return to the login page.
    navigate("/login", {
      replace: true,
    });
  };

  return (
    <aside
      className="flex flex-col h-full"
      style={{
        width: 240,
        minWidth: 240,
        background: "#1e1b3a",
        color: "white",
        overflow: "hidden",
      }}
    >
      {/* Logo */}
      <div className="flex items-center gap-2.5 px-6 py-6 mb-2">
        <div
          className="flex items-center justify-center rounded-xl text-white"
          style={{
            width: 36,
            height: 36,
            background:
              "linear-gradient(135deg, #818cf8, #7c3aed)",
          }}
        >
          <LogoIcon />
        </div>

        <div>
          <div
            style={{
              fontFamily: "var(--font-display)",
              fontSize: 16,
              fontWeight: 500,
              letterSpacing: "-0.02em",
              lineHeight: 1.1,
            }}
          >
            AcademiQ
          </div>

          <div
            style={{
              fontSize: 10,
              color: "rgba(255,255,255,0.4)",
              letterSpacing: "0.06em",
              textTransform: "uppercase",
            }}
          >
            Project Hub
          </div>
        </div>
      </div>

      {/* Navigation */}
      <nav className="flex-1 px-3 overflow-y-auto">
        <div
          style={{
            fontSize: 10,
            color: "rgba(255,255,255,0.35)",
            letterSpacing: "0.08em",
            textTransform: "uppercase",
            padding: "4px 12px 8px",
          }}
        >
          {role === "supervisor"
            ? "Supervisor Portal"
            : "Student Portal"}
        </div>

        {(role === "supervisor"
          ? supervisorNavItems
          : studentNavItems
        ).map(({ id, label, icon: Icon }) => {
          const isActive = active === id;

          return (
            <button
              key={id}
              type="button"
              onClick={() => setActive(id)}
              className="flex items-center gap-3 w-full text-left rounded-xl mb-0.5 transition-all"
              style={{
                padding: "9px 12px",
                fontSize: 13.5,
                fontWeight: isActive ? 600 : 400,
                color: isActive
                  ? "white"
                  : "rgba(255,255,255,0.55)",
                background: isActive
                  ? "rgba(99,102,241,0.35)"
                  : "transparent",
                borderLeft: isActive
                  ? "2px solid #818cf8"
                  : "2px solid transparent",
              }}
              onMouseEnter={(e) => {
                if (!isActive) {
                  e.currentTarget.style.background =
                    "rgba(255,255,255,0.06)";
                }
              }}
              onMouseLeave={(e) => {
                if (!isActive) {
                  e.currentTarget.style.background =
                    "transparent";
                }
              }}
            >
              <Icon className="w-4 h-4" />
              {label}
            </button>
          );
        })}
      </nav>

      {/* Logout */}
      <div className="px-3 pb-4">
        <button
          type="button"
          onClick={handleLogout}
          className="flex items-center gap-3 w-full rounded-xl transition-all"
          style={{
            padding: "11px 14px",
            background: "rgba(239,68,68,0.10)",
            border: "1px solid rgba(239,68,68,0.18)",
            color: "#fca5a5",
            fontSize: 13,
            fontWeight: 600,
            cursor: "pointer",
          }}
          onMouseEnter={(e) => {
            e.currentTarget.style.background =
              "rgba(239,68,68,0.20)";
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.background =
              "rgba(239,68,68,0.10)";
          }}
        >
          <svg
            width="17"
            height="17"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden="true"
          >
            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
            <polyline points="16 17 21 12 16 7" />
            <line x1="21" y1="12" x2="9" y2="12" />
          </svg>

          Logout
        </button>
      </div>
    </aside>
  );
}
