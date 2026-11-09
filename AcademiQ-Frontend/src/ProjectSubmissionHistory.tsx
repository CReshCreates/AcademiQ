
import { useEffect, useState } from "react";

interface SubmissionHistoryItem {
  submissionId: number;
  milestoneId: number;
  milestoneTitle: string;
  fileName: string;
  submissionVersion: number;
  submittedAt: string | null;
  status: string;
  submittedByUserId: number;
  submittedByName: string;
  feedback: string | null;
  reviewedByName: string | null;
  feedbackCreatedAt: string | null;
}

interface ProjectSubmissionHistoryProps {
  projectId: number;
  refreshKey?: number;
  onNewSubmission?: () => void;
}

const mutedText: React.CSSProperties = {
  fontSize: 12.5,
  color: "#6b6890",
  lineHeight: 1.6,
};

const lightButton: React.CSSProperties = {
  background: "#f0eff8",
  color: "#6366f1",
  fontSize: 12,
  fontWeight: 600,
  borderRadius: 10,
  padding: "10px 14px",
};

const primaryButton: React.CSSProperties = {
  background:
    "linear-gradient(135deg,#6366f1,#8b5cf6)",
  color: "white",
  fontSize: 12,
  fontWeight: 600,
  borderRadius: 10,
  padding: "10px 16px",
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

async function getSubmissionHistory(
  projectId: number,
  signal?: AbortSignal
): Promise<SubmissionHistoryItem[]> {
  const response = await fetch(
    `/submissions/students/projects/${projectId}/history`,
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
    if (response.status === 401) {
      throw new Error("Your session has expired.");
    }

    if (response.status === 403) {
      throw new Error(
        "You do not have permission to view these submissions."
      );
    }

    if (response.status === 404) {
      throw new Error(
        "Submission history endpoint or project not found."
      );
    }

    throw new Error(
      `Unable to load submissions (HTTP ${response.status}).`
    );
  }

  const data: unknown = await response.json();

  if (!Array.isArray(data)) {
    throw new Error("Unexpected submission history response.");
  }

  return data as SubmissionHistoryItem[];
}

function formatDate(value: string | null): string {
  if (!value) return "Not set";

  const date = new Date(value);

  return Number.isNaN(date.getTime())
    ? value
    : date.toLocaleString(undefined, {
        year: "numeric",
        month: "short",
        day: "numeric",
        hour: "numeric",
        minute: "2-digit",
      });
}

function formatStatus(value: string): string {
  if (!value) return "Unknown";

  return value
    .toLowerCase()
    .replace(/_/g, " ")
    .replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function getStatusColor(status: string): string {
  switch (status.toUpperCase()) {
    case "APPROVED":
    case "COMPLETED":
      return "#10b981";

    case "PENDING":
    case "SUBMITTED":
    case "UNDER_REVIEW":
      return "#f59e0b";

    case "REJECTED":
      return "#ef4444";

    case "CHANGES_REQUESTED":
    case "REVISION_REQUIRED":
      return "#8b5cf6";

    default:
      return "#6366f1";
  }
}

function StatusBadge({ status }: { status: string }) {
  const color = getStatusColor(status);

  return (
    <span
      className="inline-flex rounded-lg px-2.5 py-1 text-xs font-semibold"
      style={{
        color,
        background: color + "18",
      }}
    >
      {formatStatus(status)}
    </span>
  );
}

export default function ProjectSubmissionHistory({
  projectId,
  refreshKey = 0,
  onNewSubmission,
}: ProjectSubmissionHistoryProps) {
  const [submissions, setSubmissions] =
    useState<SubmissionHistoryItem[]>([]);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [retryKey, setRetryKey] = useState(0);

  const [openingId, setOpeningId] =
    useState<number | null>(null);

  const [pdfError, setPdfError] = useState("");

  useEffect(() => {
    const controller = new AbortController();

    setLoading(true);
    setError("");

    getSubmissionHistory(projectId, controller.signal)
      .then((data) => {
        if (controller.signal.aborted) return;

        setSubmissions(
          [...data].sort((a, b) => {
            const dateA = a.submittedAt || "";
            const dateB = b.submittedAt || "";

            return (
              dateB.localeCompare(dateA) ||
              b.submissionVersion - a.submissionVersion ||
              b.submissionId - a.submissionId
            );
          })
        );
      })
      .catch((err) => {
        if (controller.signal.aborted) return;

        setError(
          err instanceof Error
            ? err.message
            : "Unable to load submission history."
        );
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      });

    return () => controller.abort();
  }, [projectId, refreshKey, retryKey]);

  async function openSubmissionPdf(submissionId: number) {
    setPdfError("");
    setOpeningId(submissionId);

    // Open the tab during the user's click so the browser
    // does not block it after the asynchronous request.
    const pdfWindow = window.open("", "_blank");

    if (!pdfWindow) {
      setPdfError(
        "Your browser blocked the PDF tab. Allow pop-ups and try again."
      );
      setOpeningId(null);
      return;
    }

    try {
      const response = await fetch(
        `/submissions/students/getSubmissionPdf/${submissionId}`,
        {
          headers: {
            Authorization: `Bearer ${getToken()}`,
            Accept: "application/pdf",
          },
        }
      );

      if (!response.ok) {
        throw new Error(
          `Unable to open PDF (HTTP ${response.status}).`
        );
      }

      const blob = await response.blob();
      const pdfBlob = new Blob([blob], {
        type: "application/pdf",
      });

      const url = URL.createObjectURL(pdfBlob);

      pdfWindow.location.href = url;

      // Keep the URL available long enough for the
      // browser's PDF viewer to load it.
      window.setTimeout(() => {
        URL.revokeObjectURL(url);
      }, 120000);
    } catch (err) {
      pdfWindow.close();

      setPdfError(
        err instanceof Error
          ? err.message
          : "Unable to open the submitted PDF."
      );
    } finally {
      setOpeningId(null);
    }
  }

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
            Submission History
          </h3>

          <p
            style={{
              ...mutedText,
              marginTop: 5,
            }}
          >
            Uploaded milestone documents, versions,
            review decisions and supervisor feedback.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <span
            className="rounded-lg px-2.5 py-1 text-xs font-semibold"
            style={{
              background: "#f0eff8",
              color: "#6366f1",
            }}
          >
            {submissions.length} submissions
          </span>

          <button
            type="button"
            onClick={() =>
              setRetryKey((value) => value + 1)
            }
            disabled={loading}
            style={{
              ...lightButton,
              opacity: loading ? 0.6 : 1,
            }}
          >
            Refresh
          </button>
        </div>
      </div>

      {loading && (
        <div
          className="rounded-xl p-5"
          style={{ background: "#f7f6f3" }}
        >
          <p style={mutedText}>
            Loading submission history...
          </p>
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

      {!loading && !error && submissions.length === 0 && (
        <div
          className="rounded-xl p-6"
          style={{
            background: "#f7f6f3",
            border: "1px dashed #c7d2fe",
          }}
        >
          <div
            className="rounded-xl flex items-center justify-center mb-4"
            style={{
              width: 44,
              height: 44,
              background: "#f0eff8",
              color: "#6366f1",
              fontSize: 23,
            }}
          >
            ↑
          </div>

          <h4
            style={{
              fontFamily: "var(--font-display)",
              fontSize: 17,
              color: "#1a1830",
            }}
          >
            No Submissions Yet
          </h4>

          <p
            style={{
              ...mutedText,
              marginTop: 8,
            }}
          >
            No milestone PDF submissions have been recorded
            for this project.
          </p>

          {onNewSubmission && (
            <button
              type="button"
              onClick={onNewSubmission}
              style={{
                ...primaryButton,
                marginTop: 16,
              }}
            >
              Submit First Milestone
            </button>
          )}
        </div>
      )}

      {!loading && !error && submissions.length > 0 && (
        <div className="space-y-4">
          {submissions.map((item) => (
            <div
              key={item.submissionId}
              className="rounded-xl p-4 min-w-0"
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
                      fontSize: 14,
                      color: "#1a1830",
                      overflowWrap: "anywhere",
                    }}
                  >
                    {item.milestoneTitle}
                  </strong>

                  <p
                    style={{
                      ...mutedText,
                      marginTop: 5,
                      overflowWrap: "anywhere",
                    }}
                  >
                    {item.fileName}
                  </p>
                </div>

                <StatusBadge status={item.status} />
              </div>

              <div className="flex flex-wrap gap-x-5 gap-y-2 mt-4">
                <span style={mutedText}>
                  <strong style={{ color: "#1a1830" }}>
                    Version:
                  </strong>{" "}
                  {item.submissionVersion}
                </span>

                <span style={mutedText}>
                  <strong style={{ color: "#1a1830" }}>
                    Submitted:
                  </strong>{" "}
                  {formatDate(item.submittedAt)}
                </span>
              </div>

              <p
                style={{
                  ...mutedText,
                  marginTop: 8,
                }}
              >
                Submitted by:{" "}
                <strong style={{ color: "#1a1830" }}>
                  {item.submittedByName}
                </strong>
              </p>

              {item.feedback && (
                <div
                  className="rounded-xl p-3 mt-4"
                  style={{
                    background: "white",
                    border: "1px solid #e8e7f0",
                  }}
                >
                  <p
                    style={{
                      fontSize: 11,
                      textTransform: "uppercase",
                      letterSpacing: ".04em",
                      fontWeight: 600,
                      color: "#6366f1",
                    }}
                  >
                    Supervisor Feedback
                  </p>

                  <p
                    style={{
                      ...mutedText,
                      marginTop: 8,
                      whiteSpace: "pre-wrap",
                    }}
                  >
                    {item.feedback}
                  </p>

                  {(item.reviewedByName ||
                    item.feedbackCreatedAt) && (
                    <p
                      style={{
                        fontSize: 11,
                        color: "#a8a6c0",
                        marginTop: 8,
                      }}
                    >
                      {item.reviewedByName ||
                        "Supervisor"}
                      {" · "}
                      {formatDate(item.feedbackCreatedAt)}
                    </p>
                  )}
                </div>
              )}

              <div
                className="flex justify-between items-center gap-3 flex-wrap mt-4 pt-3"
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
                  Submission #{item.submissionId}
                </span>

                <button
                  type="button"
                  disabled={openingId === item.submissionId}
                  onClick={() =>
                    openSubmissionPdf(item.submissionId)
                  }
                  style={{
                    ...lightButton,
                    opacity:
                      openingId === item.submissionId
                        ? 0.6
                        : 1,
                  }}
                >
                  {openingId === item.submissionId
                    ? "Opening..."
                    : "View PDF →"}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {pdfError && (
        <div
          role="alert"
          className="rounded-xl p-3 mt-4"
          style={{
            background: "#fff1f2",
            color: "#b91c1c",
            fontSize: 12.5,
          }}
        >
          {pdfError}
        </div>
      )}
    </div>
  );
}
