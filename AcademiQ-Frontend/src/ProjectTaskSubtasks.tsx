
import { useEffect, useState } from "react";
import type { CSSProperties } from "react";

type SubtaskStatus = "TODO" | "IN_PROGRESS" | "COMPLETED";

interface Subtask {
  subtaskId: number;
  taskId: number;
  title: string;
  description: string | null;
  status: SubtaskStatus;
  assignedToUserId: number | null;
  assignedToName: string | null;
}

interface ProjectTaskSubtasksProps {
  projectId: number;
  taskId: number;
  onChanged: () => void;
}

const mutedText: CSSProperties = {
  fontSize: 12,
  color: "#6b6890",
  lineHeight: 1.6,
};

const buttonStyle: CSSProperties = {
  background: "#f0eff8",
  color: "#6366f1",
  fontSize: 12,
  fontWeight: 600,
  padding: "9px 12px",
  borderRadius: 9,
};

const inputStyle: CSSProperties = {
  width: "100%",
  background: "white",
  border: "1px solid #e8e7f0",
  borderRadius: 10,
  padding: "10px 12px",
  fontSize: 12.5,
};

function getToken(): string {
  const stored = sessionStorage.getItem("academiq_auth");

  if (!stored) {
    throw new Error("Please sign in again.");
  }

  const auth = JSON.parse(stored);
  const token: unknown = auth?.token;

  if (typeof token !== "string" || !token.trim()) {
    throw new Error("Authentication token is missing.");
  }

  return token;
}

