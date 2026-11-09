
import { useEffect, useMemo, useState } from "react";
import type {
  CSSProperties,
  FormEvent,
  ReactNode,
} from "react";

import {
  getProjectDetails,
  type ProjectDetailsResponse,
} from "./api/projectDetails";

import {
  PlusIcon,
  UploadIcon,
  ChatIcon,
  BellIcon,
  FlagIcon,
  UserPlusIcon,
  ClockIcon,
} from "./icons";

import ProjectSubmissionHistory from "./ProjectSubmissionHistory";
import ProjectTaskList from "./ProjectTaskList";
import ProjectInvitationNotifications from "./ProjectInvitationNotifications";

// ============================================================
// TYPES
// ============================================================

interface ProjectDetailsProps {
  projectId: number;
  onBack: () => void;
  onQuickAction?: (target: string, action?: string) => void;
}

interface Milestone {
  milestoneId: number;
  title: string;
  description: string | null;
  deadline: string | null;
  status: string;
  progress: number | null;
}

interface EligibleStudent {
  userId: number;
  fullName: string;
  email: string;
}

interface TeamMember {
  memberId: number;
  userId: number;
  fullName: string;
  role: string;
}

interface TeamSummary {
  projectId: number;
  memberCount: number;
  maxMembers: number;
  pendingInvitationCount: number;
  availableSlots: number;
  isLeader: boolean;
  canInvite: boolean;
  members: TeamMember[];
}

interface TaskForm {
  title: string;
  description: string;
  priority: string;
  deadline: string;
  milestoneId: number;
  memberId: number;
}

type TabName =
  | "Overview"
  | "Tasks"
  | "Milestones"
  | "Submissions"
  | "Documents"
  | "Team";

type QuickAction =
  | "task"
  | "milestone"
  | "document"
  | "invite"
  | "chat";

const TABS: TabName[] = [
  "Overview",
  "Tasks",
  "Milestones",
  "Submissions",
  "Documents",
  "Team",
];

const EMPTY_TASK: TaskForm = {
  title: "",
  description: "",
  priority: "MEDIUM",
  deadline: "",
  milestoneId: 0,
  memberId: 0,
};

// ============================================================
// STYLES AND HELPERS
// ============================================================

const panelStyle: CSSProperties = {
  background: "white",
  boxShadow: "0 1px 4px rgba(26,24,48,.06)",
};

const mutedText: CSSProperties = {
  color: "#6b6890",
  fontSize: 12.5,
  lineHeight: 1.65,
};

const lightButton: CSSProperties = {
  background: "#f0eff8",
  color: "#6366f1",
  padding: "10px 14px",
  borderRadius: 10,
  fontSize: 12,
  fontWeight: 600,
};

const primaryButton: CSSProperties = {
  background: "linear-gradient(135deg,#6366f1,#8b5cf6)",
  color: "white",
  padding: "10px 16px",
  borderRadius: 10,
  fontSize: 12,
  fontWeight: 600,
};

const inputStyle: CSSProperties = {
  width: "100%",
  padding: 12,
  border: "1px solid #e8e7f0",
  borderRadius: 12,
  background: "white",
  marginTop: 5,
};

const gradients = [
  "#6366f1",
  "#06b6d4",
  "#10b981",
  "#ec4899",
];

function formatDate(value?: string | null) {
  if (!value) return "Not set";

  const date = new Date(value);

  return Number.isNaN(date.getTime())
    ? value
    : date.toLocaleDateString(undefined, {
        year: "numeric",
        month: "short",
        day: "numeric",
      });
}

