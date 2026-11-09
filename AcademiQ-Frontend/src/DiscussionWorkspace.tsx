import { useEffect, useMemo, useRef, useState } from "react";
import type { FormEvent } from "react";
import { Client } from "@stomp/stompjs";
import { getMyStudentProjects, type StudentProject } from "./api/studentProjects";

type ChatMessage = {
  messageId: number;
  projectId: number;
  senderId: number;
  senderName: string;
  senderRole: string;
  content: string;
  createdAt: string;
};

type ConnectionStatus = "connecting" | "connected" | "disconnected";

function getToken(): string {
  const raw = sessionStorage.getItem("academiq_auth");
  if (!raw) throw new Error("Please sign in again.");
  try {
    const parsed: unknown = JSON.parse(raw);
    if (
      parsed && typeof parsed === "object" &&
      "token" in parsed && typeof parsed.token === "string" && parsed.token.trim()
    ) return parsed.token;
  } catch {
    throw new Error("Invalid login session.");
  }
  throw new Error("Authentication token is missing.");
}

async function getHistory(projectId: number, signal: AbortSignal): Promise<ChatMessage[]> {
  const response = await fetch(`/api/projects/${projectId}/messages`, {
    signal,
    headers: { Authorization: `Bearer ${getToken()}`, Accept: "application/json" },
  });
  if (!response.ok) {
    let reason = `Unable to load chat history (HTTP ${response.status}).`;
    try {
      const body = await response.json() as { message?: string; error?: string };
      reason = body.message || body.error || reason;
    } catch { /* Preserve HTTP error. */ }
    throw new Error(reason);
  }
  const data: unknown = await response.json();
  if (!Array.isArray(data)) throw new Error("Unexpected chat history response.");
  return data as ChatMessage[];
}

function mergeMessages(existing: ChatMessage[], incoming: ChatMessage[]): ChatMessage[] {
  const merged = new Map<number, ChatMessage>();
  for (const item of [...existing, ...incoming]) merged.set(item.messageId, item);
  return [...merged.values()].sort((a, b) =>
    a.createdAt.localeCompare(b.createdAt) || a.messageId - b.messageId
  );
}

function displayTime(value: string): string {
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? value
    : date.toLocaleString(undefined, {
        month: "short", day: "numeric", hour: "numeric", minute: "2-digit",
      });
}

