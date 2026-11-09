
import { useEffect, useMemo, useState } from "react";
import type { CSSProperties } from "react";
import ProjectTaskSubtasks from "./ProjectTaskSubtasks";

interface ProjectTask {
  taskId: number;
  title: string;
  description: string | null;
  status: string;
  priority: string;
  deadline: string | null;
  progress: number | null;
  milestoneId: number | null;
  assignedMemberId: number | null;
  assignedMemberName: string | null;
  createdAt: string | null;
}

interface ProjectTaskListProps {
  projectId: number;
  refreshKey?: number;
  milestones?: {
    milestoneId: number;
    title: string;
  }[];
  onCreateTask?: () => void;
  onTasksChanged?: () => void;
}

type TaskFilter =
  | "ALL"
  | "PENDING"
  | "IN_PROGRESS"
  | "COMPLETED";

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
  background:
    "linear-gradient(135deg,#6366f1,#8b5cf6)",
  color: "white",
  padding: "10px 16px",
  borderRadius: 10,
  fontSize: 12,
  fontWeight: 600,
};

function getToken(): string {
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

  return token;
}

async function getProjectTasks(
  projectId: number,
  signal?: AbortSignal
): Promise<ProjectTask[]> {
  const response = await fetch(
    `/api/projects/${projectId}/tasks`,
    {
      method: "GET",
      signal,
      headers: {
        Authorization: `Bearer ${getToken()}`,
        Accept: "application/json",
      },
    }
  );

  if (!response.ok) {
    const body = await response.text();
    let message = `Unable to load tasks (HTTP ${response.status}).`;

    try {
      const parsed = JSON.parse(body);
      message = parsed.message || parsed.error || message;
    } catch {
      if (body && !body.trim().startsWith("<")) {
        message = body;
      }
    }

    throw new Error(message);
  }

  const data: unknown = await response.json();

  if (!Array.isArray(data)) {
    throw new Error("Unexpected project tasks response.");
  }

  return data as ProjectTask[];
}

