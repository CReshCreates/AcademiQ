
import { useEffect, useState } from "react";
import type { CSSProperties } from "react";
import { BellIcon } from "./icons";

interface InvitationNotificationDetails {
  notificationId: number;
  notificationTitle: string;
  notificationMessage: string;
  invitationId: number;
  projectId: number;
}

interface PendingInvitation {
  invitationId: number;
  notificationDetails: InvitationNotificationDetails | null;
}

interface ProjectInvitationNotificationsProps {
  onInvitationAccepted?: () => void;
}

type InvitationAction = "ACCEPT" | "DECLINE";

const mutedText: CSSProperties = {
  fontSize: 12,
  lineHeight: 1.6,
  color: "#6b6890",
};

const lightButton: CSSProperties = {
  borderRadius: 9,
  padding: "9px 12px",
  background: "#f0eff8",
  color: "#6366f1",
  fontSize: 12,
  fontWeight: 600,
};

const primaryButton: CSSProperties = {
  borderRadius: 9,
  padding: "9px 12px",
  background: "#6366f1",
  color: "white",
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

async function parseApiError(
  response: Response
): Promise<Error> {
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

  return new Error(message);
}

async function fetchInvitations(
  signal?: AbortSignal
): Promise<PendingInvitation[]> {
  const response = await fetch(
    "/invitation/invitationNotification",
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
    throw await parseApiError(response);
  }

  const data: unknown = await response.json();

  if (!Array.isArray(data)) {
    throw new Error("Unexpected invitation response.");
  }

  return data as PendingInvitation[];
}

async function respondToInvitation(
  invitationId: number,
  projectId: number,
  action: InvitationAction
): Promise<string> {
  const response = await fetch(
    "/invitation/invitationAction",
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${getToken()}`,
        Accept: "text/plain, application/json",
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        invitationId,
        projectId,
        action,
      }),
    }
  );

  if (!response.ok) {
    throw await parseApiError(response);
  }

  return response.text();
}

export default function ProjectInvitationNotifications({
  onInvitationAccepted,
}: ProjectInvitationNotificationsProps) {
  const [open, setOpen] = useState(false);
  const [invitations, setInvitations] =
    useState<PendingInvitation[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [refreshKey, setRefreshKey] = useState(0);
  const [processingId, setProcessingId] =
    useState<number | null>(null);

  useEffect(() => {
    const controller = new AbortController();

    setLoading(true);
    setError("");

    fetchInvitations(controller.signal)
      .then((data) => {
        if (!controller.signal.aborted) {
          setInvitations(data);
        }
      })
      .catch((err) => {
        if (!controller.signal.aborted) {
          setError(
            err instanceof Error
              ? err.message
              : "Unable to load invitations."
          );
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      });

    return () => controller.abort();
  }, [refreshKey]);

  async function handleAction(
    invitation: PendingInvitation,
    action: InvitationAction
  ) {
    const projectId =
      invitation.notificationDetails?.projectId;

    if (!projectId) {
      setError(
        "This invitation has no project ID. It cannot be processed."
      );
      return;
    }

    setProcessingId(invitation.invitationId);
    setError("");
    setSuccess("");

    try {
      await respondToInvitation(
        invitation.invitationId,
        projectId,
        action
      );

      setInvitations((current) =>
        current.filter(
          (item) =>
            item.invitationId !== invitation.invitationId
        )
      );

      if (action === "ACCEPT") {
        setSuccess(
          "Invitation accepted. You are now a project team member."
        );

        onInvitationAccepted?.();
      } else {
        setSuccess("Invitation declined.");
      }

      setRefreshKey((value) => value + 1);
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Unable to respond to invitation."
      );
    } finally {
      setProcessingId(null);
    }
  }

  const count = invitations.length;

  return (
    <div className="relative flex-shrink-0">
      <button
        type="button"
        aria-label={`Project invitations: ${count} pending`}
        aria-expanded={open}
        aria-haspopup="dialog"
        onClick={() => {
        setOpen((value) => !value);

        if (!open) {
          setRefreshKey((value) => value + 1);
        }
      }}
        className="relative flex items-center justify-center rounded-xl"
        style={{
          width: 38,
          height: 38,
          background: "white",
          border: "1px solid #e8e7f0",
        }}
      >
        <BellIcon className="w-4 h-4" />

        {count > 0 && (
          <span
            className="absolute flex items-center justify-center rounded-full text-white font-bold"
            style={{
              top: -6,
              right: -6,
              minWidth: 19,
              height: 19,
              padding: "0 4px",
              background: "#ef4444",
              fontSize: 10,
              border: "2px solid white",
            }}
          >
            {count > 9 ? "9+" : count}
          </span>
        )}
      </button>

      {open && (
        <>
          <button
            type="button"
            aria-label="Close invitations"
            onClick={() => setOpen(false)}
            className="fixed inset-0"
            style={{
              zIndex: 39,
              background: "transparent",
              cursor: "default",
            }}
          />

          <div
            role="dialog"
            aria-label="Project invitations"
            className="absolute rounded-2xl overflow-hidden"
            style={{
              zIndex: 40,
              top: 48,
              right: 0,
              width: "min(390px, calc(100vw - 32px))",
              maxHeight: "min(560px, 75vh)",
              display: "flex",
              flexDirection: "column",
              background: "white",
              border: "1px solid #e8e7f0",
              boxShadow: "0 14px 45px rgba(26,24,48,.16)",
            }}
          >
            <div
              className="flex justify-between items-center gap-3 p-4"
              style={{
                borderBottom: "1px solid #e8e7f0",
              }}
            >
              <div>
                <h3
                  style={{
                    fontFamily: "var(--font-display)",
                    fontSize: 17,
                    fontWeight: 500,
                    color: "#1a1830",
                  }}
                >
                  Project Invitations
                </h3>

                <p style={mutedText}>
                  {loading
                    ? "Checking invitations..."
                    : `${count} pending invitation${count === 1 ? "" : "s"}`}
                </p>
              </div>

              <button
                type="button"
                onClick={() =>
                  setRefreshKey((value) => value + 1)
                }
                disabled={loading || processingId !== null}
                style={lightButton}
              >
                Refresh
              </button>
            </div>

            <div
              className="p-4 overflow-y-auto"
              style={{ flex: 1, minHeight: 0 }}
            >
              {error && (
                <div
                  role="alert"
                  className="rounded-xl p-3 mb-3"
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
                  className="rounded-xl p-3 mb-3"
                  style={{
                    background: "#ecfdf5",
                    color: "#047857",
                    fontSize: 12,
                  }}
                >
                  {success}
                </div>
              )}

              {loading && (
                <p style={mutedText}>
                  Loading your invitations...
                </p>
              )}

              {!loading && !error && count === 0 && (
                <div
                  className="rounded-xl p-5 text-center"
                  style={{
                    background: "#f7f6f3",
                    border: "1px dashed #c7d2fe",
                  }}
                >
                  <p
                    style={{
                      color: "#1a1830",
                      fontSize: 13,
                      fontWeight: 600,
                    }}
                  >
                    No Pending Invitations
                  </p>

                  <p style={{ ...mutedText, marginTop: 7 }}>
                    Project invitations sent to your account
                    will appear here.
                  </p>
                </div>
              )}

              {!loading && count > 0 && (
                <div className="space-y-3">
                  {invitations.map((invitation) => {
                    const details =
                      invitation.notificationDetails;

                    const busy =
                      processingId === invitation.invitationId;

                    return (
                      <div
                        key={invitation.invitationId}
                        className="rounded-xl p-4"
                        style={{
                          background: "#f7f6f3",
                          border: "1px solid #e8e7f0",
                        }}
                      >
                        <div className="flex items-center justify-between gap-2">
                          <strong
                            style={{
                              color: "#1a1830",
                              fontSize: 13,
                            }}
                          >
                            {details?.notificationTitle ||
                              "Project Invitation"}
                          </strong>

                          <span
                            className="rounded-lg px-2 py-1"
                            style={{
                              background: "#fef3c7",
                              color: "#b45309",
                              fontSize: 10.5,
                              fontWeight: 600,
                            }}
                          >
                            Pending
                          </span>
                        </div>

                        <p
                          style={{
                            ...mutedText,
                            marginTop: 10,
                            whiteSpace: "pre-wrap",
                          }}
                        >
                          {details?.notificationMessage ||
                            "You have received a project invitation."}
                        </p>

                        {details?.projectId && (
                          <p
                            style={{
                              color: "#a8a6c0",
                              fontSize: 11,
                              marginTop: 8,
                            }}
                          >
                            Project #{details.projectId}
                          </p>
                        )}

                        <div className="grid grid-cols-2 gap-2 mt-4">
                          <button
                            type="button"
                            disabled={
                              processingId !== null ||
                              !details?.projectId
                            }
                            onClick={() =>
                              handleAction(invitation, "ACCEPT")
                            }
                            style={{
                              ...primaryButton,
                              opacity:
                                processingId !== null ? 0.6 : 1,
                            }}
                          >
                            {busy
                              ? "Processing..."
                              : "Accept"}
                          </button>

                          <button
                            type="button"
                            disabled={
                              processingId !== null ||
                              !details?.projectId
                            }
                            onClick={() =>
                              handleAction(invitation, "DECLINE")
                            }
                            style={{
                              ...lightButton,
                              opacity:
                                processingId !== null ? 0.6 : 1,
                            }}
                          >
                            {busy
                              ? "Processing..."
                              : "Decline"}
                          </button>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>

            <div
              className="flex justify-end p-3"
              style={{
                borderTop: "1px solid #e8e7f0",
              }}
            >
              <button
                type="button"
                onClick={() => setOpen(false)}
                style={lightButton}
              >
                Close
              </button>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