export default function DiscussionWorkspace({
  initialProject = "",
}: {
  initialProject?: string;
}) {
  const [projects, setProjects] = useState<StudentProject[]>([]);
  const [projectsLoading, setProjectsLoading] = useState(true);
  const [projectsError, setProjectsError] = useState("");
  const [selected, setSelected] = useState<number | null>(null);
  const [messages, setMessages] = useState<Record<number, ChatMessage[]>>({});
  const [historyLoading, setHistoryLoading] = useState(false);
  const [historyError, setHistoryError] = useState("");
  const [connection, setConnection] = useState<ConnectionStatus>("disconnected");
  const [socketError, setSocketError] = useState("");
  const [draft, setDraft] = useState("");
  const [query, setQuery] = useState("");
  const [reload, setReload] = useState(0);
  const clientRef = useRef<Client | null>(null);
  const scrollRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    setProjectsLoading(true);
    setProjectsError("");
    getMyStudentProjects(controller.signal)
      .then((data) => {
        if (controller.signal.aborted) return;
        setProjects(data);
        setSelected((current) => {
          if (current !== null && data.some((p) => p.projectId === current)) return current;
          const preferred = Number(initialProject);
          return data.find((p) => p.projectId === preferred)?.projectId ?? data[0]?.projectId ?? null;
        });
      })
      .catch((error: unknown) => {
        if (!controller.signal.aborted) setProjectsError(
          error instanceof Error ? error.message : "Unable to load projects."
        );
      })
      .finally(() => { if (!controller.signal.aborted) setProjectsLoading(false); });
    return () => controller.abort();
  }, [initialProject]);

  useEffect(() => {
    setDraft("");
    setHistoryError("");
    setSocketError("");
    setConnection("disconnected");
    if (selected === null) return;

    const controller = new AbortController();
    let disposed = false;
    setHistoryLoading(true);

    // Keep real-time events received during the history request.
    getHistory(selected, controller.signal)
      .then((history) => {
        if (!disposed) setMessages((previous) => ({
          ...previous,
          [selected]: mergeMessages(previous[selected] ?? [], history),
        }));
      })
      .catch((error: unknown) => {
        if (!disposed) setHistoryError(
          error instanceof Error ? error.message : "Unable to load chat history."
        );
      })
      .finally(() => { if (!disposed) setHistoryLoading(false); });

    let client: Client | null = null;
    try {
      const token = getToken();
      const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
      const brokerURL = `${protocol}//${window.location.host}/ws`;
      setConnection("connecting");
      const stompClient = new Client({
        brokerURL,
        connectHeaders: { Authorization: `Bearer ${token}` },
        reconnectDelay: 5000,
        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,
        onConnect: () => {
          if (disposed) return;
          setConnection("connected");
          setSocketError("");
          stompClient.subscribe(`/topic/projects/${selected}`, (frame) => {
            try {
              const item = JSON.parse(frame.body) as ChatMessage;
              if (disposed || item.projectId !== selected || !Number.isInteger(item.messageId)) return;
              setMessages((previous) => ({
                ...previous,
                [selected]: mergeMessages(previous[selected] ?? [], [item]),
              }));
            } catch { /* Ignore malformed server frames. */ }
          });
        },
        onWebSocketClose: () => {
          if (!disposed) setConnection("disconnected");
        },
        onStompError: (frame) => {
          if (!disposed) {
            setConnection("disconnected");
            setSocketError(frame.headers.message || "Chat connection was rejected.");
          }
        },
        onWebSocketError: () => {
          if (!disposed) setSocketError("WebSocket connection failed. Check the /ws proxy.");
        },
      });
      client = stompClient;
      clientRef.current = stompClient;
      stompClient.activate();
    } catch (error) {
      setSocketError(error instanceof Error ? error.message : "Unable to connect to chat.");
    }

    return () => {
      disposed = true;
      controller.abort();
      if (clientRef.current === client) clientRef.current = null;
      if (client) void client.deactivate();
    };
  }, [selected, reload]);

  const chosen = projects.find((p) => p.projectId === selected);
  const visible = selected === null ? [] : messages[selected] ?? [];
  const filteredProjects = useMemo(() => projects.filter((project) =>
    project.projectTitle.toLowerCase().includes(query.trim().toLowerCase())
  ), [projects, query]);

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: "smooth" });
  }, [selected, visible.length]);

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const content = draft.trim();
    if (!content || content.length > 2000 || selected === null || !clientRef.current?.connected) return;
    try {
      clientRef.current.publish({
        destination: `/app/chat.send/${selected}`,
        body: JSON.stringify({ content }),
      });
      // Only append messages after the server broadcasts the persisted record.
      setDraft("");
      setSocketError("");
    } catch (error) {
      setSocketError(error instanceof Error ? error.message : "Unable to send message.");
    }
  }

  return (
    <main className="flex-1 min-w-0 flex flex-col overflow-hidden" style={{ background: "#f7f6f3" }}>
      <header className="px-8 py-4" style={{ background: "rgba(247,246,243,.94)", borderBottom: "1px solid #e8e7f0" }}>
        <div style={{ fontSize: 11, color: "#6b6890", letterSpacing: 1, textTransform: "uppercase" }}>Student workspace</div>
        <h1 style={{ fontFamily: "var(--font-display)", fontSize: 22, color: "#1a1830" }}>Discussion</h1>
      </header>

      <div className="flex flex-1 min-h-0 gap-5 px-8 py-6">
        <aside className="w-72 flex-shrink-0 rounded-2xl p-4 overflow-y-auto" style={{ background: "white", boxShadow: "0 1px 4px rgba(26,24,48,.06)" }}>
          <div className="mb-3">
            <h2 style={{ fontSize: 14, fontWeight: 700 }}>Project conversations</h2>
            <p style={{ fontSize: 11, color: "#6b6890" }}>Select a project to view its discussion.</p>
          </div>
          <input aria-label="Search project conversations" value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Search projects..." className="w-full rounded-xl p-2.5 mb-3" style={{ border: "1px solid #e8e7f0", fontSize: 12 }} />
          {projectsLoading && <p style={{ color: "#6b6890", fontSize: 12 }}>Loading projects...</p>}
          {projectsError && <p role="alert" style={{ color: "#b91c1c", fontSize: 12 }}>{projectsError}</p>}
          {!projectsLoading && !projectsError && projects.length === 0 && <p style={{ color: "#6b6890", fontSize: 12 }}>No projects found.</p>}
          {filteredProjects.map((project) => (
            <button key={project.projectId} onClick={() => setSelected(project.projectId)} type="button" className="w-full text-left rounded-xl p-3 mb-2" style={{ background: selected === project.projectId ? "#f0eff8" : "#f7f6f3", border: selected === project.projectId ? "1px solid #c7d2fe" : "1px solid transparent" }}>
              <div style={{ fontSize: 12.5, fontWeight: 600, color: selected === project.projectId ? "#4f46e5" : "#1a1830" }}>{project.projectTitle}</div>
              <div className="flex justify-between mt-2" style={{ fontSize: 10.5, color: "#6b6890" }}>
                <span>Project #{project.projectId}</span>
                <span>{(messages[project.projectId] ?? []).length} loaded</span>
              </div>
            </button>
          ))}
          <p style={{ fontSize: 11, color: "#a8a6c0", marginTop: 12 }}>Each conversation is scoped to one authorized project.</p>
        </aside>

        <section className="flex-1 min-w-0 rounded-2xl flex flex-col overflow-hidden" style={{ background: "white", boxShadow: "0 1px 4px rgba(26,24,48,.06)" }} aria-label="Project discussion">
          <div className="px-6 py-4 flex items-center justify-between gap-3" style={{ borderBottom: "1px solid #e8e7f0" }}>
            <div>
              <h2 style={{ fontFamily: "var(--font-display)", fontSize: 18, color: "#1a1830" }}>{chosen?.projectTitle ?? "Select a project"}</h2>
              <p style={{ fontSize: 11, color: "#6b6890", marginTop: 3 }}>Project discussion · {connection === "connected" ? "Live" : connection === "connecting" ? "Connecting..." : "Offline"}</p>
            </div>
            {chosen && <button type="button" onClick={() => setReload((v) => v + 1)} className="rounded-full px-3 py-1" style={{ background: "#f0eff8", color: "#6366f1", fontSize: 11 }}>Refresh / Reconnect</button>}
          </div>

          <div ref={scrollRef} className="flex-1 min-h-0 overflow-y-auto px-6 py-5 space-y-4" aria-live="polite">
            {historyLoading && <p style={{ color: "#6b6890", fontSize: 12 }}>Loading messages...</p>}
            {historyError && <p role="alert" style={{ color: "#b91c1c", fontSize: 12 }}>{historyError}</p>}
            {socketError && <p role="alert" style={{ color: "#b91c1c", fontSize: 12 }}>{socketError}</p>}
            {!chosen && <div className="text-center py-16" style={{ color: "#6b6890", fontSize: 13 }}>Select a project to start a discussion.</div>}
            {chosen && !historyLoading && !historyError && visible.length === 0 && <div className="text-center py-16" style={{ color: "#6b6890", fontSize: 13 }}>No messages yet. Start this project's discussion below.</div>}
            {visible.map((message) => (
              <div key={message.messageId} className="flex justify-start">
                <div style={{ maxWidth: "78%" }}>
                  <div style={{ fontSize: 11, color: "#6b6890", marginBottom: 5 }}>{message.senderName} · {displayTime(message.createdAt)}</div>
                  <div className="px-4 py-3 rounded-2xl" style={{ background: "#f7f6f3", color: "#3d3a5c", fontSize: 13, lineHeight: 1.6, whiteSpace: "pre-wrap", overflowWrap: "anywhere" }}>{message.content}</div>
                </div>
              </div>
            ))}
          </div>

          <form className="px-5 py-4" style={{ borderTop: "1px solid #e8e7f0" }} onSubmit={submit}>
            <label htmlFor="discussion-draft" className="sr-only">Your message</label>
            <div className="flex items-end gap-3">
              <textarea id="discussion-draft" rows={2} maxLength={2000} disabled={!chosen || connection !== "connected"} value={draft} onChange={(e) => setDraft(e.target.value)} onKeyDown={(e) => { if (e.key === "Enter" && !e.shiftKey) { e.preventDefault(); e.currentTarget.form?.requestSubmit(); } }} placeholder={chosen ? `Write a message to ${chosen.projectTitle}...` : "Select a project..."} className="flex-1 min-w-0 rounded-xl px-4 py-3 resize-none" style={{ border: "1px solid #e8e7f0", fontSize: 13 }} />
              <button type="submit" disabled={!draft.trim() || connection !== "connected"} className="rounded-xl px-5 py-3 text-white" style={{ background: "#6366f1", fontSize: 13, opacity: draft.trim() && connection === "connected" ? 1 : 0.45 }}>Send</button>
            </div>
            <p style={{ fontSize: 10.5, color: "#a8a6c0", marginTop: 9 }}>Messages are saved in MySQL. Enter to send, Shift+Enter for a new line. Maximum 2,000 characters.</p>
          </form>
        </section>
      </div>
    </main>
  );
}