function formatStatus(value?: string | null) {
  if (!value) return "Unknown";

  return value
    .toLowerCase()
    .replace(/_/g, " ")
    .replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function clamp(value?: number | null) {
  return typeof value === "number" &&
    Number.isFinite(value)
    ? Math.min(100, Math.max(0, value))
    : 0;
}

function isCompleted(status: string) {
  return ["COMPLETED", "APPROVED"].includes(
    status.toUpperCase()
  );
}

function statusColor(status: string) {
  switch (status.toUpperCase()) {
    case "COMPLETED":
    case "APPROVED":
      return "#10b981";
    case "IN_PROGRESS":
      return "#6366f1";
    case "UNDER_REVIEW":
      return "#f59e0b";
    case "REJECTED":
    case "REVISION_REQUIRED":
      return "#ef4444";
    default:
      return "#a8a6c0";
  }
}

function initialsOf(name: string) {
  const parts = name.trim().split(/\s+/).filter(Boolean);

  if (!parts.length) return "AQ";

  return parts.length === 1
    ? parts[0].slice(0, 2).toUpperCase()
    : (
        parts[0][0] +
        parts[parts.length - 1][0]
      ).toUpperCase();
}

function localDateToday() {
  const date = new Date();
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function Panel({
  children,
  className = "",
  style,
}: {
  children: ReactNode;
  className?: string;
  style?: CSSProperties;
}) {
  return (
    <div
      className={`rounded-2xl p-6 min-w-0 ${className}`}
      style={{ ...panelStyle, ...style }}
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
      className="inline-flex items-center rounded-lg px-2.5 py-1 text-xs font-semibold"
      style={{
        color,
        background: color + "18",
      }}
    >
      {children}
    </span>
  );
}

function Avatar({
  name,
  color = "#6366f1",
  size = 44,
}: {
  name: string;
  color?: string;
  size?: number;
}) {
  return (
    <div
      className="rounded-full flex items-center justify-center text-white font-semibold flex-shrink-0"
      style={{
        width: size,
        height: size,
        background: color,
        fontSize: size * 0.34,
      }}
    >
      {initialsOf(name)}
    </div>
  );
}

function ProgressBar({
  value,
  color = "#6366f1",
  bg = "#e8e7f0",
  height = 6,
}: {
  value: number;
  color?: string;
  bg?: string;
  height?: number;
}) {
  return (
    <div
      className="rounded-full w-full overflow-hidden"
      style={{ height, background: bg }}
    >
      <div
        className="h-full rounded-full"
        style={{
          width: `${clamp(value)}%`,
          background: color,
          transition: "width .4s ease",
        }}
      />
    </div>
  );
}

function Heading({
  title,
  subtitle,
  action,
}: {
  title: string;
  subtitle?: string;
  action?: ReactNode;
}) {
  return (
    <div className="flex justify-between items-start gap-3 flex-wrap mb-5">
      <div>
        <h3
          style={{
            fontFamily: "var(--font-display)",
            fontSize: 18,
            fontWeight: 500,
            color: "#1a1830",
          }}
        >
          {title}
        </h3>

        {subtitle && (
          <p style={{ ...mutedText, marginTop: 5 }}>
            {subtitle}
          </p>
        )}
      </div>
      {action}
    </div>
  );
}

function Metric({
  label,
  value,
  color = "#6366f1",
}: {
  label: string;
  value: number | string;
  color?: string;
}) {
  return (
    <div
      className="rounded-xl p-4"
      style={{
        background: "#f7f6f3",
        border: "1px solid #e8e7f0",
      }}
    >
      <p style={{ fontSize: 11.5, color: "#6b6890" }}>
        {label}
      </p>

      <strong
        style={{
          display: "block",
          marginTop: 8,
          fontFamily: "var(--font-display)",
          fontSize: 25,
          fontWeight: 500,
          color,
        }}
      >
        {value}
      </strong>
    </div>
  );
}

// ============================================================
// API
// ============================================================

async function requestApi<T>(
  url: string,
  options: RequestInit = {}
): Promise<T> {
  const stored = sessionStorage.getItem("academiq_auth");

  if (!stored) {
    throw new Error("Please sign in again.");
  }

  let token: unknown;

  try {
    token = JSON.parse(stored)?.token;
  } catch {
    throw new Error("Invalid login session.");
  }

  if (typeof token !== "string" || !token.trim()) {
    throw new Error("Authentication token is missing.");
  }

  const multipart = options.body instanceof FormData;

  const response = await fetch(url, {
    ...options,
    headers: {
      Authorization: `Bearer ${token}`,
      Accept: "application/json",
      ...(!multipart && options.body
        ? { "Content-Type": "application/json" }
        : {}),
      ...options.headers,
    },
  });

  if (!response.ok) {
    const body = await response.text();
    let message = `Request failed (HTTP ${response.status}).`;

    try {
      const data = JSON.parse(body);
      message = data.message || data.error || message;
    } catch {
      if (body && !body.trim().startsWith("<")) {
        message = body;
      }
    }

    throw new Error(message);
  }

  const text = await response.text();

  if (!text) return undefined as T;

  try {
    return JSON.parse(text) as T;
  } catch {
    return text as T;
  }
}

async function getMilestones(
  projectId: number,
  signal?: AbortSignal
) {
  return requestApi<Milestone[]>(
    `/api/projects/${projectId}/milestones`,
    { signal }
  );
}

async function getTeam(
  projectId: number,
  signal?: AbortSignal
) {
  const teams = await requestApi<TeamSummary[]>(
    "/team/my-project-teams",
    { signal }
  );

  return (
    teams.find((team) => team.projectId === projectId) ??
    null
  );
}

async function findStudents(
  projectId: number,
  query: string,
  signal?: AbortSignal
) {
  return requestApi<EligibleStudent[]>(
    `/team/projects/${projectId}/search?query=${encodeURIComponent(
      query
    )}`,
    { signal }
  );
}

// ============================================================
// MILESTONE TIMELINE
// ============================================================

function MilestoneTimeline({
  milestones,
  loading,
  error,
  retry,
  compact = false,
}: {
  milestones: Milestone[];
  loading: boolean;
  error: string;
  retry: () => void;
  compact?: boolean;
}) {
  if (loading) {
    return <p style={mutedText}>Loading milestones...</p>;
  }

  if (error) {
    return (
      <div role="alert">
        <p style={{ color: "#b91c1c", fontSize: 12.5 }}>
          {error}
        </p>

        <button
          type="button"
          onClick={retry}
          style={{ ...lightButton, marginTop: 12 }}
        >
          Retry
        </button>
      </div>
    );
  }

  if (!milestones.length) {
    return (
      <div
        className="rounded-xl p-5"
        style={{ background: "#f7f6f3" }}
      >
        <p style={mutedText}>
          No milestones have been created for this
          project yet.
        </p>
      </div>
    );
  }

  return (
    <div className="flex flex-col">
      {milestones.map((item, index) => {
        const completed = isCompleted(item.status);
        const color = statusColor(item.status);
        const progress = clamp(item.progress);
        const last = index === milestones.length - 1;

        return (
          <div
            key={item.milestoneId}
            className="aq-milestone-row"
            style={{
              display: "flex",
              alignItems: "stretch",
              gap: 15,
            }}
          >
            <div
              className="flex flex-col items-center"
              style={{ width: 20, flexShrink: 0 }}
            >
              <div
                className="rounded-full flex items-center justify-center"
                style={{
                  width: 20,
                  height: 20,
                  border: `3px solid ${color}`,
                  background: completed ? color : "white",
                  flexShrink: 0,
                }}
              >
                {completed && (
                  <svg
                    width="12"
                    height="12"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="white"
                    strokeWidth="3"
                  >
                    <path
                      d="M5 12l5 5 9-9"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                    />
                  </svg>
                )}
              </div>

              {!last && (
                <div
                  style={{
                    width: 2,
                    flex: 1,
                    minHeight: 28,
                    margin: "3px 0",
                    background: completed
                      ? "#10b981"
                      : "#e8e7f0",
                  }}
                />
              )}
            </div>

            <div
              className="aq-milestone-card"
              style={{
                flex: 1,
                minWidth: 0,
                paddingBottom: last ? 0 : 22,
              }}
            >
              <div className="flex justify-between items-start gap-3 flex-wrap">
                <div className="flex-1 min-w-0">
                  <div className="flex gap-2 items-center flex-wrap">
                    <strong
                      style={{
                        fontSize: 13.5,
                        color: "#1a1830",
                      }}
                    >
                      {item.title}
                    </strong>

                    <Badge color={color}>
                      {formatStatus(item.status)}
                    </Badge>
                  </div>

                  {item.description && (
                    <p
                      style={{
                        ...mutedText,
                        marginTop: 6,
                      }}
                    >
                      {item.description}
                    </p>
                  )}
                </div>

                <span
                  style={{
                    fontSize: 11,
                    color: "#6b6890",
                  }}
                >
                  {formatDate(item.deadline)}
                </span>
              </div>

              <div
                className="flex justify-between mt-3 mb-2"
                style={{
                  fontSize: 11,
                  color: "#6b6890",
                }}
              >
                <span>Progress</span>
                <strong style={{ color }}>
                  {progress}%
                </strong>
              </div>

              <ProgressBar
                value={progress}
                color={color}
                height={compact ? 5 : 7}
              />

              {!compact && (
                <p
                  style={{
                    fontSize: 11,
                    marginTop: 8,
                    color: "#a8a6c0",
                  }}
                >
                  Milestone #{item.milestoneId}
                </p>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
}

// ============================================================
// MAIN PAGE
// ============================================================

export default function ProjectDetails({
  projectId,
  onBack,
  onQuickAction,
}: ProjectDetailsProps) {
  const [activeTab, setActiveTab] =
    useState<TabName>("Overview");

  const [project, setProject] =
    useState<ProjectDetailsResponse | null>(null);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [projectReload, setProjectReload] = useState(0);
  const [taskRefreshKey, setTaskRefreshKey] = useState(0);

  const [milestones, setMilestones] =
    useState<Milestone[]>([]);

  const [milestonesLoading, setMilestonesLoading] =
    useState(true);

  const [milestonesError, setMilestonesError] =
    useState("");


  const [milestoneReload, setMilestoneReload] =
    useState(0);

  // Refresh submission history after a successful PDF upload
  const [submissionRefreshKey, setSubmissionRefreshKey] =
    useState(0);

  const [team, setTeam] =
    useState<TeamSummary | null>(null);


  const [teamLoading, setTeamLoading] = useState(true);
  const [teamError, setTeamError] = useState("");
  const [teamReload, setTeamReload] = useState(0);

  const [quickAction, setQuickAction] =
    useState<QuickAction | null>(null);

  const [taskForm, setTaskForm] =
    useState<TaskForm>({ ...EMPTY_TASK });

  const [submissionMilestoneId, setSubmissionMilestoneId] =
    useState(0);

  const [submissionFile, setSubmissionFile] =
    useState<File | null>(null);

  const [studentQuery, setStudentQuery] = useState("");
  const [eligibleStudents, setEligibleStudents] =
    useState<EligibleStudent[]>([]);

  const [selectedStudent, setSelectedStudent] =
    useState<EligibleStudent | null>(null);

  const [searchingStudents, setSearchingStudents] =
    useState(false);

  const [searchError, setSearchError] = useState("");
  const [formError, setFormError] = useState("");
  const [formSuccess, setFormSuccess] = useState("");
  const [saving, setSaving] = useState(false);

  // ----------------------------------------------------------
  // ALL HOOKS ARE ABOVE CONDITIONAL RETURNS
  // ----------------------------------------------------------

  useEffect(() => {
    const controller = new AbortController();

    // Preserve the existing page during a background refresh.
    if (!project) setLoading(true);

    setError("");

    getProjectDetails(projectId, controller.signal)
      .then((data) => {
        if (!controller.signal.aborted) {
          setProject(data);
        }
      })
      .catch((err) => {
        if (!controller.signal.aborted) {
          setError(
            err instanceof Error
              ? err.message
              : "Unable to load project."
          );
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      });

    return () => controller.abort();
    // project is intentionally not a dependency.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [projectId, projectReload]);

  useEffect(() => {
    const controller = new AbortController();

    setMilestonesLoading(true);
    setMilestonesError("");

    getMilestones(projectId, controller.signal)
      .then((data) => {
        if (!controller.signal.aborted) {
          setMilestones(data);
        }
      })
      .catch((err) => {
        if (!controller.signal.aborted) {
          setMilestonesError(
            err instanceof Error
              ? err.message
              : "Unable to load milestones."
          );
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setMilestonesLoading(false);
        }
      });

    return () => controller.abort();
  }, [projectId, milestoneReload]);

  useEffect(() => {
    const controller = new AbortController();

    setTeamLoading(true);
    setTeamError("");

    getTeam(projectId, controller.signal)
      .then((data) => {
        if (!controller.signal.aborted) {
          setTeam(data);
        }
      })
      .catch((err) => {
        if (!controller.signal.aborted) {
          setTeam(null);
          setTeamError(
            err instanceof Error
              ? err.message
              : "Unable to load team permissions."
          );
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setTeamLoading(false);
        }
      });

    return () => controller.abort();
  }, [projectId, teamReload]);

  useEffect(() => {
    if (
      quickAction !== "invite" ||
      !team?.canInvite ||
      studentQuery.trim().length < 2
    ) {
      setEligibleStudents([]);
      setSearchError("");
      setSearchingStudents(false);
      return;
    }

    const controller = new AbortController();

    setSearchingStudents(true);
    setSearchError("");

    const timeout = window.setTimeout(() => {
      findStudents(
        projectId,
        studentQuery.trim(),
        controller.signal
      )
        .then((data) => {
          if (!controller.signal.aborted) {
            setEligibleStudents(data);
          }
        })
        .catch((err) => {
          if (!controller.signal.aborted) {
            setEligibleStudents([]);
            setSearchError(
              err instanceof Error
                ? err.message
                : "Student search failed."
            );
          }
        })
        .finally(() => {
          if (!controller.signal.aborted) {
            setSearchingStudents(false);
          }
        });
    }, 350);

    return () => {
      window.clearTimeout(timeout);
      controller.abort();
    };
  }, [
    quickAction,
    studentQuery,
    projectId,
    team?.canInvite,
  ]);

  const sortedMilestones = useMemo(() => {
    return [...milestones].sort((a, b) => {
      if (!a.deadline && !b.deadline) {
        return a.milestoneId - b.milestoneId;
      }

      if (!a.deadline) return 1;
      if (!b.deadline) return -1;

      return (
        a.deadline.localeCompare(b.deadline) ||
        a.milestoneId - b.milestoneId
      );
    });
  }, [milestones]);

  const upcomingMilestone = useMemo(() => {
    return (
      sortedMilestones.find(
        (item) =>
          !isCompleted(item.status) &&
          Boolean(item.deadline)
      ) ?? null
    );
  }, [sortedMilestones]);

  function showQuickAction(action: QuickAction) {
    setQuickAction(action);
    setFormError("");
    setFormSuccess("");
    setStudentQuery("");
    setSelectedStudent(null);
    setEligibleStudents([]);
    setSearchError("");
    setSubmissionFile(null);

    setSubmissionMilestoneId(
      sortedMilestones[0]?.milestoneId ?? 0
    );

    setTaskForm({
      ...EMPTY_TASK,
      milestoneId:
        sortedMilestones[0]?.milestoneId ?? 0,
      memberId: team?.members?.[0]?.memberId ?? 0,
    });
  }

  function closeQuickAction() {
    if (saving) return;

    setQuickAction(null);
    setFormError("");
    setFormSuccess("");
  }

  function openWorkspace(
    target: string,
    action = ""
  ) {
    onQuickAction?.(target, action);
  }

  async function submitQuickAction(
    event: FormEvent<HTMLFormElement>
  ) {
    event.preventDefault();

    if (!quickAction || quickAction === "chat") {
      return;
    }

    setSaving(true);
    setFormError("");
    setFormSuccess("");

    try {
      if (quickAction === "invite") {
        if (!team?.canInvite) {
          throw new Error(
            "Invitations are not available for this project."
          );
        }

        if (!selectedStudent) {
          throw new Error(
            "Please select a student to invite."
          );
        }

        await requestApi<number>(
          "/invitation/inviteMember",
          {
            method: "POST",
            body: JSON.stringify({
              invitedUserId: selectedStudent.userId,
              projectId,
            }),
          }
        );

        setFormSuccess(
          `Invitation sent to ${selectedStudent.fullName}.`
        );

        setTeamReload((value) => value + 1);
        setSelectedStudent(null);
        setStudentQuery("");
        setEligibleStudents([]);
      }

      if (quickAction === "task") {
        if (
          !taskForm.title.trim() ||
          !taskForm.description.trim() ||
          !taskForm.deadline ||
          !taskForm.milestoneId ||
          !taskForm.memberId
        ) {
          throw new Error(
            "Please complete every task field."
          );
        }

        await requestApi<unknown>(
          `/api/projects/${projectId}/tasks`,
          {
            method: "POST",
            body: JSON.stringify({
              title: taskForm.title.trim(),
              description: taskForm.description.trim(),
              priority: taskForm.priority,
              deadline: taskForm.deadline,
              milestoneId: taskForm.milestoneId,
              memberId: taskForm.memberId,
            }),
          }
        );

        setFormSuccess("Task created successfully.");
        setProjectReload((value) => value + 1);
        setTaskRefreshKey((value) => value + 1);
      }

      if (
        quickAction === "milestone" ||
        quickAction === "document"
      ) {
        if (!submissionMilestoneId) {
          throw new Error("Please select a milestone.");
        }

        if (!submissionFile) {
          throw new Error("Please select a PDF file.");
        }

        if (
          submissionFile.type !== "application/pdf" &&
          !submissionFile.name.toLowerCase().endsWith(".pdf")
        ) {
          throw new Error("Only PDF files are accepted.");
        }

        const form = new FormData();
        form.append("file", submissionFile);

        await requestApi<unknown>(
          `/submissions/students/projects/${projectId}/milestones/${submissionMilestoneId}`,
          {
            method: "POST",
            body: form,
          }
        );

        setFormSuccess(
          "Milestone PDF submitted successfully."
        );

        setSubmissionFile(null);

        setSubmissionRefreshKey((value) => value + 1);
        setProjectReload((value) => value + 1);
        setMilestoneReload((value) => value + 1);
      }
    } catch (err) {
      setFormError(
        err instanceof Error
          ? err.message
          : "Unable to complete this action."
      );
    } finally {
      setSaving(false);
    }
  }

  // ==========================================================
  // LOADING / ERROR
  // ==========================================================

  if (loading && !project) {
    return (
      <div
        className="flex-1 p-8"
        style={{ background: "#f7f6f3" }}
      >
        <button
          type="button"
          onClick={onBack}
          style={lightButton}
        >
          ← Back to Projects
        </button>

        <Panel style={{ marginTop: 24 }}>
          Loading project details...
        </Panel>
      </div>
    );
  }

  if (!project) {
    return (
      <div
        className="flex-1 p-8"
        style={{ background: "#f7f6f3" }}
      >
        <button
          type="button"
          onClick={onBack}
          style={lightButton}
        >
          ← Back to Projects
        </button>

        <Panel style={{ marginTop: 24 }}>
          <p role="alert" style={{ color: "#b91c1c" }}>
            {error || "Project unavailable."}
          </p>

          <button
            type="button"
            onClick={() =>
              setProjectReload((value) => value + 1)
            }
            style={{
              ...lightButton,
              marginTop: 16,
            }}
          >
            Retry
          </button>
        </Panel>
      </div>
    );
  }

  // ==========================================================
  // DISPLAY DATA
  // ==========================================================

  const progress = clamp(project.overallProgress);
  const kpi = project.projectKPICards;

  const totalTasks = kpi?.totalTasks ?? 0;
  const completedTasks = kpi?.completedTasks ?? 0;
  const remainingTasks = Math.max(
    0,
    totalTasks - completedTasks
  );

  const taskProgress = totalTasks
    ? Math.round(
        (completedTasks / totalTasks) * 100
      )
    : 0;

  const totalMilestones =
    milestonesLoading || milestonesError
      ? kpi?.totalMilestones ?? 0
      : milestones.length;

  const completedMilestones =
    milestonesLoading || milestonesError
      ? kpi?.completedMilestones ?? 0
      : milestones.filter((item) =>
          isCompleted(item.status)
        ).length;

  const remainingMilestones = Math.max(
    0,
    totalMilestones - completedMilestones
  );

  const milestoneProgress = totalMilestones
    ? Math.round(
        (completedMilestones / totalMilestones) * 100
      )
    : 0;

  const teamMembers = project.teamMembers ?? [];
  const activities = project.recentActivitiesCard ?? [];
  const feedback = project.feedbackCard;

  const nextDeadline = upcomingMilestone
    ? formatDate(upcomingMilestone.deadline)
    : "Not scheduled";

  const stats = [
    {
      label: "Overall Progress",
      value: `${progress}%`,
      sub: "Project progress",
      color: "#6366f1",
      bar: progress,
    },
    {
      label: "Tasks Completed",
      value: `${completedTasks}/${totalTasks}`,
      sub: `${remainingTasks} remaining`,
      color: "#8b5cf6",
      bar: taskProgress,
    },
    {
      label: "Milestones Done",
      value: `${completedMilestones}/${totalMilestones}`,
      sub: "Completed milestones",
      color: "#10b981",
      bar: milestoneProgress,
    },
    {
      label: "Created",
      value: formatDate(project.createdAt),
      sub: "Creation date",
      color: "#f59e0b",
      bar: null as number | null,
    },
    {
      label: "Current Status",
      value: formatStatus(project.projectStatus),
      sub: "Project status",
      color: "#6366f1",
      bar: null as number | null,
    },
  ];

  const actionButtons: {
    label: string;
    icon: ReactNode;
    action: QuickAction;
  }[] = [
    {
      label: "Create Task",
      icon: <PlusIcon />,
      action: "task",
    },
    {
      label: "Submit Milestone",
      icon: <FlagIcon />,
      action: "milestone",
    },
    {
      label: "Upload Document",
      icon: <UploadIcon />,
      action: "document",
    },
    {
      label: "Invite Member",
      icon: <UserPlusIcon />,
      action: "invite",
    },
    {
      label: "Open Project Chat",
      icon: <ChatIcon />,
      action: "chat",
    },
  ];

  function TeamCards({ compact = false }: {
    compact?: boolean;
  }) {
    if (teamMembers.length === 0) {
      return <p style={mutedText}>No team members found.</p>;
    }

    return (
      <div
        className={
          compact
            ? "grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4"
            : "grid grid-cols-1 sm:grid-cols-2 gap-4"
        }
      >
        {teamMembers.map((member, index) => (
          <div
            key={`${member.fullName}-${index}`}
            className={
              compact
                ? "flex flex-col items-center text-center rounded-2xl p-5"
                : "rounded-xl p-5"
            }
            style={{
              background: "#f7f6f3",
              border: "1px solid #e8e7f0",
            }}
          >
            <div
              className={
                compact
                  ? "flex flex-col items-center"
                  : "flex items-center gap-3"
              }
            >
              <Avatar
                name={member.fullName}
                color={gradients[index % gradients.length]}
                size={compact ? 52 : 46}
              />

              <div
                style={{
                  marginTop: compact ? 10 : 0,
                }}
              >
                <strong
                  style={{
                    fontSize: 13.5,
                    color: "#1a1830",
                  }}
                >
                  {member.fullName}
                </strong>

                <p
                  style={{
                    color: "#6b6890",
                    fontSize: 11.5,
                    marginTop: 3,
                  }}
                >
                  {member.role}
                </p>
              </div>
            </div>

            <div
              className={
                compact
                  ? "flex flex-col items-center gap-2 mt-3"
                  : "flex justify-between items-center mt-5 gap-2"
              }
            >
              <Badge>{member.role}</Badge>

              <span
                style={{
                  fontSize: 11,
                  color: "#a8a6c0",
                }}
              >
                {member.taskedAssignedNumber ?? 0} tasks
                assigned
              </span>
            </div>
          </div>
        ))}
      </div>
    );
  }

  function DeadlineCard() {
    return (
      <div
        className="rounded-2xl overflow-hidden"
        style={{
          boxShadow: "0 4px 20px rgba(99,102,241,.18)",
        }}
      >
        <div
          style={{
            background:
              "linear-gradient(135deg,#4f46e5,#7c3aed)",
            padding: "20px 22px",
            color: "white",
          }}
        >
          <p
            style={{
              fontSize: 10,
              opacity: 0.75,
              textTransform: "uppercase",
            }}
          >
            Upcoming Deadline
          </p>

          <h3
            style={{
              fontFamily: "var(--font-display)",
              fontSize: 18,
              marginTop: 8,
            }}
          >
            {milestonesLoading
              ? "Loading..."
              : milestonesError
              ? "Unavailable"
              : upcomingMilestone?.title ||
                "No upcoming milestone"}
          </h3>

          <p
            style={{
              marginTop: 14,
              opacity: 0.85,
              fontSize: 13,
            }}
          >
            Due:{" "}
            {milestonesError ? "Unavailable" : nextDeadline}
          </p>

          <div className="mt-4">
            <ProgressBar
              value={clamp(upcomingMilestone?.progress)}
              color="#a5f3fc"
              bg="rgba(255,255,255,.2)"
            />
          </div>
        </div>

        <div
          className="flex items-center justify-between p-3"
          style={{ background: "white" }}
        >
          <span style={mutedText}>
            {upcomingMilestone
              ? formatStatus(upcomingMilestone.status)
              : "No pending deadline"}
          </span>

          <button
            type="button"
            onClick={() => setActiveTab("Milestones")}
            style={lightButton}
          >
            View Details
          </button>
        </div>
      </div>
    );
  }

  // ==========================================================
  // MAIN JSX
  // ==========================================================

  return (
    <div
      className="flex flex-col h-full min-w-0"
      style={{ background: "#f7f6f3" }}
    >
      {/* TOP BAR */}
      <header
        className="flex items-center justify-between px-8 py-4 flex-shrink-0"
        style={{
          background: "rgba(247,246,243,.92)",
          backdropFilter: "blur(12px)",
          borderBottom: "1px solid #e8e7f0",
          position: "sticky",
          top: 0,
          zIndex: 20,
        }}
      >
        <div className="flex items-center gap-3 min-w-0">
          <button
            type="button"
            onClick={onBack}
            className="rounded-lg px-3 py-1.5 flex-shrink-0"
            style={{
              background: "white",
              border: "1px solid #e8e7f0",
              color: "#6b6890",
              fontSize: 12.5,
            }}
          >
            ← Projects
          </button>

          <span style={{ color: "#c7d2fe" }}>/</span>

          <span
            className="truncate"
            style={{
              fontSize: 13,
              fontWeight: 600,
              color: "#1a1830",
            }}
          >
            {project.projectTitle}
          </span>
        </div>

        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={() => showQuickAction("task")}
            className="flex items-center gap-2"
            style={primaryButton}
          >
            <PlusIcon />
            Create Task
          </button>

          
          <ProjectInvitationNotifications
            onInvitationAccepted={() => {
              setTeamReload((value) => value + 1);
              setProjectReload((value) => value + 1);
            }}
          />


          <Avatar
            name={project.projectTitle}
            color="#6366f1"
            size={38}
          />
        </div>
      </header>

      {/* SCROLLABLE CONTENT */}
      <div className="flex-1 overflow-y-auto min-w-0">
        <div className="px-8 pt-6 pb-10 space-y-6">
          {/* PROJECT HEADER */}
          <div
            className="rounded-2xl overflow-hidden"
            style={{
              background: "white",
              boxShadow:
                "0 1px 6px rgba(26,24,48,.07)",
            }}
          >
            <div
              style={{
                height: 6,
                background:
                  "linear-gradient(90deg,#6366f1,#8b5cf6,#06b6d4)",
              }}
            />

            <div className="p-6">
              <div className="flex items-start gap-6">
                <div className="flex-1 min-w-0">
                  <Badge
                    color={statusColor(project.projectStatus)}
                  >
                    {formatStatus(project.projectStatus)}
                  </Badge>

                  <h1
                    style={{
                      fontFamily: "var(--font-display)",
                      fontSize: 24,
                      fontWeight: 500,
                      color: "#1a1830",
                      lineHeight: 1.2,
                      marginTop: 12,
                      marginBottom: 8,
                    }}
                  >
                    {project.projectTitle}
                  </h1>

                  <p
                    style={{
                      ...mutedText,
                      fontSize: 13.5,
                      maxWidth: 650,
                      marginBottom: 16,
                    }}
                  >
                    {project.projectDescription ||
                      "No project description provided."}
                  </p>

                  <div
                    className="flex flex-wrap items-center gap-x-6 gap-y-2"
                    style={{
                      fontSize: 12.5,
                      color: "#6b6890",
                    }}
                  >
                    <span>
                      <strong style={{ color: "#1a1830" }}>
                        {project.supervisorName ||
                          "Not assigned"}
                      </strong>{" "}
                      — Supervisor
                    </span>

                    <span>
                      Started{" "}
                      <strong style={{ color: "#1a1830" }}>
                        {formatDate(project.createdAt)}
                      </strong>
                    </span>

                    <span className="flex items-center gap-1.5">
                      <ClockIcon className="w-3.5 h-3.5" />
                      Next deadline:{" "}
                      <strong style={{ color: "#1a1830" }}>
                        {milestonesLoading
                          ? "Loading..."
                          : milestonesError
                          ? "Unavailable"
                          : nextDeadline}
                      </strong>
                    </span>
                  </div>

                  <div className="mt-4">
                    <Badge>Project #{project.projectId}</Badge>
                  </div>
                </div>

                {/* PROGRESS RING */}
                <div className="flex-shrink-0 flex flex-col items-center gap-2">
                  <div
                    className="relative"
                    style={{ width: 100, height: 100 }}
                  >
                    <svg
                      width="100"
                      height="100"
                      style={{
                        transform: "rotate(-90deg)",
                      }}
                    >
                      <circle
                        cx="50"
                        cy="50"
                        r="42"
                        fill="none"
                        stroke="#e8e7f0"
                        strokeWidth="8"
                      />

                      <circle
                        cx="50"
                        cy="50"
                        r="42"
                        fill="none"
                        stroke="#6366f1"
                        strokeWidth="8"
                        strokeDasharray={2 * Math.PI * 42}
                        strokeDashoffset={
                          2 * Math.PI * 42 *
                          (1 - progress / 100)
                        }
                        strokeLinecap="round"
                      />
                    </svg>

                    <div className="absolute inset-0 flex flex-col items-center justify-center">
                      <span
                        style={{
                          fontFamily: "var(--font-display)",
                          fontSize: 22,
                          color: "#1a1830",
                        }}
                      >
                        {progress}%
                      </span>

                      <span
                        style={{
                          fontSize: 10,
                          color: "#a8a6c0",
                        }}
                      >
                        complete
                      </span>
                    </div>
                  </div>

                  <span
                    style={{
                      fontSize: 11.5,
                      color: "#6b6890",
                    }}
                  >
                    Overall progress
                  </span>
                </div>
              </div>
            </div>
          </div>

          {/* KPI ROW */}
          <div className="grid grid-cols-2 md:grid-cols-3 xl:grid-cols-5 gap-4">
            {stats.map((stat) => (
              <div
                key={stat.label}
                className="rounded-2xl p-4 flex flex-col gap-2 min-w-0"
                style={panelStyle}
              >
                <p
                  style={{
                    fontSize: 11,
                    color: "#a8a6c0",
                    textTransform: "uppercase",
                    letterSpacing: ".04em",
                  }}
                >
                  {stat.label}
                </p>

                <div
                  style={{
                    fontFamily: "var(--font-display)",
                    fontSize: 22,
                    fontWeight: 500,
                    color: "#1a1830",
                    overflowWrap: "anywhere",
                  }}
                >
                  {stat.value}
                </div>

                {stat.bar !== null ? (
                  <ProgressBar
                    value={stat.bar}
                    color={stat.color}
                    height={4}
                  />
                ) : (
                  <span
                    style={{
                      width: 7,
                      height: 7,
                      background: stat.color,
                      borderRadius: "50%",
                    }}
                  />
                )}

                <p
                  style={{
                    fontSize: 11,
                    color: stat.color,
                  }}
                >
                  {stat.sub}
                </p>
              </div>
            ))}
          </div>

          {/* TEAM PREVIEW */}
          <Panel>
            <Heading
              title="Project Team"
              action={
                <button
                  type="button"
                  onClick={() => showQuickAction("invite")}
                  className="flex items-center gap-2"
                  style={lightButton}
                >
                  <UserPlusIcon />
                  Invite Member
                </button>
              }
            />

            <TeamCards compact />
          </Panel>

          {/* TAB BAR */}
          <div
            style={{
              borderBottom: "1px solid #e8e7f0",
            }}
          >
            <div className="flex gap-1 overflow-x-auto">
              {TABS.map((tab) => (
                <button
                  key={tab}
                  type="button"
                  onClick={() => setActiveTab(tab)}
                  className="px-4 py-3"
                  style={{
                    whiteSpace: "nowrap",
                    fontSize: 13.5,
                    color:
                      tab === activeTab
                        ? "#6366f1"
                        : "#6b6890",
                    fontWeight:
                      tab === activeTab ? 600 : 400,
                    borderBottom:
                      tab === activeTab
                        ? "2px solid #6366f1"
                        : "2px solid transparent",
                    marginBottom: -1,
                  }}
                >
                  {tab}
                </button>
              ))}
            </div>
          </div>

          {/* OVERVIEW */}
          {activeTab === "Overview" && (
            <div
              className="grid grid-cols-1 xl:grid-cols-2 gap-6"
              style={{
                alignItems: "start",
                width: "100%",
              }}
            >
              <div className="flex flex-col gap-5 min-w-0">
                <Panel>
                  <Heading
                    title="Milestone Timeline"
                    action={
                      <Badge>
                        {completedMilestones}/
                        {totalMilestones} done
                      </Badge>
                    }
                  />

                  <MilestoneTimeline
                    milestones={sortedMilestones}
                    loading={milestonesLoading}
                    error={milestonesError}
                    retry={() =>
                      setMilestoneReload((v) => v + 1)
                    }
                    compact
                  />

                  {!milestonesLoading &&
                    !milestonesError &&
                    milestones.length > 0 && (
                      <button
                        type="button"
                        onClick={() =>
                          setActiveTab("Milestones")
                        }
                        style={{
                          ...lightButton,
                          marginTop: 18,
                        }}
                      >
                        View All Milestones →
                      </button>
                    )}
                </Panel>

                <Panel>
                  <Heading title="Recent Activity" />

                  {activities.length === 0 ? (
                    <p style={mutedText}>
                      No recent project activity.
                    </p>
                  ) : (
                    <div className="space-y-3">
                      {activities.map((activity, index) => (
                        <div
                          key={index}
                          className="rounded-xl p-3"
                          style={{
                            background: "#f7f6f3",
                          }}
                        >
                          <p style={{ fontSize: 13 }}>
                            {activity.activityDescription}
                          </p>

                          <small
                            style={{ color: "#a8a6c0" }}
                          >
                            {formatDate(
                              activity.activityCreatedTime
                            )}
                          </small>
                        </div>
                      ))}
                    </div>
                  )}
                </Panel>
              </div>

              <div className="flex flex-col gap-5 min-w-0">
                <DeadlineCard />

                <Panel>
                  <Heading title="Supervisor Feedback" />

                  {feedback?.feedbackDescription ? (
                    <>
                      <strong style={{ fontSize: 13.5 }}>
                        {feedback.supervisorsFullname ||
                          "Supervisor"}
                      </strong>

                      <p style={mutedText}>
                        On{" "}
                        {feedback.milestoneName ||
                          "project milestone"}
                      </p>

                      <div
                        className="rounded-xl p-3 mt-3"
                        style={{
                          background: "#f7f6f3",
                          fontSize: 12.5,
                        }}
                      >
                        {feedback.feedbackDescription}
                      </div>

                      <small
                        style={{ color: "#a8a6c0" }}
                      >
                        {formatDate(
                          feedback.activityCreatedTime
                        )}
                      </small>
                    </>
                  ) : (
                    <p style={mutedText}>
                      No supervisor feedback yet.
                    </p>
                  )}
                </Panel>

                <Panel>
                  <Heading title="Quick Actions" />

                  <div className="grid grid-cols-2 gap-3">
                    {actionButtons.map((item) => (
                      <button
                        type="button"
                        key={item.label}
                        onClick={() =>
                          showQuickAction(item.action)
                        }
                        className="flex items-center gap-3 text-left"
                        style={lightButton}
                      >
                        {item.icon}
                        {item.label}
                      </button>
                    ))}
                  </div>
                </Panel>
              </div>
            </div>
          )}

          
          {/* TASKS */}
          {activeTab === "Tasks" && (
            <div className="grid grid-cols-1 xl:grid-cols-5 gap-6 items-start">

              {/* LEFT COLUMN — ALL PROJECT TASKS */}
              <div className="xl:col-span-3 min-w-0">
                <Panel>
                  
                <ProjectTaskList
                  projectId={projectId}
                  refreshKey={taskRefreshKey}
                  milestones={sortedMilestones}
                  onCreateTask={() => showQuickAction("task")}
                  onTasksChanged={() => {
                    setProjectReload((value) => value + 1);
                    setMilestoneReload((value) => value + 1);
                  }}
                />

                </Panel>
              </div>

              {/* RIGHT COLUMN — TASK SUMMARY AND ACTIONS */}
              <div className="xl:col-span-2 min-w-0 space-y-5">
                <Panel>
                  <Heading title="Task Summary" />

                  <div className="grid grid-cols-2 gap-3">
                    <Metric
                      label="Total Tasks"
                      value={totalTasks}
                    />

                    <Metric
                      label="Completed"
                      value={completedTasks}
                      color="#10b981"
                    />

                    <Metric
                      label="Remaining"
                      value={remainingTasks}
                      color="#f59e0b"
                    />

                    <Metric
                      label="Completion"
                      value={`${taskProgress}%`}
                      color="#8b5cf6"
                    />
                  </div>

                  <div className="mt-5">
                    <div className="flex justify-between mb-2">
                      <strong
                        style={{
                          fontSize: 12,
                          color: "#1a1830",
                        }}
                      >
                        Overall Task Completion
                      </strong>

                      <strong
                        style={{
                          fontSize: 12,
                          color: "#6366f1",
                        }}
                      >
                        {taskProgress}%
                      </strong>
                    </div>

                    <ProgressBar
                      value={taskProgress}
                      color="#10b981"
                      height={8}
                    />
                  </div>
                </Panel>

                <Panel>
                  <Heading title="Quick Actions" />

                  <p style={mutedText}>
                    Create tasks for this project and assign them
                    to your team members.
                  </p>

                  <div className="flex flex-col gap-3 mt-4">
                    <button
                      type="button"
                      onClick={() => showQuickAction("task")}
                      style={primaryButton}
                    >
                      + Create Task
                    </button>

                    <button
                      type="button"
                      onClick={() => openWorkspace("tasks")}
                      style={lightButton}
                    >
                      Open Tasks Workspace →
                    </button>
                  </div>
                </Panel>

                <Panel>
                  <Heading title="Project Milestones" />

                  <div className="grid grid-cols-2 gap-3">
                    <Metric
                      label="Total"
                      value={totalMilestones}
                    />

                    <Metric
                      label="Completed"
                      value={completedMilestones}
                      color="#10b981"
                    />
                  </div>

                  <button
                    type="button"
                    onClick={() => setActiveTab("Milestones")}
                    style={{
                      ...lightButton,
                      marginTop: 16,
                    }}
                  >
                    View Milestones →
                  </button>
                </Panel>
              </div>
            </div>
          )}


          {/* MILESTONES */}
          {activeTab === "Milestones" && (
            <div className="grid grid-cols-1 xl:grid-cols-5 gap-6 items-start">
              <div className="xl:col-span-3 min-w-0">
                <Panel>
                  <Heading
                    title="Milestone Timeline"
                    subtitle="Full milestone progress, deadlines and statuses."
                    action={
                      <Badge>
                        {totalMilestones} milestones
                      </Badge>
                    }
                  />

                  <MilestoneTimeline
                    milestones={sortedMilestones}
                    loading={milestonesLoading}
                    error={milestonesError}
                    retry={() =>
                      setMilestoneReload((v) => v + 1)
                    }
                  />
                </Panel>
              </div>

              <div className="xl:col-span-2 min-w-0 space-y-5">
                <Panel>
                  <Heading title="Milestone Summary" />

                  <div className="grid grid-cols-2 gap-3">
                    <Metric
                      label="Total Milestones"
                      value={totalMilestones}
                    />
                    <Metric
                      label="Completed"
                      value={completedMilestones}
                      color="#10b981"
                    />
                    <Metric
                      label="Remaining"
                      value={remainingMilestones}
                      color="#f59e0b"
                    />
                    <Metric
                      label="Completion"
                      value={`${milestoneProgress}%`}
                      color="#8b5cf6"
                    />
                  </div>

                  <div className="mt-5">
                    <ProgressBar
                      value={milestoneProgress}
                      color="#10b981"
                      height={8}
                    />
                  </div>
                </Panel>

                <DeadlineCard />

                <Panel>
                  <Heading title="Milestone Actions" />

                  <p style={mutedText}>
                    Submit a milestone deliverable without
                    leaving Project Details.
                  </p>

                  <button
                    type="button"
                    onClick={() =>
                      showQuickAction("milestone")
                    }
                    style={{
                      ...primaryButton,
                      marginTop: 16,
                    }}
                  >
                    Submit Milestone
                  </button>
                </Panel>
              </div>
            </div>
          )}

          
{/* SUBMISSIONS */}
{activeTab === "Submissions" && (
  <div className="grid grid-cols-1 xl:grid-cols-5 gap-6 items-start">

    {/* LEFT COLUMN — REAL SUBMISSION HISTORY */}
    <div className="xl:col-span-3 min-w-0">
      <Panel>
        <ProjectSubmissionHistory
          projectId={projectId}
          refreshKey={submissionRefreshKey}
          onNewSubmission={() =>
            showQuickAction("milestone")
          }
        />
      </Panel>
    </div>

    {/* RIGHT COLUMN — PROJECT MILESTONES AND NEW SUBMISSION */}
    <div className="xl:col-span-2 min-w-0 space-y-5">
      <Panel>
        <Heading title="Project Milestones" />

        <div className="grid grid-cols-2 gap-3">
          <Metric
            label="Total"
            value={totalMilestones}
          />

          <Metric
            label="Completed"
            value={completedMilestones}
            color="#10b981"
          />
        </div>
      </Panel>

      <Panel>
        <Heading title="New Submission" />

        <p style={mutedText}>
          Select a milestone and submit a PDF
          for supervisor review.
        </p>

        <button
          type="button"
          onClick={() =>
            showQuickAction("milestone")
          }
          style={{
            ...primaryButton,
            marginTop: 16,
          }}
        >
          New Submission
        </button>
      </Panel>
    </div>
  </div>
)}


          {/* DOCUMENTS */}
          {activeTab === "Documents" && (
            <div className="grid grid-cols-1 xl:grid-cols-3 gap-6 items-start">
              <div className="xl:col-span-2 min-w-0">
                <Panel>
                  <Heading
                    title="Project Documents"
                    subtitle="Documents and milestone deliverables."
                  />

                  <div
                    className="rounded-xl p-6"
                    style={{
                      background: "#f7f6f3",
                      border: "1px dashed #c7d2fe",
                    }}
                  >
                    <UploadIcon />

                    <h4
                      style={{
                        fontFamily: "var(--font-display)",
                        fontSize: 17,
                        marginTop: 15,
                      }}
                    >
                      Document Library
                    </h4>

                    <p
                      style={{
                        ...mutedText,
                        marginTop: 10,
                      }}
                    >
                      The project overview does not yet
                      expose an independent document list.
                      Existing milestone PDFs are available
                      through the Submissions workspace.
                    </p>

                    <button
                      type="button"
                      onClick={() =>
                        openWorkspace("submissions")
                      }
                      style={{
                        ...lightButton,
                        marginTop: 20,
                      }}
                    >
                      View Submissions →
                    </button>
                  </div>
                </Panel>
              </div>

              <div className="min-w-0">
                <Panel>
                  <Heading title="Document Actions" />

                  <div className="flex flex-col gap-3">
                    <button
                      type="button"
                      onClick={() =>
                        showQuickAction("document")
                      }
                      style={primaryButton}
                    >
                      Upload Document
                    </button>

                    <button
                      type="button"
                      onClick={() =>
                        openWorkspace("submissions")
                      }
                      style={lightButton}
                    >
                      Submission History →
                    </button>

                    <button
                      type="button"
                      onClick={() =>
                        setActiveTab("Milestones")
                      }
                      style={lightButton}
                    >
                      View Milestones →
                    </button>
                  </div>
                </Panel>
              </div>
            </div>
          )}

          {/* TEAM */}
          {activeTab === "Team" && (
            <div className="grid grid-cols-1 xl:grid-cols-5 gap-6 items-start">
              <div className="xl:col-span-3 min-w-0">
                <Panel>
                  <Heading
                    title="Team Members"
                    subtitle="Accepted project team members."
                    action={
                      <Badge>
                        {teamMembers.length} members
                      </Badge>
                    }
                  />

                  <TeamCards />
                </Panel>
              </div>

              <div className="xl:col-span-2 min-w-0 space-y-5">
                <Panel>
                  <Heading title="Team Overview" />

                  <div className="grid grid-cols-2 gap-3">
                    <Metric
                      label="Members"
                      value={teamMembers.length}
                    />

                    <Metric
                      label="Project Tasks"
                      value={totalTasks}
                      color="#8b5cf6"
                    />
                  </div>

                  <div className="mt-5">
                    <strong style={{ fontSize: 12 }}>
                      Academic Supervisor
                    </strong>

                    <p
                      style={{
                        ...mutedText,
                        marginTop: 5,
                      }}
                    >
                      {project.supervisorName ||
                        "Not assigned"}
                    </p>
                  </div>
                </Panel>

                <Panel>
                  <Heading title="Team Actions" />

                  <p style={mutedText}>
                    Invite eligible students directly
                    from this page.
                  </p>

                  <div className="flex flex-col gap-3 mt-4">
                    <button
                      type="button"
                      onClick={() =>
                        showQuickAction("invite")
                      }
                      style={primaryButton}
                    >
                      Invite Member
                    </button>

                    <button
                      type="button"
                      onClick={() =>
                        showQuickAction("chat")
                      }
                      style={lightButton}
                    >
                      Open Project Discussion →
                    </button>
                  </div>
                </Panel>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* ======================================================
          QUICK ACTION MODAL
         ====================================================== */}

      {quickAction && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center p-4"
          style={{
            background: "rgba(20,18,40,.48)",
          }}
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) {
              closeQuickAction();
            }
          }}
        >
          <div
            role="dialog"
            aria-modal="true"
            aria-label="Project quick action"
            className="w-full rounded-2xl p-6 max-h-[90vh] overflow-y-auto"
            style={{
              maxWidth: 560,
              background: "white",
              boxShadow:
                "0 16px 50px rgba(26,24,48,.2)",
            }}
          >
            <div className="flex items-start justify-between gap-3 mb-5">
              <div>
                <h2
                  style={{
                    fontFamily: "var(--font-display)",
                    fontSize: 21,
                    color: "#1a1830",
                  }}
                >
                  {quickAction === "task"
                    ? "Create Task"
                    : quickAction === "milestone"
                    ? "Submit Milestone"
                    : quickAction === "document"
                    ? "Upload Document"
                    : quickAction === "invite"
                    ? "Invite Member"
                    : "Project Discussion"}
                </h2>

                <p
                  style={{
                    ...mutedText,
                    marginTop: 5,
                  }}
                >
                  {project.projectTitle}
                </p>
              </div>

              <button
                type="button"
                disabled={saving}
                onClick={closeQuickAction}
                aria-label="Close"
                style={{
                  color: "#6b6890",
                  fontSize: 19,
                }}
              >
                ✕
              </button>
            </div>

            {/* CHAT ONLY: NAVIGATION CONFIRMATION */}
            {quickAction === "chat" ? (
              <>
                <p style={mutedText}>
                  Project Chat is available in the
                  Discussion workspace. Would you like
                  to go there?
                </p>

                <div className="flex justify-end gap-3 mt-6">
                  <button
                    type="button"
                    onClick={closeQuickAction}
                    style={lightButton}
                  >
                    Stay Here
                  </button>

                  <button
                    type="button"
                    onClick={() => {
                      setQuickAction(null);
                      openWorkspace("discussion");
                    }}
                    style={primaryButton}
                  >
                    Open Discussion →
                  </button>
                </div>
              </>
            ) : (
              <form
                onSubmit={submitQuickAction}
                className="space-y-4"
              >
                {/* INVITE MEMBER FORM */}
                {quickAction === "invite" && (
                  <>
                    {teamLoading && (
                      <p style={mutedText}>
                        Loading team permissions...
                      </p>
                    )}

                    {teamError && (
                      <p
                        role="alert"
                        style={{
                          color: "#b91c1c",
                          fontSize: 12,
                        }}
                      >
                        {teamError}
                      </p>
                    )}

                    {team && (
                      <div
                        className="rounded-xl p-4"
                        style={{
                          background: "#f7f6f3",
                        }}
                      >
                        <div className="flex justify-between items-center gap-3">
                          <div>
                            <strong style={{ fontSize: 13 }}>
                              Project Team
                            </strong>

                            <p style={mutedText}>
                              {team.memberCount} of{" "}
                              {team.maxMembers} members
                            </p>
                          </div>

                          <Badge
                            color={
                              team.availableSlots > 0
                                ? "#10b981"
                                : "#ef4444"
                            }
                          >
                            {team.availableSlots} slots
                            available
                          </Badge>
                        </div>

                        {team.pendingInvitationCount > 0 && (
                          <p
                            style={{
                              ...mutedText,
                              marginTop: 10,
                            }}
                          >
                            {team.pendingInvitationCount}{" "}
                            pending invitation(s)
                          </p>
                        )}
                      </div>
                    )}

                    {!teamLoading &&
                      !teamError &&
                      !team && (
                        <p
                          style={{
                            color: "#b91c1c",
                            fontSize: 12.5,
                          }}
                        >
                          This project was not found in
                          your available project teams.
                        </p>
                      )}

                    {team && !team.canInvite && (
                      <p
                        style={{
                          color: "#b91c1c",
                          fontSize: 12.5,
                        }}
                      >
                        Only the project leader can
                        invite students, and the team
                        must have an available slot.
                      </p>
                    )}

                    {team?.canInvite && !formSuccess && (
                      <>
                        <label className="block text-sm">
                          Search Eligible Students
                          <input
                            type="search"
                            value={studentQuery}
                            onChange={(event) => {
                              setStudentQuery(
                                event.target.value
                              );
                              setSelectedStudent(null);
                            }}
                            placeholder="Search by name or email"
                            style={inputStyle}
                          />
                        </label>

                        {searchingStudents && (
                          <p style={mutedText}>
                            Searching students...
                          </p>
                        )}

                        {searchError && (
                          <p
                            role="alert"
                            style={{
                              fontSize: 12,
                              color: "#b91c1c",
                            }}
                          >
                            {searchError}
                          </p>
                        )}

                        {!searchingStudents &&
                          !searchError &&
                          studentQuery.trim().length >= 2 &&
                          eligibleStudents.length === 0 && (
                            <p style={mutedText}>
                              No eligible students found.
                            </p>
                          )}

                        {eligibleStudents.length > 0 && (
                          <div
                            className="rounded-xl overflow-hidden"
                            style={{
                              border:
                                "1px solid #e8e7f0",
                            }}
                          >
                            {eligibleStudents.map(
                              (student) => (
                                <button
                                  type="button"
                                  key={student.userId}
                                  onClick={() =>
                                    setSelectedStudent(student)
                                  }
                                  className="w-full flex justify-between items-center gap-3 p-3 text-left"
                                  style={{
                                    background:
                                      selectedStudent?.userId ===
                                      student.userId
                                        ? "#f0eff8"
                                        : "white",
                                    borderBottom:
                                      "1px solid #e8e7f0",
                                  }}
                                >
                                  <div className="min-w-0">
                                    <strong
                                      style={{
                                        fontSize: 13,
                                      }}
                                    >
                                      {student.fullName}
                                    </strong>

                                    <p style={mutedText}>
                                      {student.email}
                                    </p>
                                  </div>

                                  {selectedStudent?.userId ===
                                    student.userId && (
                                    <Badge color="#10b981">
                                      Selected
                                    </Badge>
                                  )}
                                </button>
                              )
                            )}
                          </div>
                        )}

                        {selectedStudent && (
                          <div
                            className="rounded-xl p-3"
                            style={{
                              background: "#ecfdf5",
                            }}
                          >
                            <strong
                              style={{
                                color: "#047857",
                                fontSize: 12.5,
                              }}
                            >
                              Invite {selectedStudent.fullName}
                            </strong>

                            <p
                              style={{
                                color: "#047857",
                                fontSize: 11.5,
                              }}
                            >
                              {selectedStudent.email}
                            </p>
                          </div>
                        )}
                      </>
                    )}
                  </>
                )}

                {/* CREATE TASK FORM */}
                {quickAction === "task" && (
                  <>
                    <label className="block text-sm">
                      Task Title
                      <input
                        required
                        maxLength={255}
                        value={taskForm.title}
                        onChange={(event) =>
                          setTaskForm((form) => ({
                            ...form,
                            title: event.target.value,
                          }))
                        }
                        style={inputStyle}
                        disabled={Boolean(formSuccess)}
                      />
                    </label>

                    <label className="block text-sm">
                      Description
                      <textarea
                        required
                        rows={3}
                        value={taskForm.description}
                        onChange={(event) =>
                          setTaskForm((form) => ({
                            ...form,
                            description: event.target.value,
                          }))
                        }
                        style={inputStyle}
                        disabled={Boolean(formSuccess)}
                      />
                    </label>

                    <div className="grid grid-cols-2 gap-3">
                      <label className="block text-sm">
                        Priority
                        <select
                          value={taskForm.priority}
                          onChange={(event) =>
                            setTaskForm((form) => ({
                              ...form,
                              priority: event.target.value,
                            }))
                          }
                          style={inputStyle}
                          disabled={Boolean(formSuccess)}
                        >
                          <option value="LOW">Low</option>
                          <option value="MEDIUM">Medium</option>
                          <option value="HIGH">High</option>
                        </select>
                      </label>

                      <label className="block text-sm">
                        Deadline
                        <input
                          required
                          type="date"
                          min={localDateToday()}
                          value={taskForm.deadline}
                          onChange={(event) =>
                            setTaskForm((form) => ({
                              ...form,
                              deadline: event.target.value,
                            }))
                          }
                          style={inputStyle}
                          disabled={Boolean(formSuccess)}
                        />
                      </label>
                    </div>

                    <label className="block text-sm">
                      Milestone
                      <select
                        required
                        value={taskForm.milestoneId}
                        onChange={(event) =>
                          setTaskForm((form) => ({
                            ...form,
                            milestoneId: Number(
                              event.target.value
                            ),
                          }))
                        }
                        style={inputStyle}
                        disabled={Boolean(formSuccess)}
                      >
                        <option value={0}>
                          Select milestone
                        </option>

                        {sortedMilestones.map((item) => (
                          <option
                            key={item.milestoneId}
                            value={item.milestoneId}
                          >
                            {item.title}
                          </option>
                        ))}
                      </select>
                    </label>

                    <label className="block text-sm">
                      Assign To
                      <select
                        required
                        value={taskForm.memberId}
                        onChange={(event) =>
                          setTaskForm((form) => ({
                            ...form,
                            memberId: Number(
                              event.target.value
                            ),
                          }))
                        }
                        style={inputStyle}
                        disabled={Boolean(formSuccess)}
                      >
                        <option value={0}>
                          Select team member
                        </option>

                        {(team?.members ?? []).map(
                          (member) => (
                            <option
                              key={member.memberId}
                              value={member.memberId}
                            >
                              {member.fullName}
                            </option>
                          )
                        )}
                      </select>
                    </label>

                    {teamError && (
                      <p
                        style={{
                          fontSize: 12,
                          color: "#b91c1c",
                        }}
                      >
                        Team members could not be loaded:
                        {" "}{teamError}
                      </p>
                    )}
                  </>
                )}

                {/* MILESTONE / DOCUMENT FORM */}
                {(quickAction === "milestone" ||
                  quickAction === "document") && (
                  <>
                    <label className="block text-sm">
                      Select Milestone
                      <select
                        required
                        value={submissionMilestoneId}
                        onChange={(event) =>
                          setSubmissionMilestoneId(
                            Number(event.target.value)
                          )
                        }
                        style={inputStyle}
                        disabled={Boolean(formSuccess)}
                      >
                        <option value={0}>
                          Select milestone
                        </option>

                        {sortedMilestones.map((item) => (
                          <option
                            key={item.milestoneId}
                            value={item.milestoneId}
                          >
                            {item.title}
                          </option>
                        ))}
                      </select>
                    </label>

                    <label className="block text-sm">
                      PDF Document
                      <input
                        type="file"
                        accept=".pdf,application/pdf"
                        required={!formSuccess}
                        onChange={(event) =>
                          setSubmissionFile(
                            event.target.files?.[0] ??
                              null
                          )
                        }
                        style={inputStyle}
                        disabled={Boolean(formSuccess)}
                      />
                    </label>

                    <p style={mutedText}>
                      The PDF will be submitted for
                      supervisor review. Submission does
                      not automatically mean approval.
                    </p>
                  </>
                )}

                {/* ERROR MESSAGE */}
                {formError && (
                  <div
                    role="alert"
                    className="rounded-xl p-3"
                    style={{
                      background: "#fff1f2",
                      color: "#b91c1c",
                      fontSize: 12.5,
                    }}
                  >
                    {formError}
                  </div>
                )}

                {/* SUCCESS MESSAGE */}
                {formSuccess && (
                  <div
                    role="status"
                    className="rounded-xl p-3"
                    style={{
                      background: "#ecfdf5",
                      color: "#047857",
                      fontSize: 12.5,
                    }}
                  >
                    {formSuccess}
                  </div>
                )}

                {/* FORM FOOTER */}
                <div className="flex justify-end gap-3 pt-3">
                  <button
                    type="button"
                    disabled={saving}
                    onClick={closeQuickAction}
                    style={lightButton}
                  >
                    {formSuccess ? "Close" : "Cancel"}
                  </button>

                  {!formSuccess && (
                    <button
                      type="submit"
                      disabled={
                        saving ||
                        (quickAction === "invite" &&
                          (!team?.canInvite ||
                            !selectedStudent))
                      }
                      style={{
                        ...primaryButton,
                        opacity: saving ? 0.6 : 1,
                      }}
                    >
                      {saving
                        ? "Saving..."
                        : quickAction === "invite"
                        ? "Send Invitation"
                        : quickAction === "task"
                        ? "Create Task"
                        : "Submit PDF"}
                    </button>
                  )}
                </div>
              </form>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
