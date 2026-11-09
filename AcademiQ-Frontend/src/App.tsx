
import { useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";

import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from "recharts";

import {
  TasksWorkspace,
  TeamWorkspace,
  SubmissionsWorkspace,
} from "./StudentWorkspaces";

import {
  SupervisorDashboard,
  SupervisorProjects,
  ReviewQueue,
  SupervisorCalendar,
  SupervisorFeedback,
} from "./Supervisor";

import Sidebar from "./Sidebar";
import ProjectDetails from "./ProjectDetails";
import CreateProjectModal from "./CreateProjectModal";
import DiscussionWorkspace from "./DiscussionWorkspace";
import ProjectInvitationNotifications from "./ProjectInvitationNotifications";

import {
  ChecklistIcon,
  FlagIcon,
  UploadIcon,
  ChatIcon,
  UsersIcon,
  BellIcon,
  PlusIcon,
  SparklesIcon,
  DocumentIcon,
  MessageIcon,
} from "./icons";

import {
  getMyStudentProjects,
  type StudentProject,
} from "./api/studentProjects";

import {
  getProjectDetails,
  type ProjectDetailsResponse,
} from "./api/projectDetails";

// ============================================================
// TYPES — MATCHED TO THE SPRING BOOT BACKEND
// ============================================================

interface MyTaskResponse {
  taskId: number;
  projectId: number;
  projectTitle: string;
  milestoneId: number | null;
  title: string;
  description: string | null;
  status: string;
  priority: string;
  deadline: string | null;
  progress: number | null;
}

interface MilestoneResponse {
  milestoneId: number;
  title: string;
  description: string | null;
  deadline: string | null;
  status: string;
  progress: number | null;
}

interface DashboardMilestone extends MilestoneResponse {
  projectId: number;
  projectTitle: string;
}

interface DashboardActivity {
  projectId: number;
  projectTitle: string;
  description: string;
  createdAt: string | null;
}

interface DashboardFeedback {
  projectId: number;
  projectTitle: string;
  supervisorName: string;
  milestoneName: string | null;
  description: string;
  createdAt: string | null;
}

interface DashboardData {
  projects: StudentProject[];
  tasks: MyTaskResponse[];
  milestones: DashboardMilestone[];
  feedback: DashboardFeedback[];
  activities: DashboardActivity[];
  milestoneUnavailable: number;
  detailsUnavailable: number;
}

const EMPTY_DASHBOARD: DashboardData = {
  projects: [],
  tasks: [],
  milestones: [],
  feedback: [],
  activities: [],
  milestoneUnavailable: 0,
  detailsUnavailable: 0,
};

// ============================================================
// SHARED HELPERS
// ============================================================

function getAuthToken(): string {
  const stored = sessionStorage.getItem("academiq_auth");

  if (!stored) {
    throw new Error("Please log in again.");
  }

  try {
    const parsed: unknown = JSON.parse(stored);

    if (
      typeof parsed === "object" &&
      parsed !== null &&
      "token" in parsed &&
      typeof parsed.token === "string" &&
      parsed.token.trim()
    ) {
      return parsed.token;
    }
  } catch {
    // Invalid session.
  }

  throw new Error("Your login session is invalid.");
}

async function getAuthenticatedJson<T>(
  url: string,
  signal?: AbortSignal
): Promise<T> {
  const response = await fetch(url, {
    method: "GET",
    signal,
    headers: {
      Authorization: `Bearer ${getAuthToken()}`,
      Accept: "application/json",
    },
  });

  if (!response.ok) {
    if (response.status === 401) {
      throw new Error("Your login session has expired.");
    }

    if (response.status === 403) {
      throw new Error("Access denied for this workspace.");
    }

    throw new Error(
      `Request failed (HTTP ${response.status}): ${url}`
    );
  }

  return (await response.json()) as T;
}

function safeProgress(value: number | null | undefined): number {
  if (typeof value !== "number" || !Number.isFinite(value)) {
    return 0;
  }

  return Math.min(100, Math.max(0, value));
}

function formatStatus(value: string | null | undefined): string {
  if (!value) return "Not available";

  return value
    .toLowerCase()
    .replace(/_/g, " ")
    .replace(/\b\w/g, (letter: string) =>
      letter.toUpperCase()
    );
}

function formatDate(value: string | null | undefined): string {
  if (!value) return "Not set";

  const date = new Date(value);

  return Number.isNaN(date.getTime())
    ? value
    : date.toLocaleDateString(undefined, {
        month: "short",
        day: "numeric",
        year: "numeric",
      });
}

function formatShortDate(
  value: string | null | undefined
): string {
  if (!value) return "—";

  const date = new Date(value);

  return Number.isNaN(date.getTime())
    ? value
    : date.toLocaleDateString(undefined, {
        month: "short",
        day: "numeric",
      });
}

function statusColor(value: string | null | undefined): string {
  switch (value?.toUpperCase()) {
    case "COMPLETED":
    case "APPROVED":
      return "#10b981";
    case "IN_PROGRESS":
      return "#6366f1";
    case "PROPOSED":
    case "PENDING":
      return "#f59e0b";
    case "REJECTED":
    case "OVERDUE":
      return "#ef4444";
    default:
      return "#8b5cf6";
  }
}

function isCompleted(status: string): boolean {
  return ["COMPLETED", "APPROVED"].includes(
    status.toUpperCase()
  );
}

function isInProgress(status: string): boolean {
  return ["IN_PROGRESS", "UNDER_REVIEW"].includes(
    status.toUpperCase()
  );
}

function getDashboardGreeting(): string {
  let firstName = "Student";

  try {
    const stored = sessionStorage.getItem("academiq_auth");

    if (stored) {
      const session = JSON.parse(stored);

      if (
        typeof session.fullName === "string" &&
        session.fullName.trim()
      ) {
        firstName = session.fullName.trim().split(/\s+/)[0];
      }
    }
  } catch {
    // Use default.
  }

  const hour = new Date().getHours();

  const greeting =
    hour < 12
      ? "Good morning"
      : hour < 17
      ? "Good afternoon"
      : "Good evening";

  return `${greeting}, ${firstName} ✦`;
}

function cardStyle(): React.CSSProperties {
  return {
    background: "white",
    boxShadow: "0 1px 4px rgba(26,24,48,0.06)",
  };
}

// ============================================================
// SHARED UI
// ============================================================

function Header({
  title,
  subtitle,
  action,
  onAction,
}: {
  title: string;
  subtitle?: string;
  action?: string;
  onAction?: () => void;
}) {
  return (
    <header
      className="sticky top-0 z-30 flex items-center justify-between px-8 py-4"
      style={{
        background: "rgba(247,246,243,0.9)",
        backdropFilter: "blur(12px)",
        borderBottom: "1px solid #e8e7f0",
      }}
    >
      <div>
        <div
          style={{
            fontSize: 11,
            color: "#6b6890",
            letterSpacing: "0.06em",
            textTransform: "uppercase",
          }}
        >
          {subtitle || "AcademiQ"}
        </div>

        <div
          style={{
            fontFamily: "var(--font-display)",
            fontSize: 20,
            fontWeight: 500,
            color: "#1a1830",
            lineHeight: 1.2,
          }}
        >
          {title}
        </div>
      </div>

      <div className="flex items-center gap-3">
        {action && (
          <button
            type="button"
            onClick={onAction}
            className="flex items-center gap-2 rounded-xl px-4 py-2"
            style={{
              background:
                "linear-gradient(135deg,#6366f1,#8b5cf6)",
              color: "white",
              fontSize: 13,
              fontWeight: 600,
            }}
          >
            <PlusIcon />
            {action}
          </button>
        )}

        
        <ProjectInvitationNotifications
          onInvitationAccepted={() => {
            window.dispatchEvent(
              new Event("academiq:invitation-accepted")
            );
          }}
        />


        <div
          className="flex items-center justify-center rounded-full font-semibold text-sm text-white"
          style={{
            width: 38,
            height: 38,
            background:
              "linear-gradient(135deg,#6366f1,#8b5cf6)",
          }}
        >
          AQ
        </div>
      </div>
    </header>
  );
}

function Card({
  children,
  className = "",
  style = {},
}: {
  children: ReactNode;
  className?: string;
  style?: React.CSSProperties;
}) {
  return (
    <div
      className={`rounded-2xl p-6 ${className}`}
      style={{ ...cardStyle(), ...style }}
    >
      {children}
    </div>
  );
}

function Badge({
  children,
  color = "#6366f1",
}: {
  children: ReactNode;
  color?: string;
}) {
  return (
    <span
      className="rounded-full px-2.5 py-1 text-xs font-semibold"
      style={{
        background: color + "18",
        color,
      }}
    >
      {children}
    </span>
  );
}

function StatCard({
  label,
  value,
  sub,
  color,
  icon,
}: {
  label: string;
  value: number | string;
  sub: string;
  color: string;
  icon: ReactNode;
}) {
  return (
    <div
      className="rounded-2xl p-5 flex flex-col gap-3"
      style={cardStyle()}
    >
      <div
        className="flex items-center justify-center rounded-xl"
        style={{
          width: 40,
          height: 40,
          background: color + "18",
          color,
        }}
      >
        {icon}
      </div>

      <div>
        <div
          style={{
            fontFamily: "var(--font-display)",
            fontSize: 28,
            fontWeight: 500,
            color: "#1a1830",
            lineHeight: 1,
          }}
        >
          {value}
        </div>

        <div
          style={{
            fontSize: 12.5,
            color: "#6b6890",
            marginTop: 3,
          }}
        >
          {label}
        </div>
      </div>

      <div style={{ fontSize: 11.5, color }}>
        {sub}
      </div>
    </div>
  );
}

// ============================================================
// DASHBOARD DATA LOADING
// ============================================================

async function loadDashboard(
  signal: AbortSignal
): Promise<DashboardData> {
  // Start with the authenticated student's real projects and tasks.
  const [projects, tasks] = await Promise.all([
    getMyStudentProjects(signal),
    getAuthenticatedJson<MyTaskResponse[]>(
      "/api/tasks/assigned-to-me",
      signal
    ),
  ]);

  if (!Array.isArray(projects) || !Array.isArray(tasks)) {
    throw new Error("Invalid dashboard response.");
  }

  if (projects.length === 0) {
    return {
      ...EMPTY_DASHBOARD,
      projects,
      tasks,
    };
  }

  // Retrieve milestones and project overview for every project.
  // Individual failures are recorded instead of replacing real
  // data with invented dashboard values.
  const projectResults = await Promise.all(
    projects.map(async (project) => {
      const projectId = project.projectId;

      const [milestoneResult, detailsResult] =
        await Promise.allSettled([
          getAuthenticatedJson<MilestoneResponse[]>(
            `/api/projects/${projectId}/milestones`,
            signal
          ),
          getProjectDetails(projectId, signal),
        ]);

      return {
        project,
        milestoneResult,
        detailsResult,
      };
    })
  );

  const milestones: DashboardMilestone[] = [];
  const feedback: DashboardFeedback[] = [];
  const activities: DashboardActivity[] = [];

  let milestoneUnavailable = 0;
  let detailsUnavailable = 0;

  for (const result of projectResults) {
    const { project, milestoneResult, detailsResult } =
      result;

    if (
      milestoneResult.status === "fulfilled" &&
      Array.isArray(milestoneResult.value)
    ) {
      for (const milestone of milestoneResult.value) {
        milestones.push({
          ...milestone,
          projectId: project.projectId,
          projectTitle: project.projectTitle,
        });
      }
    } else {
      milestoneUnavailable++;
    }

    if (detailsResult.status !== "fulfilled") {
      detailsUnavailable++;
      continue;
    }

    const details: ProjectDetailsResponse =
      detailsResult.value;

    const feedbackCard = details.feedbackCard;

    if (
      feedbackCard &&
      feedbackCard.feedbackDescription
    ) {
      feedback.push({
        projectId: project.projectId,
        projectTitle: project.projectTitle,
        supervisorName:
          feedbackCard.supervisorsFullname ||
          details.supervisorName ||
          "Supervisor",
        milestoneName:
          feedbackCard.milestoneName || null,
        description:
          feedbackCard.feedbackDescription,
        createdAt:
          feedbackCard.activityCreatedTime || null,
      });
    }

    for (const activity of details.recentActivitiesCard || []) {
      if (!activity.activityDescription) {
        continue;
      }

      activities.push({
        projectId: project.projectId,
        projectTitle: project.projectTitle,
        description: activity.activityDescription,
        createdAt:
          activity.activityCreatedTime || null,
      });
    }
  }

  milestones.sort((a, b) => {
    if (!a.deadline) return 1;
    if (!b.deadline) return -1;
    return a.deadline.localeCompare(b.deadline);
  });

  feedback.sort((a, b) =>
    (b.createdAt || "").localeCompare(
      a.createdAt || ""
    )
  );

  activities.sort((a, b) =>
    (b.createdAt || "").localeCompare(
      a.createdAt || ""
    )
  );

  return {
    projects,
    tasks,
    milestones,
    feedback,
    activities,
    milestoneUnavailable,
    detailsUnavailable,
  };
}

// ============================================================
// STUDENT DASHBOARD — ALL SECTIONS
// ============================================================

function DashboardPage({
  onNavigate,
  onOpenProject,
}: {
  onNavigate: (id: string) => void;
  onOpenProject: (id: number) => void;
}) {
  const [data, setData] =
    useState<DashboardData>(EMPTY_DASHBOARD);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [refreshKey, setRefreshKey] = useState(0);

  useEffect(() => {
    const controller = new AbortController();

    async function load() {
      setLoading(true);
      setError("");

      try {
        const response = await loadDashboard(
          controller.signal
        );

        if (!controller.signal.aborted) {
          setData(response);
        }
      } catch (err) {
        if (!controller.signal.aborted) {
          setError(
            err instanceof Error
              ? err.message
              : "Unable to load dashboard."
          );
        }
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      }
    }

    load();

    return () => controller.abort();
  }, [refreshKey]);

  const projects = data.projects;
  const tasks = data.tasks;

  const activeProjects = projects.filter((project) =>
    ["APPROVED", "IN_PROGRESS"].includes(
      (project.currentStage || "").toUpperCase()
    )
  ).length;

  const proposalProjects = projects.filter(
    (project) =>
      (project.currentStage || "").toUpperCase() ===
      "PROPOSED"
  ).length;

  const completedTasks = tasks.filter((task) =>
    isCompleted(task.status)
  ).length;

  const inProgressTasks = tasks.filter((task) =>
    isInProgress(task.status)
  ).length;

  const overdueTasks = tasks.filter((task) => {
    if (!task.deadline || isCompleted(task.status)) {
      return false;
    }

    const today = new Date().toISOString().slice(0, 10);
    return task.deadline < today;
  }).length;

  const upcomingMilestones = data.milestones
    .filter(
      (milestone) =>
        milestone.deadline !== null &&
        !isCompleted(milestone.status)
    )
    .slice(0, 4);

  const progressChartData = useMemo(
    () =>
      projects.map((project) => ({
        name:
          project.projectTitle.length > 19
            ? project.projectTitle.slice(0, 19) + "…"
            : project.projectTitle,
        progress: safeProgress(project.progress),
      })),
    [projects]
  );

  const feedbackCount = data.feedback.length;

  const numberOrLoading = (number: number) =>
    loading || error ? "—" : number;

  return (
    <main
      className="flex-1 overflow-y-auto"
      style={{
        background: "#f7f6f3",
        minWidth: 0,
      }}
    >
      <Header
        title={getDashboardGreeting()}
        subtitle="Dashboard"
      />

      <div className="px-8 py-6 space-y-6">
        {error && (
          <Card>
            <div role="alert" style={{ color: "#b91c1c" }}>
              {error}
            </div>

            <button
              type="button"
              onClick={() =>
                setRefreshKey((key) => key + 1)
              }
              className="rounded-xl px-4 py-2 mt-3"
              style={{
                background: "#f0eff8",
                color: "#6366f1",
                fontWeight: 600,
                fontSize: 12,
              }}
            >
              Retry
            </button>
          </Card>
        )}

        {/* ==========================================
            PORTFOLIO + MILESTONES
           ========================================== */}

        <div className="aq-dashboard-overview">
          <section className="aq-portfolio-hero">
            <div className="aq-portfolio-heading">
              <div>
                <span className="aq-portfolio-eyebrow">
                  YOUR ACADEMIC WORKSPACE
                </span>

                <h2>All your projects. One place.</h2>

                <p>
                  Track the projects you lead and the teams
                  you contribute to.
                </p>
              </div>

              <button
                type="button"
                onClick={() => onNavigate("projects")}
                className="aq-portfolio-view"
              >
                View all projects →
              </button>
            </div>

            <div className="aq-portfolio-summary">
              <div>
                <strong>
                  {numberOrLoading(projects.length)}
                </strong>
                <span>Total projects</span>
              </div>

              <div>
                <strong>
                  {numberOrLoading(activeProjects)}
                </strong>
                <span>Active</span>
              </div>

              <div>
                <strong>
                  {numberOrLoading(proposalProjects)}
                </strong>
                <span>Proposal stage</span>
              </div>
            </div>

            {loading && (
              <p style={{ padding: 18, fontSize: 13 }}>
                Loading academic projects...
              </p>
            )}

            {!loading &&
              !error &&
              projects.length === 0 && (
                <p style={{ padding: 18, fontSize: 13 }}>
                  No projects yet.
                </p>
              )}

            {!loading &&
              !error &&
              projects.length > 0 && (
                <div className="aq-portfolio-projects">
                  {projects.map((project) => {
                    const progress = safeProgress(
                      project.progress
                    );

                    return (
                      <button
                        type="button"
                        key={project.projectId}
                        onClick={() =>
                          onOpenProject(project.projectId)
                        }
                        className="aq-portfolio-project"
                      >
                        <div className="aq-portfolio-project-top">
                          <span>
                            {project.projectTitle}
                          </span>

                          <b>{progress}%</b>
                        </div>

                        <div className="aq-portfolio-project-stage">
                          {formatStatus(
                            project.currentStage
                          )}
                        </div>

                        <div className="aq-portfolio-track">
                          <div
                            style={{
                              width: `${progress}%`,
                            }}
                          />
                        </div>
                      </button>
                    );
                  })}
                </div>
              )}
          </section>

          <Card className="aq-dashboard-deadlines">
            <div className="flex items-center justify-between mb-4">
              <h3
                style={{
                  fontFamily: "var(--font-display)",
                  fontSize: 16,
                }}
              >
                Upcoming Milestones
              </h3>

              <button
                type="button"
                onClick={() => onNavigate("milestones")}
                style={{
                  fontSize: 12,
                  color: "#6366f1",
                }}
              >
                View all
              </button>
            </div>

            {loading && (
              <p style={{ fontSize: 12, color: "#6b6890" }}>
                Loading milestones...
              </p>
            )}

            {!loading &&
              !error &&
              upcomingMilestones.length === 0 && (
                <p style={{ fontSize: 12, color: "#6b6890" }}>
                  No upcoming milestones available.
                </p>
              )}

            <div className="space-y-3">
              {!loading &&
                !error &&
                upcomingMilestones.map((milestone) => (
                  <button
                    type="button"
                    key={`${milestone.projectId}-${milestone.milestoneId}`}
                    onClick={() =>
                      onOpenProject(milestone.projectId)
                    }
                    className="aq-deadline-item"
                  >
                    <span
                      className="aq-deadline-dot"
                      style={{
                        background: statusColor(
                          milestone.status
                        ),
                      }}
                    />

                    <span className="aq-deadline-copy">
                      <strong>{milestone.title}</strong>

                      <small>
                        {milestone.projectTitle}
                      </small>
                    </span>

                    <span className="aq-deadline-date">
                      {formatShortDate(
                        milestone.deadline
                      )}
                    </span>
                  </button>
                ))}
            </div>

            {!loading &&
              data.milestoneUnavailable > 0 && (
                <p
                  style={{
                    marginTop: 12,
                    color: "#a8a6c0",
                    fontSize: 11,
                  }}
                >
                  Some project milestones could not be loaded.
                </p>
              )}
          </Card>
        </div>

        {/* ==========================================
            REAL TASK STATISTICS
           ========================================== */}

        <div className="aq-dashboard-stats grid grid-cols-4 gap-4">
          <StatCard
            label="Tasks Completed"
            value={numberOrLoading(completedTasks)}
            sub="Assigned to you"
            color="#6366f1"
            icon={
              <ChecklistIcon className="w-5 h-5" />
            }
          />

          <StatCard
            label="In Progress"
            value={numberOrLoading(inProgressTasks)}
            sub="Including tasks under review"
            color="#8b5cf6"
            icon={
              <SparklesIcon className="w-5 h-5" />
            }
          />

          <StatCard
            label="Overdue"
            value={numberOrLoading(overdueTasks)}
            sub="Incomplete tasks past deadline"
            color="#ef4444"
            icon={<FlagIcon className="w-5 h-5" />}
          />

          <StatCard
            label="Supervisor Reviews"
            value={
              loading || error
                ? "—"
                : data.detailsUnavailable
                ? "—"
                : feedbackCount
            }
            sub="Latest feedback per project"
            color="#f59e0b"
            icon={<ChatIcon className="w-5 h-5" />}
          />
        </div>

        {/* ==========================================
            CURRENT PROGRESS + SUPERVISOR FEEDBACK
           ========================================== */}

        <div className="aq-dashboard-bottom-grid grid gap-5">
          <Card>
            <div className="flex items-start justify-between mb-5">
              <div>
                <h3
                  style={{
                    fontFamily: "var(--font-display)",
                    fontSize: 17,
                  }}
                >
                  Project Completion Overview
                </h3>

                <p
                  style={{
                    fontSize: 12.5,
                    color: "#6b6890",
                  }}
                >
                  Current progress across your projects
                </p>
              </div>
            </div>

            {loading ? (
              <p style={{ fontSize: 12, color: "#6b6890" }}>
                Loading progress...
              </p>
            ) : !error && progressChartData.length > 0 ? (
              <ResponsiveContainer
                width="100%"
                height={210}
              >
                <AreaChart
                  data={progressChartData}
                  margin={{
                    top: 0,
                    right: 0,
                    left: -24,
                    bottom: 0,
                  }}
                >
                  <defs>
                    <linearGradient
                      id="pg"
                      x1="0"
                      y1="0"
                      x2="0"
                      y2="1"
                    >
                      <stop
                        offset="5%"
                        stopColor="#6366f1"
                        stopOpacity={0.15}
                      />

                      <stop
                        offset="95%"
                        stopColor="#6366f1"
                        stopOpacity={0}
                      />
                    </linearGradient>
                  </defs>

                  <CartesianGrid
                    strokeDasharray="3 3"
                    stroke="#f0eff8"
                    vertical={false}
                  />

                  <XAxis
                    dataKey="name"
                    tick={{
                      fill: "#a8a6c0",
                      fontSize: 10,
                    }}
                    axisLine={false}
                    tickLine={false}
                  />

                  <YAxis
                    domain={[0, 100]}
                    tick={{
                      fill: "#a8a6c0",
                      fontSize: 11,
                    }}
                    axisLine={false}
                    tickLine={false}
                    tickFormatter={(value: number) =>
                      `${value}%`
                    }
                  />

                  <Tooltip
                    formatter={(value) => [
                      `${value}%`,
                      "Progress",
                    ]}
                  />

                  <Area
                    type="monotone"
                    dataKey="progress"
                    stroke="#6366f1"
                    strokeWidth={2.5}
                    fill="url(#pg)"
                  />
                </AreaChart>
              </ResponsiveContainer>
            ) : (
              <p style={{ fontSize: 12, color: "#6b6890" }}>
                No project progress data available.
              </p>
            )}

            <p
              style={{
                fontSize: 11,
                color: "#a8a6c0",
                marginTop: 10,
              }}
            >
              This shows current progress, not historical
              weekly progress.
            </p>
          </Card>

          <Card>
            <div className="flex justify-between mb-4">
              <h3
                style={{
                  fontFamily: "var(--font-display)",
                  fontSize: 16,
                }}
              >
                Supervisor Feedback
              </h3>

              <button
                type="button"
                onClick={() => onNavigate("feedback")}
                style={{
                  fontSize: 12,
                  color: "#6366f1",
                }}
              >
                View all
              </button>
            </div>

            {loading && (
              <p style={{ fontSize: 12, color: "#6b6890" }}>
                Loading supervisor feedback...
              </p>
            )}

            {!loading &&
              !error &&
              data.feedback.length === 0 && (
                <p style={{ fontSize: 12, color: "#6b6890" }}>
                  No supervisor feedback available yet.
                </p>
              )}

            <div className="space-y-3">
              {!loading &&
                !error &&
                data.feedback.slice(0, 3).map((item) => (
                  <button
                    type="button"
                    key={item.projectId}
                    onClick={() =>
                      onOpenProject(item.projectId)
                    }
                    className="w-full rounded-xl p-3.5 text-left"
                    style={{
                      background: "#f7f6f3",
                      borderLeft: "3px solid #f59e0b",
                    }}
                  >
                    <div className="flex justify-between gap-2">
                      <b style={{ fontSize: 11.5 }}>
                        {item.milestoneName ||
                          "Supervisor Feedback"}
                      </b>

                      <Badge color="#f59e0b">
                        Review
                      </Badge>
                    </div>

                    <p
                      style={{
                        fontSize: 11.5,
                        color: "#6b6890",
                        lineHeight: 1.5,
                        marginTop: 6,
                      }}
                    >
                      {item.description}
                    </p>

                    <small
                      style={{
                        color: "#a8a6c0",
                      }}
                    >
                      {item.supervisorName} ·{" "}
                      {item.projectTitle} ·{" "}
                      {formatDate(item.createdAt)}
                    </small>
                  </button>
                ))}
            </div>
          </Card>
        </div>

        {/* ==========================================
            RECENT PROJECT ACTIVITY + QUICK ACTIONS
           ========================================== */}

        <div className="aq-dashboard-half-grid grid gap-5">
          <Card>
            <div className="flex justify-between mb-4">
              <h3
                style={{
                  fontFamily: "var(--font-display)",
                  fontSize: 16,
                }}
              >
                Recent Project Activity
              </h3>

              <span
                style={{
                  fontSize: 11,
                  color: "#a8a6c0",
                }}
              >
                Latest updates
              </span>
            </div>

            {loading && (
              <p style={{ fontSize: 12, color: "#6b6890" }}>
                Loading recent activity...
              </p>
            )}

            {!loading &&
              !error &&
              data.activities.length === 0 && (
                <p style={{ fontSize: 12, color: "#6b6890" }}>
                  No recent project activity available.
                </p>
              )}

            <div className="space-y-2">
              {!loading &&
                !error &&
                data.activities.slice(0, 5).map(
                  (activity, index) => (
                    <button
                      type="button"
                      key={`${activity.projectId}-${index}`}
                      onClick={() =>
                        onOpenProject(
                          activity.projectId
                        )
                      }
                      className="w-full flex items-start gap-3 rounded-xl p-3 text-left"
                      style={{
                        background: "#f7f6f3",
                      }}
                    >
                      <div
                        className="rounded-full mt-1"
                        style={{
                          width: 9,
                          height: 9,
                          background: "#6366f1",
                          flexShrink: 0,
                        }}
                      />

                      <div className="flex-1">
                        <b style={{ fontSize: 12.5 }}>
                          {activity.description}
                        </b>

                        <div
                          style={{
                            fontSize: 11,
                            color: "#a8a6c0",
                            marginTop: 4,
                          }}
                        >
                          {activity.projectTitle} ·{" "}
                          {formatDate(
                            activity.createdAt
                          )}
                        </div>
                      </div>
                    </button>
                  )
                )}
            </div>
          </Card>

          <Card>
            <h3
              style={{
                fontFamily: "var(--font-display)",
                fontSize: 16,
                marginBottom: 16,
              }}
            >
              Quick Actions
            </h3>

            <div className="grid grid-cols-2 gap-3">
              {[
                {
                  label: "Submit Chapter",
                  sub: "Upload to supervisor",
                  icon: (
                    <UploadIcon className="w-5 h-5" />
                  ),
                  color: "#6366f1",
                  target: "submissions",
                },
                {
                  label: "Supervisor Feedback",
                  sub: "View project reviews",
                  icon: (
                    <ChatIcon className="w-5 h-5" />
                  ),
                  color: "#8b5cf6",
                  target: "feedback",
                },
                {
                  label: "My Tasks",
                  sub: "Manage your workload",
                  icon: <PlusIcon />,
                  color: "#10b981",
                  target: "tasks",
                },
                {
                  label: "Milestones",
                  sub: "Check deadlines",
                  icon: (
                    <FlagIcon className="w-5 h-5" />
                  ),
                  color: "#f59e0b",
                  target: "milestones",
                },
                {
                  label: "Team Chat",
                  sub: "Message your group",
                  icon: (
                    <UsersIcon className="w-5 h-5" />
                  ),
                  color: "#06b6d4",
                  target: "discussion",
                },
                {
                  label: "My Projects",
                  sub: "Open project workspace",
                  icon: (
                    <DocumentIcon className="w-5 h-5" />
                  ),
                  color: "#ec4899",
                  target: "projects",
                },
              ].map((item) => (
                <button
                  type="button"
                  key={item.label}
                  onClick={() =>
                    onNavigate(item.target)
                  }
                  className="flex items-start gap-3 rounded-xl p-3.5 text-left"
                  style={{
                    background: "#f7f6f3",
                  }}
                >
                  <div
                    className="flex items-center justify-center rounded-xl"
                    style={{
                      width: 36,
                      height: 36,
                      background: item.color + "18",
                      color: item.color,
                    }}
                  >
                    {item.icon}
                  </div>

                  <div>
                    <b style={{ fontSize: 12.5 }}>
                      {item.label}
                    </b>

                    <div
                      style={{
                        fontSize: 11,
                        color: "#a8a6c0",
                      }}
                    >
                      {item.sub}
                    </div>
                  </div>
                </button>
              ))}
            </div>
          </Card>
        </div>
      </div>
    </main>
  );
}

// ============================================================
// MY PROJECTS — EXISTING WORKING API
// ============================================================

function StudentProjects({
  onOpen,
}: {
  onOpen: (id: string) => void;
}) {
  const [projects, setProjects] =
    useState<StudentProject[]>([]);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [refreshKey, setRefreshKey] = useState(0);
  const [showCreate, setShowCreate] = useState(false);

  useEffect(() => {
    const controller = new AbortController();

    async function load() {
      setLoading(true);
      setError("");

      try {
        const result = await getMyStudentProjects(
          controller.signal
        );

        if (!controller.signal.aborted) {
          setProjects(result);
        }
      } catch (err) {
        if (!controller.signal.aborted) {
          setError(
            err instanceof Error
              ? err.message
              : "Unable to load projects."
          );
        }
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      }
    }

    load();

    return () => controller.abort();
  }, [refreshKey]);

  return (
    <main
      className="flex-1 overflow-y-auto"
      style={{
        background: "#f7f6f3",
        minWidth: 0,
      }}
    >
      <Header
        title="My Projects"
        subtitle="Academic Portfolio"
        action="Create Project"
        onAction={() => setShowCreate(true)}
      />

      <div className="px-8 py-6 space-y-5">
        <div
          className="rounded-2xl p-5"
          style={{
            background:
              "linear-gradient(135deg,#312e81,#4f46e5)",
            color: "white",
          }}
        >
          <div className="flex justify-between items-center">
            <div>
              <Badge color="#34d399">
                Academic Projects
              </Badge>

              <h2
                style={{
                  fontFamily: "var(--font-display)",
                  fontSize: 21,
                  marginTop: 10,
                }}
              >
                Your Academic Portfolio
              </h2>

              <p
                style={{
                  fontSize: 12.5,
                  color: "rgba(255,255,255,.7)",
                  marginTop: 4,
                }}
              >
                View and manage projects associated
                with your account.
              </p>
            </div>

            <div className="text-right">
              <div
                style={{
                  fontFamily: "var(--font-display)",
                  fontSize: 30,
                }}
              >
                {loading || error
                  ? "—"
                  : projects.length}
              </div>

              <div
                style={{
                  fontSize: 10,
                  color: "rgba(255,255,255,.7)",
                }}
              >
                Projects
              </div>
            </div>
          </div>
        </div>

        {loading && (
          <Card>Loading your projects...</Card>
        )}

        {!loading && error && (
          <Card>
            <p
              role="alert"
              style={{ color: "#b91c1c" }}
            >
              {error}
            </p>

            <button
              type="button"
              onClick={() =>
                setRefreshKey((key) => key + 1)
              }
              style={{
                marginTop: 12,
                color: "#6366f1",
              }}
            >
              Retry
            </button>
          </Card>
        )}

        {!loading &&
          !error &&
          projects.length === 0 && (
            <Card>
              <div className="py-12 text-center">
                <h2
                  style={{
                    fontFamily: "var(--font-display)",
                    fontSize: 21,
                  }}
                >
                  No projects yet
                </h2>

                <p
                  style={{
                    color: "#6b6890",
                    fontSize: 13,
                    marginTop: 7,
                  }}
                >
                  Your academic projects will appear here.
                </p>
              </div>
            </Card>
          )}

        {!loading &&
          !error &&
          projects.length > 0 && (
            <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-5">
              {projects.map((project) => {
                const color = statusColor(
                  project.currentStage
                );

                const progress = safeProgress(
                  project.progress
                );

                return (
                  <Card
                    key={project.projectId}
                    style={{
                      padding: 0,
                      overflow: "hidden",
                    }}
                  >
                    <div
                      style={{
                        height: 5,
                        background: color,
                      }}
                    />

                    <div className="p-5">
                      <div className="flex justify-between items-center">
                        <Badge color={color}>
                          {formatStatus(
                            project.currentStage
                          )}
                        </Badge>

                        <span
                          style={{
                            fontSize: 10.5,
                            color: "#a8a6c0",
                          }}
                        >
                          #{project.projectId}
                        </span>
                      </div>

                      <h3
                        style={{
                          fontFamily: "var(--font-display)",
                          fontSize: 17,
                          marginTop: 12,
                          minHeight: 44,
                        }}
                      >
                        {project.projectTitle}
                      </h3>

                      <p
                        style={{
                          color: "#6b6890",
                          fontSize: 11.5,
                          marginTop: 8,
                        }}
                      >
                        Supervisor:{" "}
                        {project.supervisorName ||
                          "Not assigned"}
                      </p>

                      <p
                        style={{
                          color: "#6b6890",
                          fontSize: 11.5,
                          marginTop: 5,
                        }}
                      >
                        Subject:{" "}
                        {project.programmeSubjectName ||
                          "Not available"}
                      </p>

                      <p
                        style={{
                          color: "#6b6890",
                          fontSize: 11.5,
                          marginTop: 5,
                        }}
                      >
                        Semester:{" "}
                        {project.semesterName ||
                          "Not available"}
                      </p>

                      <div style={{ marginTop: 18 }}>
                        <div
                          className="flex justify-between mb-1"
                          style={{
                            fontSize: 11,
                            color: "#6b6890",
                          }}
                        >
                          <span>Project progress</span>
                          <b>{progress}%</b>
                        </div>

                        <div
                          className="rounded-full overflow-hidden"
                          style={{
                            height: 6,
                            background: "#e8e7f0",
                          }}
                        >
                          <div
                            style={{
                              width: `${progress}%`,
                              height: "100%",
                              background: color,
                            }}
                          />
                        </div>
                      </div>

                      <button
                        type="button"
                        onClick={() =>
                          onOpen(
                            String(project.projectId)
                          )
                        }
                        className="w-full rounded-xl py-2.5 mt-5"
                        style={{
                          background: "#f0eff8",
                          color: "#6366f1",
                          fontSize: 12,
                          fontWeight: 600,
                        }}
                      >
                        Open Project →
                      </button>
                    </div>
                  </Card>
                );
              })}
            </div>
          )}

        {showCreate && (
          <CreateProjectModal
            onClose={() => setShowCreate(false)}
            onCreated={() => {
              setShowCreate(false);
              setRefreshKey(key => key + 1);
            }}
          />
        )}
      </div>
    </main>
  );
}

// ============================================================
// FEEDBACK PAGE — LIVE PROJECT OVERVIEW FEEDBACK
// ============================================================

function FeedbackPage({
  onOpenProject,
}: {
  onOpenProject: (id: number) => void;
}) {
  const [feedback, setFeedback] =
    useState<DashboardFeedback[]>([]);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const controller = new AbortController();

    async function load() {
      try {
        const projects =
          await getMyStudentProjects(controller.signal);

        const responses = await Promise.allSettled(
          projects.map((project) =>
            getProjectDetails(
              project.projectId,
              controller.signal
            )
          )
        );

        const results: DashboardFeedback[] = [];

        responses.forEach((response, index) => {
          if (response.status !== "fulfilled") return;

          const project = projects[index];
          const details = response.value;
          const item = details.feedbackCard;

          if (!item?.feedbackDescription) return;

          results.push({
            projectId: project.projectId,
            projectTitle: project.projectTitle,
            supervisorName:
              item.supervisorsFullname ||
              details.supervisorName ||
              "Supervisor",
            milestoneName: item.milestoneName,
            description: item.feedbackDescription,
            createdAt: item.activityCreatedTime,
          });
        });

        if (!controller.signal.aborted) {
          setFeedback(results);
        }
      } catch (err) {
        if (!controller.signal.aborted) {
          setError(
            err instanceof Error
              ? err.message
              : "Unable to load feedback."
          );
        }
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      }
    }

    load();

    return () => controller.abort();
  }, []);

  return (
    <main
      className="flex-1 overflow-y-auto"
      style={{
        background: "#f7f6f3",
        minWidth: 0,
      }}
    >
      <Header
        title="Supervisor Feedback"
        subtitle="Academic Review"
      />

      <div
        className="px-8 py-6 space-y-4"
        style={{ maxWidth: 1200, margin: "0 auto" }}
      >
        <Card>
          <div className="flex justify-between items-center">
            <div>
              <h2
                style={{
                  fontFamily: "var(--font-display)",
                  fontSize: 20,
                }}
              >
                Latest Supervisor Feedback
              </h2>

              <p
                style={{
                  color: "#6b6890",
                  fontSize: 12,
                  marginTop: 6,
                }}
              >
                Latest available feedback for each project
              </p>
            </div>

            <Badge>
              {loading ? "—" : feedback.length}
            </Badge>
          </div>
        </Card>

        {loading && <Card>Loading feedback...</Card>}

        {error && (
          <Card>
            <p role="alert" style={{ color: "#b91c1c" }}>
              {error}
            </p>
          </Card>
        )}

        {!loading &&
          !error &&
          feedback.length === 0 && (
            <Card>
              No supervisor feedback available yet.
            </Card>
          )}

        {!loading &&
          !error &&
          feedback.map((item) => (
            <Card key={item.projectId}>
              <div className="flex items-start gap-4">
                <div
                  className="rounded-xl p-3"
                  style={{
                    background: "#fef3c7",
                    color: "#b45309",
                  }}
                >
                  <MessageIcon className="w-5 h-5" />
                </div>

                <div className="flex-1">
                  <h3
                    style={{
                      fontFamily: "var(--font-display)",
                      fontSize: 17,
                    }}
                  >
                    {item.milestoneName ||
                      "Supervisor Feedback"}
                  </h3>

                  <p
                    style={{
                      fontSize: 11,
                      color: "#6b6890",
                    }}
                  >
                    {item.projectTitle} ·{" "}
                    {item.supervisorName}
                  </p>

                  <p
                    style={{
                      marginTop: 12,
                      fontSize: 13,
                      color: "#3d3a5c",
                    }}
                  >
                    {item.description}
                  </p>

                  <div className="flex justify-between mt-4">
                    <small
                      style={{ color: "#a8a6c0" }}
                    >
                      {formatDate(item.createdAt)}
                    </small>

                    <button
                      type="button"
                      onClick={() =>
                        onOpenProject(item.projectId)
                      }
                      style={{
                        color: "#6366f1",
                        fontSize: 12,
                        fontWeight: 600,
                      }}
                    >
                      Open Project →
                    </button>
                  </div>
                </div>
              </div>
            </Card>
          ))}
      </div>
    </main>
  );
}