async function apiRequest<T>(
  url: string,
  options: RequestInit = {}
): Promise<T> {
  const response = await fetch(url, {
    ...options,
    headers: {
      Authorization: `Bearer ${getToken()}`,
      Accept: "application/json",
      ...(options.body
        ? { "Content-Type": "application/json" }
        : {}),
      ...options.headers,
    },
  });

  if (!response.ok) {
    const body = await response.text();
    let message = `Request failed (HTTP ${response.status}).`;

    try {
      const parsed = JSON.parse(body);
      message = parsed.message || parsed.error || message;
    } catch {
      if (body.trim() && !body.trim().startsWith("<")) {
        message = body;
      }
    }

    throw new Error(message);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

function statusColor(status: SubtaskStatus): string {
  switch (status) {
    case "COMPLETED":
      return "#10b981";
    case "IN_PROGRESS":
      return "#6366f1";
    default:
      return "#f59e0b";
  }
}

function statusLabel(status: SubtaskStatus): string {
  switch (status) {
    case "COMPLETED":
      return "Completed";
    case "IN_PROGRESS":
      return "In Progress";
    default:
      return "To Do";
  }
}

export default function ProjectTaskSubtasks({
  projectId,
  taskId,
  onChanged,
}: ProjectTaskSubtasksProps) {
  const [subtasks, setSubtasks] = useState<Subtask[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [refreshKey, setRefreshKey] = useState(0);
  const [newTitle, setNewTitle] = useState("");
  const [newDescription, setNewDescription] = useState("");
  const [creating, setCreating] = useState(false);
  const [updatingId, setUpdatingId] = useState<number | null>(
    null
  );

  const baseUrl =
    `/api/projects/${projectId}/tasks/${taskId}/subtasks`;

  useEffect(() => {
    const controller = new AbortController();

    setLoading(true);
    setError("");

    apiRequest<Subtask[]>(baseUrl, {
      signal: controller.signal,
    })
      .then((data) => {
        if (!controller.signal.aborted) {
          setSubtasks(data);
        }
      })
      .catch((err) => {
        if (!controller.signal.aborted) {
          setError(
            err instanceof Error
              ? err.message
              : "Unable to load subtasks."
          );
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      });

    return () => controller.abort();
  }, [baseUrl, refreshKey]);

  async function createSubtask(
    event: React.FormEvent<HTMLFormElement>
  ) {
    event.preventDefault();

    if (!newTitle.trim()) {
      setError("Subtask title is required.");
      return;
    }

    setCreating(true);
    setError("");
    setSuccess("");

    try {
      await apiRequest<Subtask>(baseUrl, {
        method: "POST",
        body: JSON.stringify({
          title: newTitle.trim(),
          description: newDescription.trim(),
        }),
      });

      setNewTitle("");
      setNewDescription("");
      setSuccess("Subtask created successfully.");
      setRefreshKey((value) => value + 1);
      onChanged();
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Unable to create subtask."
      );
    } finally {
      setCreating(false);
    }
  }

  async function updateStatus(
    subtask: Subtask,
    status: SubtaskStatus
  ) {
    if (subtask.status === status) return;

    setUpdatingId(subtask.subtaskId);
    setError("");
    setSuccess("");

    try {
      await apiRequest<Subtask>(
        `${baseUrl}/${subtask.subtaskId}/status`,
        {
          method: "PATCH",
          body: JSON.stringify({ status }),
        }
      );

      setSuccess("Subtask status updated.");
      setRefreshKey((value) => value + 1);
      onChanged();
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Unable to update subtask status."
      );
    } finally {
      setUpdatingId(null);
    }
  }

  const completed = subtasks.filter(
    (item) => item.status === "COMPLETED"
  ).length;

  return (
    <div
      className="rounded-xl p-4 mt-4"
      style={{
        background: "white",
        border: "1px solid #e8e7f0",
      }}
    >
      <div className="flex justify-between items-center gap-3 mb-4">
        <div>
          <strong
            style={{
              fontSize: 13.5,
              color: "#1a1830",
            }}
          >
            Subtasks
          </strong>
          <p style={{ ...mutedText, marginTop: 4 }}>
            {completed}/{subtasks.length} completed
          </p>
        </div>

        <button
          type="button"
          onClick={() => setRefreshKey((value) => value + 1)}
          style={buttonStyle}
          disabled={loading}
        >
          Refresh
        </button>
      </div>

      {error && (
        <div
          role="alert"
          className="rounded-lg p-3 mb-3"
          style={{
            background: "#fff1f2",
            color: "#b91c1c",
            fontSize: 12,
          }}
        >
          {error}
        </div>
      )}

      {success && (
        <div
          role="status"
          className="rounded-lg p-3 mb-3"
          style={{
            background: "#ecfdf5",
            color: "#047857",
            fontSize: 12,
          }}
        >
          {success}
        </div>
      )}

      {loading ? (
        <p style={mutedText}>Loading subtasks...</p>
      ) : subtasks.length === 0 ? (
        <p style={mutedText}>
          No subtasks have been created for this task.
        </p>
      ) : (
        <div className="space-y-3">
          {subtasks.map((subtask) => (
            <div
              key={subtask.subtaskId}
              className="rounded-xl p-3"
              style={{
                background: "#f7f6f3",
                border: "1px solid #e8e7f0",
              }}
            >
              <div className="flex items-start justify-between gap-3 flex-wrap">
                <div className="min-w-0 flex-1">
                  <strong
                    style={{
                      display: "block",
                      fontSize: 12.5,
                      color: "#1a1830",
                    }}
                  >
                    {subtask.title}
                  </strong>

                  {subtask.description && (
                    <p
                      style={{
                        ...mutedText,
                        marginTop: 5,
                        whiteSpace: "pre-wrap",
                      }}
                    >
                      {subtask.description}
                    </p>
                  )}

                  {subtask.assignedToName && (
                    <p
                      style={{
                        ...mutedText,
                        marginTop: 5,
                      }}
                    >
                      Assigned to: {subtask.assignedToName}
                    </p>
                  )}
                </div>

                <span
                  className="rounded-lg px-2 py-1"
                  style={{
                    fontSize: 11,
                    fontWeight: 600,
                    color: statusColor(subtask.status),
                    background:
                      `${statusColor(subtask.status)}18`,
                  }}
                >
                  {statusLabel(subtask.status)}
                </span>
              </div>

              <label
                className="block mt-3"
                style={{
                  fontSize: 11.5,
                  color: "#6b6890",
                }}
              >
                Update Status
                <select
                  aria-label={`Status for ${subtask.title}`}
                  value={subtask.status}
                  disabled={updatingId !== null}
                  onChange={(event) =>
                    updateStatus(
                      subtask,
                      event.target.value as SubtaskStatus
                    )
                  }
                  style={{
                    ...inputStyle,
                    marginTop: 6,
                    cursor: "pointer",
                  }}
                >
                  <option value="TODO">To Do</option>
                  <option value="IN_PROGRESS">In Progress</option>
                  <option value="COMPLETED">Completed</option>
                </select>
              </label>
            </div>
          ))}
        </div>
      )}

      <form
        onSubmit={createSubtask}
        className="space-y-3 mt-5"
        style={{
          borderTop: "1px solid #e8e7f0",
          paddingTop: 16,
        }}
      >
        <strong
          style={{
            fontSize: 12.5,
            color: "#1a1830",
          }}
        >
          Add Subtask
        </strong>

        <input
          type="text"
          required
          maxLength={255}
          value={newTitle}
          onChange={(event) =>
            setNewTitle(event.target.value)
          }
          placeholder="Subtask title"
          style={inputStyle}
        />

        <textarea
          rows={2}
          maxLength={5000}
          value={newDescription}
          onChange={(event) =>
            setNewDescription(event.target.value)
          }
          placeholder="Description (optional)"
          style={inputStyle}
        />

        <button
          type="submit"
          disabled={creating || !newTitle.trim()}
          style={{
            ...buttonStyle,
            opacity: creating ? 0.6 : 1,
          }}
        >
          {creating ? "Creating..." : "+ Add Subtask"}
        </button>

        <p style={mutedText}>
          Only the member assigned to this task can
          create or update its subtasks.
        </p>
      </form>
    </div>
  );
}