function formatStatus(value: string | null): string {
  if (!value) return "Unknown";

  return value
    .toLowerCase()
    .replace(/_/g, " ")
    .replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function formatDate(value: string | null): string {
  if (!value) return "Not set";

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return date.toLocaleDateString(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}

function statusColor(status: string): string {
  switch (status.toUpperCase()) {
    case "COMPLETED":
    case "DONE":
      return "#10b981";

    case "IN_PROGRESS":
      return "#6366f1";

    case "PENDING":
    case "TODO":
    case "TO_DO":
      return "#f59e0b";

    case "BLOCKED":
    case "CANCELLED":
      return "#ef4444";

    default:
      return "#6b6890";
  }
}

function priorityColor(priority: string): string {
  switch (priority.toUpperCase()) {
    case "HIGH":
    case "URGENT":
      return "#ef4444";

    case "MEDIUM":
      return "#f59e0b";

    case "LOW":
      return "#10b981";

    default:
      return "#6366f1";
  }
}

function Badge({
  value,
  color,
}: {
  value: string;
  color: string;
}) {
  return (
    <span
      className="inline-flex items-center rounded-lg px-2.5 py-1 text-xs font-semibold"
      style={{
        color,
        background: `${color}18`,
      }}
    >
      {formatStatus(value)}
    </span>
  );
}

function ProgressBar({
  value,
  color = "#6366f1",
}: {
  value: number;
  color?: string;
}) {
  const progress = Math.min(
    100,
    Math.max(0, Number.isFinite(value) ? value : 0)
  );

  return (
    <div
      className="w-full rounded-full overflow-hidden"
      style={{
        height: 6,
        background: "#e8e7f0",
      }}
    >
      <div
        className="h-full rounded-full"
        style={{
          width: `${progress}%`,
          background: color,
        }}
      />
    </div>
  );
}

export default function ProjectTaskList({
  projectId,
  refreshKey = 0,
  milestones = [],
  onCreateTask,
  onTasksChanged,
}: ProjectTaskListProps) {
  const [tasks, setTasks] = useState<ProjectTask[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [retryKey, setRetryKey] = useState(0);
  const [filter, setFilter] = useState<TaskFilter>("ALL");
  const [search, setSearch] = useState("");
  const [expandedTaskId, setExpandedTaskId] =
  useState<number | null>(null);

  useEffect(() => {
    const controller = new AbortController();

    setLoading(true);
    setError("");

    getProjectTasks(projectId, controller.signal)
      .then((data) => {
        if (!controller.signal.aborted) {
          setTasks(data);
        }
      })
      .catch((err) => {
        if (!controller.signal.aborted) {
          setError(
            err instanceof Error
              ? err.message
              : "Unable to load project tasks."
          );
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      });

    return () => controller.abort();
  }, [projectId, refreshKey, retryKey]);

  const filteredTasks = useMemo(() => {
    const query = search.trim().toLowerCase();

    return tasks
      .filter((task) => {
        const status = task.status?.toUpperCase() ?? "";

        if (filter === "COMPLETED") {
          return status === "COMPLETED" || status === "DONE";
        }

        if (filter === "PENDING") {
          return (
            status === "PENDING" ||
            status === "TODO" ||
            status === "TO_DO"
          );
        }

        if (filter === "IN_PROGRESS") {
          return status === "IN_PROGRESS";
        }

        return true;
      })
      .filter((task) => {
        if (!query) return true;

        return (
          task.title.toLowerCase().includes(query) ||
          (task.description ?? "")
            .toLowerCase()
            .includes(query) ||
          (task.assignedMemberName ?? "")
            .toLowerCase()
            .includes(query)
        );
      })
      .sort((a, b) => {
        if (!a.deadline && !b.deadline) {
          return b.taskId - a.taskId;
        }

        if (!a.deadline) return 1;
        if (!b.deadline) return -1;

        return (
          a.deadline.localeCompare(b.deadline) ||
          b.taskId - a.taskId
        );
      });
  }, [tasks, filter, search]);

  const completedCount = tasks.filter((task) =>
    ["COMPLETED", "DONE"].includes(
      task.status?.toUpperCase() ?? ""
    )
  ).length;

  function getMilestoneTitle(
    milestoneId: number | null
  ): string {
    if (milestoneId == null) {
      return "No milestone";
    }

    const milestone = milestones.find(
      (item) => item.milestoneId === milestoneId
    );

    return milestone?.title ?? `Milestone #${milestoneId}`;
  }

  const filters: {
    label: string;
    value: TaskFilter;
  }[] = [
    { label: "All", value: "ALL" },
    { label: "Pending", value: "PENDING" },
    { label: "In Progress", value: "IN_PROGRESS" },
    { label: "Completed", value: "COMPLETED" },
  ];

  return (
    <div className="min-w-0">
      <div className="flex items-start justify-between gap-3 flex-wrap mb-5">
        <div>
          <h3
            style={{
              fontFamily: "var(--font-display)",
              fontSize: 18,
              fontWeight: 500,
              color: "#1a1830",
            }}
          >
            Project Tasks
          </h3>

          <p style={{ ...mutedText, marginTop: 5 }}>
            Every task assigned within this project.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <button
            type="button"
            disabled={loading}
            onClick={() => setRetryKey((value) => value + 1)}
            style={{
              ...lightButton,
              opacity: loading ? 0.6 : 1,
            }}
          >
            Refresh
          </button>

          {onCreateTask && (
            <button
              type="button"
              onClick={onCreateTask}
              style={primaryButton}
            >
              + Create Task
            </button>
          )}
        </div>
      </div>

      <div className="grid grid-cols-2 gap-3 mb-5">
        <div
          className="rounded-xl p-4"
          style={{
            background: "#f7f6f3",
            border: "1px solid #e8e7f0",
          }}
        >
          <p style={mutedText}>Total Tasks</p>
          <strong
            style={{
              display: "block",
              fontSize: 24,
              color: "#6366f1",
              marginTop: 6,
            }}
          >
            {tasks.length}
          </strong>
        </div>

        <div
          className="rounded-xl p-4"
          style={{
            background: "#f7f6f3",
            border: "1px solid #e8e7f0",
          }}
        >
          <p style={mutedText}>Completed</p>
          <strong
            style={{
              display: "block",
              fontSize: 24,
              color: "#10b981",
              marginTop: 6,
            }}
          >
            {completedCount}
          </strong>
        </div>
      </div>

      <input
        type="search"
        value={search}
        onChange={(event) => setSearch(event.target.value)}
        placeholder="Search tasks or team members..."
        aria-label="Search project tasks"
        className="w-full rounded-xl mb-4"
        style={{
          border: "1px solid #e8e7f0",
          background: "#fff",
          padding: "11px 13px",
          fontSize: 12.5,
        }}
      />

      <div className="flex gap-2 flex-wrap mb-5">
        {filters.map((item) => (
          <button
            key={item.value}
            type="button"
            onClick={() => setFilter(item.value)}
            className="rounded-lg px-3 py-2"
            style={{
              fontSize: 12,
              fontWeight: 600,
              background:
                filter === item.value
                  ? "#6366f1"
                  : "#f0eff8",
              color:
                filter === item.value
                  ? "#fff"
                  : "#6366f1",
            }}
          >
            {item.label}
          </button>
        ))}
      </div>

      {loading && (
        <div
          className="rounded-xl p-5"
          style={{ background: "#f7f6f3" }}
        >
          <p style={mutedText}>Loading project tasks...</p>
        </div>
      )}

      {!loading && error && (
        <div
          role="alert"
          className="rounded-xl p-5"
          style={{ background: "#fff1f2" }}
        >
          <p
            style={{
              color: "#b91c1c",
              fontSize: 12.5,
            }}
          >
            {error}
          </p>

          <button
            type="button"
            onClick={() =>
              setRetryKey((value) => value + 1)
            }
            style={{
              ...lightButton,
              marginTop: 12,
            }}
          >
            Try Again
          </button>
        </div>
      )}

      {!loading && !error && tasks.length === 0 && (
        <div
          className="rounded-xl p-6"
          style={{
            background: "#f7f6f3",
            border: "1px dashed #c7d2fe",
          }}
        >
          <h4
            style={{
              fontFamily: "var(--font-display)",
              fontSize: 17,
              color: "#1a1830",
            }}
          >
            No Tasks Yet
          </h4>

          <p style={{ ...mutedText, marginTop: 8 }}>
            No tasks have been created for this project.
          </p>

          {onCreateTask && (
            <button
              type="button"
              onClick={onCreateTask}
              style={{
                ...primaryButton,
                marginTop: 16,
              }}
            >
              Create First Task
            </button>
          )}
        </div>
      )}

      {!loading &&
        !error &&
        tasks.length > 0 &&
        filteredTasks.length === 0 && (
          <div
            className="rounded-xl p-5"
            style={{ background: "#f7f6f3" }}
          >
            <p style={mutedText}>
              No tasks match your selected filters.
            </p>
          </div>
        )}

      {!loading && !error && filteredTasks.length > 0 && (
        <div className="space-y-4">
          {filteredTasks.map((task) => {
            const color = statusColor(task.status);
            const progress = Math.min(
              100,
              Math.max(0, task.progress ?? 0)
            );

            return (
              <div
                key={task.taskId}
                className="rounded-xl p-4 min-w-0"
                style={{
                  background: "#f7f6f3",
                  border: "1px solid #e8e7f0",
                }}
              >
                <div className="flex items-start justify-between gap-3 flex-wrap">
                  <div className="flex-1 min-w-0">
                    <strong
                      style={{
                        display: "block",
                        fontSize: 14,
                        color: "#1a1830",
                        overflowWrap: "anywhere",
                      }}
                    >
                      {task.title}
                    </strong>

                    {task.description && (
                      <p
                        style={{
                          ...mutedText,
                          marginTop: 7,
                          whiteSpace: "pre-wrap",
                          overflowWrap: "anywhere",
                        }}
                      >
                        {task.description}
                      </p>
                    )}
                  </div>

                  <Badge
                    value={task.status}
                    color={color}
                  />
                </div>

                <div className="flex flex-wrap items-center gap-2 mt-4">
                  <Badge
                    value={`${task.priority} Priority`}
                    color={priorityColor(task.priority)}
                  />

                  <span
                    style={{
                      color: "#6b6890",
                      fontSize: 11.5,
                    }}
                  >
                    {getMilestoneTitle(task.milestoneId)}
                  </span>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 mt-4">
                  <div>
                    <p style={{ ...mutedText, fontSize: 11 }}>
                      Assigned To
                    </p>
                    <strong
                      style={{
                        fontSize: 12.5,
                        color: "#1a1830",
                      }}
                    >
                      {task.assignedMemberName ||
                        "Unassigned"}
                    </strong>
                  </div>

                  <div>
                    <p style={{ ...mutedText, fontSize: 11 }}>
                      Deadline
                    </p>
                    <strong
                      style={{
                        fontSize: 12.5,
                        color: "#1a1830",
                      }}
                    >
                      {formatDate(task.deadline)}
                    </strong>
                  </div>
                </div>

                <div className="flex justify-between items-center mt-4 mb-2">
                  <span style={mutedText}>Task Progress</span>
                  <strong
                    style={{
                      color,
                      fontSize: 12,
                    }}
                  >
                    {progress}%
                  </strong>
                </div>

                <ProgressBar
                  value={progress}
                  color={color}
                />

                <div
                  className="flex justify-between items-center mt-4 pt-3"
                  style={{
                  borderTop: "1px solid #e8e7f0",
                  }}
                  >
                  <span
                  style={{
                        fontSize: 11,
                        color: "#a8a6c0",
                  }}
                  >
                  Task #{task.taskId}
                  </span>

                  <span
                  style={{
                        fontSize: 11,
                        color: "#a8a6c0",
                  }}
                  >
                  Created {formatDate(task.createdAt)}
                  </span>
                  </div>

                  {/* EXPAND / COLLAPSE SUBTASKS */}
                  <button
                  type="button"
                  onClick={() =>
                  setExpandedTaskId((current) =>
                        current === task.taskId ? null : task.taskId
                  )
                  }
                  style={{
                  ...lightButton,
                  marginTop: 14,
                  width: "100%",
                  }}
                  >
                  {expandedTaskId === task.taskId
                  ? "Hide Subtasks ↑"
                  : "View / Manage Subtasks ↓"}
                  </button>

                  {/* SUBTASK MANAGEMENT */}
                  {expandedTaskId === task.taskId && (
                  <ProjectTaskSubtasks
                  projectId={projectId}
                  taskId={task.taskId}
                  onChanged={() => {
                        setRetryKey((value) => value + 1);
                        onTasksChanged?.();
                  }}
                  />
                  )}
                  </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