// ============================================================
// MILESTONES PAGE — LIVE BACKEND DATA
// ============================================================


function MilestonesPage({
  onOpenProject,
}: {
  onOpenProject: (id: number) => void;
}) {
  type MilestoneGroup = {
    project: StudentProject;
    milestones: MilestoneResponse[];
    error: string | null;
  };

  const [groups, setGroups] = useState<MilestoneGroup[]>([]);
  const [expanded, setExpanded] = useState<number[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [refreshKey, setRefreshKey] = useState(0);

  useEffect(() => {
    const controller = new AbortController();

    async function load() {
      setLoading(true);
      setError("");

      try {
        const projects = await getMyStudentProjects(
          controller.signal
        );

        const responses = await Promise.allSettled(
          projects.map((project) =>
            getAuthenticatedJson<MilestoneResponse[]>(
              `/api/projects/${project.projectId}/milestones`,
              controller.signal
            )
          )
        );

        if (controller.signal.aborted) return;

        const result: MilestoneGroup[] = projects.map(
          (project, index) => {
            const response = responses[index];

            if (
              response.status === "fulfilled" &&
              Array.isArray(response.value)
            ) {
              return {
                project,
                milestones: [...response.value].sort(
                  (a, b) => {
                    if (!a.deadline && !b.deadline) {
                      return a.milestoneId - b.milestoneId;
                    }
                    if (!a.deadline) return 1;
                    if (!b.deadline) return -1;

                    return (
                      a.deadline.localeCompare(b.deadline) ||
                      a.milestoneId - b.milestoneId
                    );
                  }
                ),
                error: null,
              };
            }

            return {
              project,
              milestones: [],
              error:
                response.status === "rejected" &&
                response.reason instanceof Error
                  ? response.reason.message
                  : "Unable to load this project's milestones.",
            };
          }
        );

        setGroups(result);

        // Keep all project groups collapsed on initial load.
        // Preserve the user's selections during refresh.
        setExpanded((previous) => previous);
      } catch (err) {
        if (!controller.signal.aborted) {
          setError(
            err instanceof Error
              ? err.message
              : "Unable to load project milestones."
          );
        }
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      }
    }

    load();

    return () => controller.abort();
  }, [refreshKey]);

  function toggleProject(projectId: number) {
    setExpanded((previous) =>
      previous.includes(projectId)
        ? previous.filter((id) => id !== projectId)
        : [...previous, projectId]
    );
  }

  const totalMilestones = groups.reduce(
    (total, group) => total + group.milestones.length,
    0
  );

  const completedMilestones = groups.reduce(
    (total, group) =>
      total +
      group.milestones.filter((milestone) =>
        isCompleted(milestone.status)
      ).length,
    0
  );

  const failedProjects = groups.filter(
    (group) => group.error !== null
  ).length;

  const labelStyle: React.CSSProperties = {
    color: "#6b6890",
    fontSize: 12,
  };

  return (
    <main
      className="flex-1 overflow-y-auto"
      style={{
        background: "#f7f6f3",
        minWidth: 0,
      }}
    >
      <Header
        title="Milestones"
        subtitle="Academic Timeline"
      />

      <div className="px-8 py-6 space-y-5">
        {/* PAGE OVERVIEW */}
        <Card>
          <div className="flex flex-wrap justify-between items-center gap-4">
            <div>
              <h2
                style={{
                  fontFamily: "var(--font-display)",
                  fontSize: 20,
                  color: "#1a1830",
                }}
              >
                Your Project Milestones
              </h2>

              <p
                style={{
                  ...labelStyle,
                  marginTop: 6,
                }}
              >
                Track milestones grouped by your academic
                projects.
              </p>
            </div>

            <button
              type="button"
              disabled={loading}
              onClick={() =>
                setRefreshKey((value) => value + 1)
              }
              className="rounded-xl px-4 py-2"
              style={{
                background: "#f0eff8",
                color: "#6366f1",
                fontSize: 12,
                fontWeight: 600,
                opacity: loading ? 0.6 : 1,
              }}
            >
              Refresh
            </button>
          </div>

          {!loading && !error && (
            <div className="grid grid-cols-3 gap-3 mt-5">
              {[
                {
                  label: "Projects",
                  value: groups.length,
                  color: "#6366f1",
                },
                {
                  label: "Milestones",
                  value: totalMilestones,
                  color: "#8b5cf6",
                },
                {
                  label: "Completed",
                  value: completedMilestones,
                  color: "#10b981",
                },
              ].map((item) => (
                <div
                  key={item.label}
                  className="rounded-xl p-3"
                  style={{
                    background: "#f7f6f3",
                    minWidth: 0,
                  }}
                >
                  <strong
                    style={{
                      display: "block",
                      fontSize: 23,
                      fontFamily: "var(--font-display)",
                      color: item.color,
                    }}
                  >
                    {item.value}
                  </strong>

                  <span style={labelStyle}>
                    {item.label}
                  </span>
                </div>
              ))}
            </div>
          )}
        </Card>

        {loading && (
          <Card>Loading project milestones...</Card>
        )}

        {error && (
          <Card>
            <p
              role="alert"
              style={{
                color: "#b91c1c",
                fontSize: 13,
              }}
            >
              {error}
            </p>

            <button
              type="button"
              onClick={() =>
                setRefreshKey((value) => value + 1)
              }
              style={{
                marginTop: 12,
                color: "#6366f1",
                fontWeight: 600,
                fontSize: 12,
              }}
            >
              Try Again
            </button>
          </Card>
        )}

        {!loading && !error && failedProjects > 0 && (
          <div
            role="alert"
            className="rounded-xl p-4"
            style={{
              background: "#fffbeb",
              border: "1px solid #fde68a",
              color: "#92400e",
              fontSize: 12,
            }}
          >
            Milestones could not be loaded for{" "}
            {failedProjects} project(s). Expand the project
            groups to see the errors.
          </div>
        )}

        {!loading && !error && groups.length === 0 && (
          <Card>
            <p style={labelStyle}>
              No academic projects are associated with your
              account yet.
            </p>
          </Card>
        )}

        {/* PROJECT-GROUPED MILESTONES */}
        {!loading &&
          !error &&
          groups.map((group) => {
            const projectId = group.project.projectId;
            const isOpen = expanded.includes(projectId);

            const completed = group.milestones.filter(
              (milestone) =>
                isCompleted(milestone.status)
            ).length;

            const total = group.milestones.length;

            return (
              <section
                key={projectId}
                className="rounded-2xl overflow-hidden"
                style={{
                  background: "white",
                  boxShadow:
                    "0 1px 4px rgba(26,24,48,0.06)",
                  border: "1px solid #e8e7f0",
                }}
              >
                {/* PROJECT GROUP HEADER */}
                <button
                  type="button"
                  aria-expanded={isOpen}
                  onClick={() => toggleProject(projectId)}
                  className="w-full text-left p-5"
                  style={{
                    background: isOpen
                      ? "#f8f7ff"
                      : "white",
                  }}
                >
                  <div className="flex justify-between items-center gap-4">
                    <div className="flex items-center gap-3 min-w-0">
                      <div
                        className="rounded-xl flex items-center justify-center"
                        style={{
                          width: 42,
                          height: 42,
                          flexShrink: 0,
                          background: "#eef2ff",
                          color: "#6366f1",
                        }}
                      >
                        <FlagIcon className="w-5 h-5" />
                      </div>

                      <div className="min-w-0">
                        <h3
                          style={{
                            fontFamily:
                              "var(--font-display)",
                            fontSize: 17,
                            fontWeight: 500,
                            color: "#1a1830",
                            overflowWrap: "anywhere",
                          }}
                        >
                          {group.project.projectTitle}
                        </h3>

                        <p
                          style={{
                            ...labelStyle,
                            marginTop: 4,
                          }}
                        >
                          {group.error
                            ? "Milestones unavailable"
                            : `${completed} of ${total} milestones completed`}
                        </p>
                      </div>
                    </div>

                    <div className="flex items-center gap-3 flex-shrink-0">
                      <Badge
                        color={
                          group.error
                            ? "#ef4444"
                            : "#6366f1"
                        }
                      >
                        {group.error
                          ? "Error"
                          : `${total} milestones`}
                      </Badge>

                      <span
                        style={{
                          fontSize: 18,
                          color: "#6366f1",
                        }}
                      >
                        {isOpen ? "⌃" : "⌄"}
                      </span>
                    </div>
                  </div>

                  {!group.error && total > 0 && (
                    <div className="mt-4">
                      <div
                        className="rounded-full overflow-hidden"
                        style={{
                          height: 5,
                          background: "#e8e7f0",
                        }}
                      >
                        <div
                          style={{
                            height: "100%",
                            width: `${
                              (completed / total) * 100
                            }%`,
                            background: "#6366f1",
                          }}
                        />
                      </div>
                    </div>
                  )}
                </button>

                {/* EXPANDED PROJECT CONTENT */}
                {isOpen && (
                  <div
                    className="p-5 space-y-3"
                    style={{
                      borderTop:
                        "1px solid #e8e7f0",
                    }}
                  >
                    {group.error && (
                      <p
                        role="alert"
                        style={{
                          color: "#b91c1c",
                          fontSize: 12,
                        }}
                      >
                        {group.error}
                      </p>
                    )}

                    {!group.error && total === 0 && (
                      <p style={labelStyle}>
                        No milestones have been created for
                        this project yet.
                      </p>
                    )}

                    {group.milestones.map(
                      (milestone, index) => {
                        const progress = safeProgress(
                          milestone.progress
                        );

                        const color = statusColor(
                          milestone.status
                        );

                        return (
                          <div
                            key={milestone.milestoneId}
                            className="rounded-xl p-4"
                            style={{
                              background: "#f7f6f3",
                              border:
                                "1px solid #e8e7f0",
                            }}
                          >
                            <div className="flex items-start justify-between gap-4">
                              <div className="flex gap-3 min-w-0">
                                <div
                                  className="rounded-lg flex items-center justify-center"
                                  style={{
                                    width: 30,
                                    height: 30,
                                    flexShrink: 0,
                                    background:
                                      "#eef2ff",
                                    color: "#6366f1",
                                    fontSize: 12,
                                    fontWeight: 700,
                                  }}
                                >
                                  {index + 1}
                                </div>

                                <div className="min-w-0">
                                  <h4
                                    style={{
                                      fontSize: 14,
                                      fontWeight: 600,
                                      color: "#1a1830",
                                    }}
                                  >
                                    {milestone.title}
                                  </h4>

                                  <p
                                    style={{
                                      ...labelStyle,
                                      marginTop: 7,
                                      lineHeight: 1.6,
                                      whiteSpace: "pre-wrap",
                                    }}
                                  >
                                    {milestone.description ||
                                      "No description provided."}
                                  </p>
                                </div>
                              </div>

                              <Badge color={color}>
                                {formatStatus(
                                  milestone.status
                                )}
                              </Badge>
                            </div>

                            <div className="flex flex-wrap justify-between items-center gap-3 mt-4">
                              <span style={labelStyle}>
                                Deadline:{" "}
                                <strong
                                  style={{
                                    color: "#1a1830",
                                  }}
                                >
                                  {formatDate(
                                    milestone.deadline
                                  )}
                                </strong>
                              </span>

                              <strong
                                style={{
                                  color,
                                  fontSize: 12,
                                }}
                              >
                                {progress}% complete
                              </strong>
                            </div>

                            <div
                              className="rounded-full overflow-hidden mt-2"
                              style={{
                                height: 6,
                                background: "#e8e7f0",
                              }}
                            >
                              <div
                                style={{
                                  width: `${progress}%`,
                                  height: "100%",
                                  background: color,
                                }}
                              />
                            </div>

                            <div
                              className="flex justify-end mt-4 pt-3"
                              style={{
                                borderTop:
                                  "1px solid #e8e7f0",
                              }}
                            >
                              <button
                                type="button"
                                onClick={() =>
                                  onOpenProject(projectId)
                                }
                                style={{
                                  color: "#6366f1",
                                  fontSize: 12,
                                  fontWeight: 600,
                                }}
                              >
                                Open Project →
                              </button>
                            </div>
                          </div>
                        );
                      }
                    )}

                    <div className="flex justify-end pt-2">
                      <button
                        type="button"
                        onClick={() =>
                          onOpenProject(projectId)
                        }
                        className="rounded-xl px-4 py-2.5"
                        style={{
                          background: "#f0eff8",
                          color: "#6366f1",
                          fontWeight: 600,
                          fontSize: 12,
                        }}
                      >
                        View Project Details →
                      </button>
                    </div>
                  </div>
                )}
              </section>
            );
          })}
      </div>
    </main>
  );
}


// ============================================================
// OTHER SCREENS
// ============================================================

function NotificationsPage({
  onOpenProject,
}: {
  onOpenProject: (id: number) => void;
}) {
  const [items, setItems] =
    useState<DashboardActivity[]>([]);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const controller = new AbortController();

    async function load() {
      try {
        const projects =
          await getMyStudentProjects(controller.signal);

        const responses = await Promise.allSettled(
          projects.map((project) =>
            getProjectDetails(
              project.projectId,
              controller.signal
            )
          )
        );

        const activities: DashboardActivity[] = [];

        responses.forEach((response, index) => {
          if (response.status !== "fulfilled") return;

          for (
            const activity of
            response.value.recentActivitiesCard || []
          ) {
            if (!activity.activityDescription) continue;

            activities.push({
              projectId: projects[index].projectId,
              projectTitle:
                projects[index].projectTitle,
              description:
                activity.activityDescription,
              createdAt:
                activity.activityCreatedTime,
            });
          }
        });

        activities.sort((a, b) =>
          (b.createdAt || "").localeCompare(
            a.createdAt || ""
          )
        );

        if (!controller.signal.aborted) {
          setItems(activities);
        }
      } catch (err) {
        if (!controller.signal.aborted) {
          setError(
            err instanceof Error
              ? err.message
              : "Unable to load activity."
          );
        }
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      }
    }

    load();

    return () => controller.abort();
  }, []);

  return (
    <main
      className="flex-1 overflow-y-auto"
      style={{ background: "#f7f6f3" }}
    >
      <Header
        title="Recent Activity"
        subtitle="Project Updates"
      />

      <div className="px-8 py-6 space-y-4">
        <Card>
          <h2
            style={{
              fontFamily: "var(--font-display)",
              fontSize: 20,
            }}
          >
            Activity Across Your Projects
          </h2>

          <p
            style={{
              color: "#6b6890",
              fontSize: 12,
              marginTop: 6,
            }}
          >
            Recent project activity. Notification
            delivery and read states are not yet
            supported by this screen.
          </p>
        </Card>

        {loading && <Card>Loading activity...</Card>}

        {error && (
          <Card>
            <p role="alert" style={{ color: "#b91c1c" }}>
              {error}
            </p>
          </Card>
        )}

        {!loading &&
          !error &&
          items.length === 0 && (
            <Card>No recent activity available.</Card>
          )}

        {!loading &&
          items.map((item, index) => (
            <Card
              key={`${item.projectId}-${index}`}
            >
              <div className="flex justify-between gap-4">
                <div>
                  <b style={{ fontSize: 13 }}>
                    {item.description}
                  </b>

                  <p
                    style={{
                      fontSize: 11,
                      color: "#6b6890",
                      marginTop: 6,
                    }}
                  >
                    {item.projectTitle} ·{" "}
                    {formatDate(item.createdAt)}
                  </p>
                </div>

                <button
                  type="button"
                  onClick={() =>
                    onOpenProject(item.projectId)
                  }
                  style={{
                    color: "#6366f1",
                    fontSize: 12,
                  }}
                >
                  Open →
                </button>
              </div>
            </Card>
          ))}
      </div>
    </main>
  );
}

function GenericPage({
  title,
  subtitle,
}: {
  title: string;
  subtitle: string;
}) {
  return (
    <main className="flex-1 overflow-y-auto">
      <Header title={title} subtitle={subtitle} />

      <div className="px-8 py-6">
        <Card>
          <div className="text-center py-16">
            <h2
              style={{
                fontFamily: "var(--font-display)",
                fontSize: 21,
              }}
            >
              Ready for the next module
            </h2>

            <p
              style={{
                fontSize: 13,
                color: "#6b6890",
                marginTop: 8,
              }}
            >
              This module is ready for its
              next backend integration.
            </p>
          </div>
        </Card>
      </div>
    </main>
  );
}

// ============================================================
// STUDENT ROUTING
// ============================================================

function StudentApp() {
  const [activeNav, setActiveNav] =
    useState("dashboard");

  const [invitationRevision, setInvitationRevision] =
    useState(0);

  useEffect(() => {
    function handleInvitationAccepted() {
      setInvitationRevision((value) => value + 1);
    }

    window.addEventListener(
      "academiq:invitation-accepted",
      handleInvitationAccepted
    );

    return () => {
      window.removeEventListener(
        "academiq:invitation-accepted",
        handleInvitationAccepted
      );
    };
  }, []);

  const [page, setPage] = useState<
    "dashboard" | "project-details"
  >("dashboard");

  const [projectContext, setProjectContext] =
    useState("");

  const [workspaceAction, setWorkspaceAction] =
    useState("");

  function navigate(id: string) {
    setActiveNav(id);
    setWorkspaceAction("");
    setPage("dashboard");
  }

  function openProject(id: number) {
    if (!Number.isInteger(id) || id <= 0) {
      console.error("Invalid project ID:", id);
      return;
    }

    setProjectContext(String(id));
    setActiveNav("projects");
    setPage("project-details");
  }

  function goFromProject(
    target: string,
    projectId: string,
    action = ""
  ) {
    navigate(target);
    setProjectContext(projectId);
    setWorkspaceAction(action);
  }

  const content =
    page === "project-details" ? (
      <ProjectDetails
        projectId={Number(projectContext)}
        onBack={() => {
          setPage("dashboard");
          setActiveNav("projects");
        }}
        onQuickAction={(target, action) =>
          goFromProject(
            target,
            projectContext,
            action
          )
        }
      />
    
        ) : activeNav === "dashboard" ? (
          <DashboardPage
            key={invitationRevision}
            onNavigate={navigate}
            onOpenProject={openProject}
          />
        ) : activeNav === "projects" ? (
          <StudentProjects
            key={invitationRevision}
            onOpen={(id) =>
              openProject(Number(id))
            }
          />

    ) : activeNav === "tasks" ? (
      <TasksWorkspace
        initialProject={projectContext}
        initialAction={workspaceAction}
        onActionConsumed={() =>
          setWorkspaceAction("")
        }
        onOpenProject={openProject}
      />
    ) : activeNav === "milestones" ? (
      <MilestonesPage
        onOpenProject={openProject}
      />
    ) : activeNav === "submissions" ? (
      <SubmissionsWorkspace
        initialProject={projectContext}
        initialAction={workspaceAction}
        onActionConsumed={() =>
          setWorkspaceAction("")
        }
      />
    ) : activeNav === "feedback" ? (
      <FeedbackPage onOpenProject={openProject} />
    ) : activeNav === "team" ? (
      <TeamWorkspace
        initialProject={projectContext}
        initialAction={workspaceAction}
        onActionConsumed={() =>
          setWorkspaceAction("")
        }
      />
    ) : activeNav === "discussion" ? (
      <DiscussionWorkspace
        initialProject={projectContext}
      />
    ) : activeNav === "notifications" ? (
      <NotificationsPage
        onOpenProject={openProject}
      />
    ) : (
      <GenericPage
        title={
          activeNav.charAt(0).toUpperCase() +
          activeNav.slice(1)
        }
        subtitle="AcademiQ"
      />
    );

  return (
    <div
      className="flex h-full"
      style={{
        fontFamily: "var(--font-sans)",
      }}
    >
      <Sidebar
        active={activeNav}
        setActive={navigate}
      />

      {content}
    </div>
  );
}

// ============================================================
// SUPERVISOR ROUTING — UNCHANGED
// ============================================================

export default function PortalApp() {
  const isSupervisor =
    window.location.pathname.startsWith(
      "/supervisor"
    );

  const [supervisorPage, setSupervisorPage] =
    useState("dashboard");

  function go(page: string) {
    setSupervisorPage(page);
  }

  if (!isSupervisor) {
    return <StudentApp />;
  }

  const content =
    supervisorPage === "dashboard" ? (
      <SupervisorDashboard go={go} />
    ) : supervisorPage === "projects" ? (
      <SupervisorProjects />
    ) : supervisorPage === "reviews" ? (
      <ReviewQueue />
    ) : supervisorPage === "calendar" ? (
      <SupervisorCalendar />
    ) : supervisorPage === "feedback" ? (
      <SupervisorFeedback />
    ) : (
      <GenericPage
        title={
          supervisorPage.charAt(0).toUpperCase() +
          supervisorPage.slice(1)
        }
        subtitle="Supervisor Portal"
      />
    );

  return (
    <div className="flex h-full">
      <Sidebar
        active={supervisorPage}
        setActive={go}
        role="supervisor"
      />

      {content}
    </div>
  );
}
