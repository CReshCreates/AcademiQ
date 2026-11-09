import { useEffect, useMemo, useState } from "react";
import type { FormEvent, ReactNode } from "react";

import {
  getMyStudentProjects,
  type StudentProject,
} from "./api/studentProjects";

import ProjectTaskSubtasks from "./ProjectTaskSubtasks";
import ProjectSubmissionHistory from "./ProjectSubmissionHistory";

// UI-only data model. Replace with authenticated API responses during integration.
type Project = { id: string; title: string; subject: string; stage: string };
type Member = { id: number; name: string; responsibility: string; leader?: boolean };
type Subtask = { id: number; title: string; completed: boolean };
type Status = "TODO" | "IN_PROGRESS" | "UNDER_REVIEW" | "COMPLETED";
type Task = { id: number; projectId: string; title: string; description: string; memberId: number; priority: "LOW" | "MEDIUM" | "HIGH"; status: Status; deadline: string; subtasks: Subtask[] };
type Submission = { id: number; projectId: string; milestone: string; fileName: string; version: number; status: "Submitted" | "Under Review" | "Approved" | "Revision Required"; date: string; feedback?: string };

const projects: Project[] = [
  { id: "network", title: "AI-Based Network Monitoring System", subject: "Major Project", stage: "Implementation" },
  { id: "rental", title: "Smart Rental Platform", subject: "Software Engineering", stage: "System design" },
  { id: "market", title: "Campus Marketplace", subject: "Web Application Project", stage: "Proposal" },
];
const initialTeams: Record<string, Member[]> = {
  network: [{ id: 1, name: "Resh Raj", responsibility: "Backend and API development", leader: true }, { id: 2, name: "Anish K.", responsibility: "Frontend development" }, { id: 3, name: "Suman T.", responsibility: "Testing and documentation" }],
  rental: [{ id: 1, name: "Resh Raj", responsibility: "Project coordination", leader: true }, { id: 4, name: "Kabita S.", responsibility: "UI and research" }],
  market: [{ id: 1, name: "Resh Raj", responsibility: "Project leader", leader: true }],
};
const initialTasks: Task[] = [
  { id: 101, projectId: "network", title: "Implement JWT authentication", description: "Protect authenticated backend endpoints.", memberId: 1, priority: "HIGH", deadline: "2026-11-18", status: "UNDER_REVIEW", subtasks: [{id:1,title:"Create login flow",completed:true},{id:2,title:"Add authorization filter",completed:true}] },
  { id: 102, projectId: "network", title: "Design dashboard components", description: "Build dashboard cards and responsive layout.", memberId: 2, priority: "MEDIUM", deadline: "2026-11-25", status: "IN_PROGRESS", subtasks: [{id:3,title:"Create components",completed:true},{id:4,title:"Connect API",completed:false}] },
  { id: 103, projectId: "network", title: "Write integration tests", description: "Test API workflows.", memberId: 3, priority: "HIGH", deadline: "2026-12-04", status: "TODO", subtasks: [] },
  { id: 104, projectId: "rental", title: "Create system design", description: "Prepare ERD and architecture.", memberId: 1, priority: "MEDIUM", deadline: "2026-11-21", status: "IN_PROGRESS", subtasks: [{id:5,title:"Draft ERD",completed:true},{id:6,title:"Review diagrams",completed:false}] },
  { id: 105, projectId: "market", title: "Prepare proposal", description: "Write project scope and objectives.", memberId: 1, priority: "HIGH", deadline: "2026-11-08", status: "TODO", subtasks: [] },
];
const initialSubmissions: Submission[] = [
 {id:11,projectId:"network",milestone:"Project Proposal",fileName:"proposal-v1.pdf",version:1,status:"Approved",date:"2026-09-15",feedback:"Approved. Proceed to design."},
 {id:12,projectId:"network",milestone:"System Design",fileName:"system-design-v2.pdf",version:2,status:"Under Review",date:"2026-10-06"},
 {id:13,projectId:"rental",milestone:"Requirements Analysis",fileName:"requirements-v1.pdf",version:1,status:"Revision Required",date:"2026-10-02",feedback:"Expand functional requirements."},
];
const stages: {id:Status;label:string;color:string}[] = [{id:"TODO",label:"To Do",color:"#6b6890"},{id:"IN_PROGRESS",label:"In Progress",color:"#6366f1"},{id:"UNDER_REVIEW",label:"Under Review",color:"#f59e0b"},{id:"COMPLETED",label:"Completed",color:"#10b981"}];
const inputStyle: React.CSSProperties = {border:"1px solid #e8e7f0",background:"white",borderRadius:12,padding:"10px 13px",width:"100%",color:"#1a1830",fontSize:13};
const muted: React.CSSProperties = {color:"#6b6890",fontSize:12};
const brandButton: React.CSSProperties = {background:"linear-gradient(135deg,#6366f1,#8b5cf6)",color:"white",fontWeight:600,fontSize:13,borderRadius:12,padding:"10px 16px"};
const lightButton: React.CSSProperties = {background:"#f0eff8",color:"#4f46e5",fontWeight:600,fontSize:12,borderRadius:10,padding:"9px 13px"};
const panel: React.CSSProperties = {background:"white",borderRadius:18,padding:20,boxShadow:"0 1px 4px rgba(26,24,48,.06)"};
function Tag({children,color="#6366f1"}:{children:ReactNode;color?:string}){return <span style={{background:color+"18",color,borderRadius:30,padding:"4px 10px",fontSize:11,fontWeight:650,whiteSpace:"nowrap"}}>{children}</span>}
function Header({title,subtitle,action,onAction}:{title:string;subtitle:string;action?:string;onAction?:()=>void}){return <header className="sticky top-0 z-10 flex items-center justify-between px-8 py-4" style={{background:"rgba(247,246,243,.94)",borderBottom:"1px solid #e8e7f0"}}><div><div style={{...muted,fontSize:11,letterSpacing:1,textTransform:"uppercase"}}>{subtitle}</div><h1 style={{fontFamily:"var(--font-display)",fontSize:22}}>{title}</h1></div>{action&&<button type="button" onClick={onAction} style={brandButton}>+ {action}</button>}</header>}
function ProjectPicker({value,onChange,allowAll=false}:{value:string;onChange:(v:string)=>void;allowAll?:boolean}){return <label className="flex flex-col gap-1" style={{minWidth:240}}><span style={{...muted,fontWeight:600}}>Project</span><select aria-label="Select project" style={inputStyle} value={value} onChange={e=>onChange(e.target.value)}>{allowAll&&<option value="all">All projects</option>}{projects.map(p=><option key={p.id} value={p.id}>{p.title}</option>)}</select></label>}
function Modal({title,onClose,children}:{title:string;onClose:()=>void;children:ReactNode}){return <div className="fixed inset-0 z-50 flex items-center justify-center p-4" style={{background:"rgba(20,18,40,.5)"}} onMouseDown={onClose}><section role="dialog" aria-modal="true" aria-label={title} onMouseDown={e=>e.stopPropagation()} className="w-full max-w-xl max-h-[90vh] overflow-y-auto" style={panel}><div className="flex justify-between items-center mb-5"><h2 style={{fontFamily:"var(--font-display)",fontSize:21}}>{title}</h2><button onClick={onClose} aria-label="Close" style={lightButton}>✕</button></div>{children}</section></div>}
function Empty({text}:{text:string}){return <div style={{...muted,textAlign:"center",padding:"34px 10px"}}>{text}</div>}
const memberName=(projectId:string,memberId:number)=>initialTeams[projectId]?.find(m=>m.id===memberId)?.name ?? "Project member";
const pct=(task:Task)=>task.subtasks.length===0?0:Math.round(task.subtasks.filter(x=>x.completed).length*100/task.subtasks.length);


type LiveTask = {
  taskId: number;
  projectId: number;
  projectTitle: string;
  title: string;
  description: string | null;
  status: string;
  priority: string;
  deadline: string | null;
  progress: number | null;
  milestoneId: number | null;
  assignedMemberId?: number | null;
  assignedMemberName?: string | null;
  createdAt?: string | null;
};

type TaskScope = "mine" | "team";

const liveTaskStages = [
  {
    id: "TODO",
    label: "To Do",
    color: "#6b6890",
  },
  {
    id: "IN_PROGRESS",
    label: "In Progress",
    color: "#6366f1",
  },
  {
    id: "UNDER_REVIEW",
    label: "Under Review",
    color: "#f59e0b",
  },
  {
    id: "COMPLETED",
    label: "Completed",
    color: "#10b981",
  },
];

function liveTaskStatus(status: string): string {
  const normalized = (status || "").toUpperCase();

  if (
    normalized === "PENDING" ||
    normalized === "TODO" ||
    normalized === "TO_DO"
  ) {
    return "TODO";
  }

  if (normalized === "IN_PROGRESS") {
    return "IN_PROGRESS";
  }

  if (normalized === "UNDER_REVIEW") {
    return "UNDER_REVIEW";
  }

  if (
    normalized === "COMPLETED" ||
    normalized === "DONE"
  ) {
    return "COMPLETED";
  }

  return "OTHER";
}

function liveTaskProgress(value: number | null): number {
  if (typeof value !== "number" || !Number.isFinite(value)) {
    return 0;
  }

  return Math.min(100, Math.max(0, value));
}

function liveTaskPriorityColor(priority: string): string {
  switch (priority?.toUpperCase()) {
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

function liveTaskToken(): string {
  const raw = sessionStorage.getItem("academiq_auth");

  if (!raw) {
    throw new Error("Please sign in again.");
  }

  let parsed: { token?: unknown };

  try {
    parsed = JSON.parse(raw);
  } catch {
    throw new Error("Invalid login session.");
  }

  if (
    typeof parsed?.token !== "string" ||
    !parsed.token.trim()
  ) {
    throw new Error("Authentication token is missing.");
  }

  return parsed.token;
}

async function liveTaskRequest<T>(
  url: string,
  signal?: AbortSignal
): Promise<T> {
  const response = await fetch(url, {
    method: "GET",
    signal,
    headers: {
      Authorization: `Bearer ${liveTaskToken()}`,
      Accept: "application/json",
    },
  });

  if (!response.ok) {
    const body = await response.text();

    let message = `Request failed (HTTP ${response.status}).`;

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

  const result: unknown = await response.json();

  if (!Array.isArray(result)) {
    throw new Error("Unexpected tasks API response.");
  }

  return result as T;
}

type TaskCreationMilestone = { milestoneId: number; title: string; status: string };
type TaskCreationTeam = { projectId: number; members: { memberId: number; fullName: string }[] };

function currentLocalDate(): string {
  const now = new Date();
  const local = new Date(now.getTime() - now.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 10);
}

async function taskCreationRequest<T>(url: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(url, {
    ...options,
    headers: {
      Authorization: `Bearer ${liveTaskToken()}`,
      Accept: "application/json",
      ...(options.body ? { "Content-Type": "application/json" } : {}),
      ...options.headers,
    },
  });
  if (!response.ok) {
    const body = await response.text();
    let detail = `Request failed (HTTP ${response.status}).`;
    try {
      const parsed = JSON.parse(body);
      detail = parsed.message || parsed.error || detail;
    } catch {
      if (body && !body.trim().startsWith("<")) detail = body;
    }
    throw new Error(detail);
  }
  return response.json() as Promise<T>;
}

export function TasksWorkspace({
  initialProject = "all",
  initialAction = "",
  onActionConsumed,
  onOpenProject,
}: {
  initialProject?: string;
  initialAction?: string;
  onActionConsumed?: () => void;
  onOpenProject?: (projectId: number) => void;
}) {
  const [projectList, setProjectList] =
    useState<StudentProject[]>([]);

  const [project, setProject] = useState(
    initialProject !== "all" &&
      Number.isInteger(Number(initialProject)) &&
      Number(initialProject) > 0
      ? initialProject
      : "all"
  );

  const [scope, setScope] = useState<TaskScope>(
    initialAction ? "team" : "mine"
  );

  const [search, setSearch] = useState("");
  const [items, setItems] = useState<LiveTask[]>([]);
  const [selected, setSelected] = useState<number | null>(null);

  const [showCreate, setShowCreate] = useState(
    initialAction === "create"
  );

  const [createProjectId, setCreateProjectId] =
    useState(
      initialProject !== "all" &&
        Number(initialProject) > 0
        ? initialProject
        : ""
    );

  const [createMilestones, setCreateMilestones] = useState<TaskCreationMilestone[]>([]);
  const [createMembers, setCreateMembers] = useState<TaskCreationTeam["members"]>([]);
  const [createLoading, setCreateLoading] = useState(false);
  const [createSaving, setCreateSaving] = useState(false);
  const [createError, setCreateError] = useState("");
  const [createSuccess, setCreateSuccess] = useState("");
  const [createTaskForm, setCreateTaskForm] = useState({
    title: "", description: "", priority: "MEDIUM", deadline: "", milestoneId: 0, memberId: 0,
  });

  useEffect(() => {
    if (!showCreate || !createProjectId) {
      setCreateMilestones([]);
      setCreateMembers([]);
      return;
    }
    const controller = new AbortController();
    setCreateLoading(true);
    setCreateError("");
    setCreateMilestones([]);
    setCreateMembers([]);
    setCreateTaskForm((old) => ({ ...old, milestoneId: 0, memberId: 0 }));

    Promise.all([
      taskCreationRequest<TaskCreationMilestone[]>(
        `/api/projects/${createProjectId}/milestones`, { signal: controller.signal }
      ),
      taskCreationRequest<TaskCreationTeam[]>(
        "/team/my-project-teams", { signal: controller.signal }
      ),
    ]).then(([milestones, teams]) => {
      if (controller.signal.aborted) return;
      setCreateMilestones(milestones);
      setCreateMembers(teams.find((team) => String(team.projectId) === createProjectId)?.members ?? []);
    }).catch((error) => {
      if (!controller.signal.aborted) setCreateError(
        error instanceof Error ? error.message : "Could not load task form options."
      );
    }).finally(() => {
      if (!controller.signal.aborted) setCreateLoading(false);
    });
    return () => controller.abort();
  }, [showCreate, createProjectId]);

  async function submitCreatedTask(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!createProjectId || createSaving) return;
    setCreateError("");
    setCreateSuccess("");
    if (!createTaskForm.title.trim() || !createTaskForm.description.trim()
        || !createTaskForm.deadline || !createTaskForm.milestoneId || !createTaskForm.memberId) {
      setCreateError("Please fill in all task fields.");
      return;
    }
    setCreateSaving(true);
    try {
      await taskCreationRequest<unknown>(`/api/projects/${createProjectId}/tasks`, {
        method: "POST",
        body: JSON.stringify({
          title: createTaskForm.title.trim(),
          description: createTaskForm.description.trim(),
          priority: createTaskForm.priority,
          deadline: createTaskForm.deadline,
          milestoneId: createTaskForm.milestoneId,
          memberId: createTaskForm.memberId,
        }),
      });
      setCreateSuccess("Task created successfully.");
      setCreateTaskForm({title: "", description: "", priority: "MEDIUM", deadline: "", milestoneId: 0, memberId: 0});
      setScope("team");
      setRefreshKey((value) => value + 1);
    } catch (error) {
      setCreateError(error instanceof Error ? error.message : "Could not create task.");
    } finally {
      setCreateSaving(false);
    }
  }

  const [projectsLoading, setProjectsLoading] =
    useState(true);

  const [tasksLoading, setTasksLoading] =
    useState(false);

  const [error, setError] = useState("");
  const [warning, setWarning] = useState("");
  const [refreshKey, setRefreshKey] = useState(0);

  useEffect(() => {
    const controller = new AbortController();

    setProjectsLoading(true);
    setError("");

    getMyStudentProjects(controller.signal)
      .then((data) => {
        if (!controller.signal.aborted) {
          setProjectList(data);
        }
      })
      .catch((err) => {
        if (!controller.signal.aborted) {
          setError(
            err instanceof Error
              ? err.message
              : "Unable to load your projects."
          );
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setProjectsLoading(false);
        }
      });

    return () => controller.abort();
  }, [refreshKey]);

  useEffect(() => {
    if (projectsLoading || error) return;

    const controller = new AbortController();

    async function loadTasks() {
      setTasksLoading(true);
      setWarning("");

      try {
        let result: LiveTask[] = [];

        if (scope === "mine") {
          result = await liveTaskRequest<LiveTask[]>(
            "/api/tasks/assigned-to-me",
            controller.signal
          );
        } else {
          const selectedProjects =
            project === "all"
              ? projectList
              : projectList.filter(
                  (item) =>
                    String(item.projectId) === project
                );

          const responses = await Promise.allSettled(
            selectedProjects.map((item) =>
              liveTaskRequest<LiveTask[]>(
                `/api/projects/${item.projectId}/tasks`,
                controller.signal
              )
            )
          );

          let failed = 0;

          result = responses.flatMap((response, index) => {
            if (response.status !== "fulfilled") {
              failed++;
              return [];
            }

            const source = selectedProjects[index];

            return response.value.map((task) => ({
              ...task,
              projectId: source.projectId,
              projectTitle: source.projectTitle,
            }));
          });

          if (!controller.signal.aborted && failed > 0) {
            setWarning(
              `Tasks could not be loaded for ${failed} project(s).`
            );
          }
        }

        if (!controller.signal.aborted) {
          setItems(result);
        }
      } catch (err) {
        if (!controller.signal.aborted) {
          setItems([]);
          setWarning(
            err instanceof Error
              ? err.message
              : "Unable to load tasks."
          );
        }
      } finally {
        if (!controller.signal.aborted) {
          setTasksLoading(false);
        }
      }
    }

    loadTasks();

    return () => controller.abort();
  }, [
    scope,
    project,
    projectList,
    projectsLoading,
    error,
  ]);

  const visible = useMemo(() => {
    const query = search.trim().toLowerCase();

    return items
      .filter(
        (task) =>
          project === "all" ||
          String(task.projectId) === project
      )
      .filter((task) => {
        if (!query) return true;

        return (
          task.title.toLowerCase().includes(query) ||
          (task.description || "")
            .toLowerCase()
            .includes(query) ||
          (task.assignedMemberName || "")
            .toLowerCase()
            .includes(query) ||
          (task.projectTitle || "")
            .toLowerCase()
            .includes(query)
        );
      })
      .sort((a, b) => {
        if (!a.deadline) return 1;
        if (!b.deadline) return -1;
        return a.deadline.localeCompare(b.deadline);
      });
  }, [items, project, search]);

  const chosen =
    visible.find((task) => task.taskId === selected) ??
    items.find((task) => task.taskId === selected);

  function refreshTasks() {
    setSelected(null);
    setRefreshKey((value) => value + 1);
  }

  function openCreate() {
    setCreateProjectId(
      project !== "all"
        ? project
        : projectList.length === 1
        ? String(projectList[0].projectId)
        : ""
    );

    setCreateError("");
    setCreateSuccess("");
    setCreateTaskForm({title: "", description: "", priority: "MEDIUM", deadline: "", milestoneId: 0, memberId: 0});
    setShowCreate(true);
  }

  const displayStages = [
    ...liveTaskStages,
    ...(visible.some(
      (task) => liveTaskStatus(task.status) === "OTHER"
    )
      ? [
          {
            id: "OTHER",
            label: "Other",
            color: "#ef4444",
          },
        ]
      : []),
  ];

  return (
    <main
      className="flex-1 overflow-y-auto"
      style={{
        minWidth: 0,
        background: "#f7f6f3",
      }}
    >
      <Header
        title="Tasks"
        subtitle="My academic workload"
        action="Create Task"
        onAction={openCreate}
      />

      <div className="px-8 py-6 space-y-5">
        <div
          style={panel}
          className="flex flex-wrap gap-4 items-end"
        >
          <label
            className="flex flex-col gap-1"
            style={{ minWidth: 240 }}
          >
            <span style={{ ...muted, fontWeight: 600 }}>
              Project
            </span>

            <select
              aria-label="Select project"
              style={inputStyle}
              value={project}
              onChange={(event) => {
                setProject(event.target.value);
                setSelected(null);
              }}
            >
              <option value="all">All projects</option>

              {projectList.map((item) => (
                <option
                  key={item.projectId}
                  value={String(item.projectId)}
                >
                  {item.projectTitle}
                </option>
              ))}
            </select>
          </label>

          <label className="flex flex-col gap-1">
            <span style={{ ...muted, fontWeight: 600 }}>
              View
            </span>

            <select
              style={inputStyle}
              value={scope}
              onChange={(event) =>
                setScope(event.target.value as TaskScope)
              }
            >
              <option value="mine">Assigned to me</option>
              <option value="team">All team tasks</option>
            </select>
          </label>

          <label
            className="flex flex-col gap-1 flex-1"
            style={{ minWidth: 180 }}
          >
            <span style={{ ...muted, fontWeight: 600 }}>
              Search tasks
            </span>

            <input
              style={inputStyle}
              placeholder="Search by task title"
              value={search}
              onChange={(event) =>
                setSearch(event.target.value)
              }
            />
          </label>

          <button
            type="button"
            style={lightButton}
            onClick={refreshTasks}
            disabled={projectsLoading || tasksLoading}
          >
            Refresh
          </button>
        </div>

        <div className="flex flex-wrap gap-3 items-center">
          <Tag>{visible.length} tasks</Tag>

          <span style={muted}>
            {scope === "mine"
              ? "Tasks assigned to your account."
              : "Tasks across the selected project teams."}
          </span>
        </div>

        {(projectsLoading || tasksLoading) && (
          <div style={panel}>
            <span style={muted}>Loading tasks...</span>
          </div>
        )}

        {error && (
          <div
            role="alert"
            style={{
              ...panel,
              color: "#b91c1c",
            }}
          >
            {error}
          </div>
        )}

        {warning && (
          <div
            role="alert"
            style={{
              ...panel,
              color: "#b45309",
            }}
          >
            {warning}
          </div>
        )}

        {!projectsLoading &&
          !tasksLoading &&
          !error &&
          projectList.length === 0 && (
            <div style={panel}>
              <Empty text="No projects are associated with your account." />
            </div>
          )}

        {!projectsLoading && !tasksLoading && !error && (
          <div
            className={`grid grid-cols-1 md:grid-cols-2 ${
              displayStages.length > 4
                ? "xl:grid-cols-5"
                : "xl:grid-cols-4"
            } gap-4`}
          >
            {displayStages.map((stage) => {
              const stageTasks = visible.filter(
                (task) =>
                  liveTaskStatus(task.status) === stage.id
              );

              return (
                <section
                  key={stage.id}
                  style={{
                    background: "#efeee9",
                    borderRadius: 16,
                    padding: 14,
                    minHeight: 310,
                  }}
                >
                  <div className="flex justify-between items-center mb-4">
                    <b style={{ fontSize: 13 }}>
                      {stage.label}
                    </b>

                    <Tag color={stage.color}>
                      {stageTasks.length}
                    </Tag>
                  </div>

                  <div className="space-y-3">
                    {stageTasks.map((task) => {
                      const progress = liveTaskProgress(
                        task.progress
                      );

                      return (
                        <button
                          type="button"
                          key={`${task.projectId}-${task.taskId}`}
                          onClick={() =>
                            setSelected(task.taskId)
                          }
                          className="w-full text-left"
                          style={{
                            background: "white",
                            borderRadius: 13,
                            padding: 14,
                            boxShadow:
                              "0 1px 3px rgba(26,24,48,.05)",
                          }}
                        >
                          <div className="flex justify-between gap-2">
                            <Tag
                              color={liveTaskPriorityColor(
                                task.priority
                              )}
                            >
                              {task.priority}
                            </Tag>

                            <span
                              style={{
                                ...muted,
                                fontSize: 10,
                              }}
                            >
                              {task.deadline || "No deadline"}
                            </span>
                          </div>

                          <b
                            className="block mt-3"
                            style={{ fontSize: 13 }}
                          >
                            {task.title}
                          </b>

                          <p
                            style={{
                              ...muted,
                              marginTop: 6,
                            }}
                          >
                            {task.projectTitle}
                          </p>

                          <p
                            style={{
                              ...muted,
                              marginTop: 5,
                            }}
                          >
                            Assigned:{" "}
                            {task.assignedMemberName ||
                              "Not available"}
                          </p>

                          <div
                            className="flex justify-between mt-4"
                            style={{
                              ...muted,
                              fontSize: 11,
                            }}
                          >
                            <span>Task progress</span>
                            <b>{progress}%</b>
                          </div>

                          <div
                            style={{
                              height: 5,
                              background: "#e8e7f0",
                              borderRadius: 4,
                              marginTop: 5,
                              overflow: "hidden",
                            }}
                          >
                            <div
                              style={{
                                width: `${progress}%`,
                                height: "100%",
                                background: stage.color,
                              }}
                            />
                          </div>
                        </button>
                      );
                    })}

                    {stageTasks.length === 0 && (
                      <Empty text="No tasks" />
                    )}
                  </div>
                </section>
              );
            })}
          </div>
        )}
      </div>

      {chosen && (
        <Modal
          title={chosen.title}
          onClose={() => setSelected(null)}
        >
          <div className="flex flex-wrap gap-2 mb-4">
            <Tag>{chosen.projectTitle}</Tag>

            <Tag
              color={
                liveTaskStages.find(
                  (stage) =>
                    stage.id === liveTaskStatus(chosen.status)
                )?.color || "#6b6890"
              }
            >
              {chosen.status.replace(/_/g, " ")}
            </Tag>

            <Tag>
              {liveTaskProgress(chosen.progress)}% complete
            </Tag>
          </div>

          <p style={muted}>
            {chosen.description || "No description provided."}
          </p>

          <div
            className="grid grid-cols-2 gap-3 mt-4 mb-5"
            style={muted}
          >
            <div>
              Assigned to:{" "}
              <b>
                {chosen.assignedMemberName || "Not available"}
              </b>
            </div>

            <div>
              Deadline:{" "}
              <b>{chosen.deadline || "Not set"}</b>
            </div>

            <div>
              Priority: <b>{chosen.priority}</b>
            </div>

            <div>
              Milestone:{" "}
              <b>
                {chosen.milestoneId == null
                  ? "Not linked"
                  : `#${chosen.milestoneId}`}
              </b>
            </div>
          </div>

          <h3 className="font-semibold mb-3">
            Manage Subtasks
          </h3>

          <ProjectTaskSubtasks
            projectId={chosen.projectId}
            taskId={chosen.taskId}
            onChanged={refreshTasks}
          />

          <div className="flex justify-end mt-5">
            <button
              type="button"
              style={lightButton}
              onClick={() => {
                setSelected(null);
                onOpenProject?.(chosen.projectId);
              }}
              disabled={!onOpenProject}
            >
              Open Project →
            </button>
          </div>
        </Modal>
      )}

      {showCreate && (
        <Modal
          title="Create Task"
          onClose={() => {
            if (createSaving) return;
            setShowCreate(false);
            onActionConsumed?.();
          }}
        >
          <form onSubmit={submitCreatedTask} className="space-y-4">
            <label className="flex flex-col gap-2">
              <span style={{...muted, fontWeight:600}}>Project</span>
              <select style={inputStyle} required value={createProjectId}
                disabled={createSaving} onChange={(event) => {
                  setCreateProjectId(event.target.value);
                  setCreateSuccess("");
                }}>
                <option value="">Select project</option>
                {projectList.map((item) => (
                  <option key={item.projectId} value={String(item.projectId)}>{item.projectTitle}</option>
                ))}
              </select>
            </label>

            {createProjectId && (
              <>
                <label className="flex flex-col gap-2">
                  <span style={{...muted, fontWeight:600}}>Task Title</span>
                  <input style={inputStyle} required maxLength={255}
                    disabled={createSaving} value={createTaskForm.title}
                    onChange={(event) => setCreateTaskForm((old) => ({...old, title: event.target.value}))}
                    placeholder="Enter task title" />
                </label>
                <label className="flex flex-col gap-2">
                  <span style={{...muted, fontWeight:600}}>Description</span>
                  <textarea style={inputStyle} required rows={3}
                    disabled={createSaving} value={createTaskForm.description}
                    onChange={(event) => setCreateTaskForm((old) => ({...old, description: event.target.value}))}
                    placeholder="Describe the task" />
                </label>
                <div className="grid grid-cols-2 gap-3">
                  <label className="flex flex-col gap-2">
                    <span style={{...muted, fontWeight:600}}>Priority</span>
                    <select style={inputStyle} value={createTaskForm.priority} disabled={createSaving}
                      onChange={(event) => setCreateTaskForm((old) => ({...old, priority: event.target.value}))}>
                      <option value="LOW">Low</option>
                      <option value="MEDIUM">Medium</option>
                      <option value="HIGH">High</option>
                    </select>
                  </label>
                  <label className="flex flex-col gap-2">
                    <span style={{...muted, fontWeight:600}}>Deadline</span>
                    <input type="date" style={inputStyle} required min={currentLocalDate()}
                      value={createTaskForm.deadline} disabled={createSaving}
                      onChange={(event) => setCreateTaskForm((old) => ({...old, deadline: event.target.value}))} />
                  </label>
                </div>
                <label className="flex flex-col gap-2">
                  <span style={{...muted, fontWeight:600}}>Milestone</span>
                  <select style={inputStyle} required value={createTaskForm.milestoneId}
                    disabled={createLoading || createSaving}
                    onChange={(event) => setCreateTaskForm((old) => ({...old, milestoneId: Number(event.target.value)}))}>
                    <option value={0}>Select milestone</option>
                    {createMilestones.map((item) => (
                      <option key={item.milestoneId} value={item.milestoneId}>{item.title}</option>
                    ))}
                  </select>
                </label>
                <label className="flex flex-col gap-2">
                  <span style={{...muted, fontWeight:600}}>Assign To</span>
                  <select style={inputStyle} required value={createTaskForm.memberId}
                    disabled={createLoading || createSaving}
                    onChange={(event) => setCreateTaskForm((old) => ({...old, memberId: Number(event.target.value)}))}>
                    <option value={0}>Select team member</option>
                    {createMembers.map((item) => (
                      <option key={item.memberId} value={item.memberId}>{item.fullName}</option>
                    ))}
                  </select>
                </label>
                {createLoading && <p style={muted}>Loading milestones and team members...</p>}
                {!createLoading && createMilestones.length === 0 && !createError && (
                  <p style={muted}>No milestones are available for this project.</p>
                )}
                {!createLoading && createMembers.length === 0 && !createError && (
                  <p style={muted}>No team members are available for assignment.</p>
                )}
              </>
            )}

            {createError && <p role="alert" style={{...muted,color:"#b91c1c"}}>{createError}</p>}
            {createSuccess && <p role="status" style={{...muted,color:"#047857"}}>{createSuccess}</p>}
            <div className="flex justify-end gap-2 mt-5">
              <button type="button" style={lightButton} disabled={createSaving}
                onClick={() => {setShowCreate(false); onActionConsumed?.();}}>
                {createSuccess ? "Done" : "Cancel"}
              </button>
              <button type="submit" style={{...brandButton, opacity: createLoading || createSaving || !createProjectId ? 0.5 : 1}}
                disabled={!createProjectId || createLoading || createSaving}>
                {createSaving ? "Creating..." : "Create Task"}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </main>
  );
}



type LiveTeamMember = {
  memberId: number;
  userId: number;
  fullName: string;
  role: string;
};

type LiveTeamSummary = {
  projectId: number;
  memberCount: number;
  maxMembers: number;
  pendingInvitationCount: number;
  availableSlots: number;
  isLeader: boolean;
  canInvite: boolean;
  members: LiveTeamMember[];
};

type LiveEligibleStudent = {
  userId: number;
  fullName: string;
  email: string;
};

async function teamApiRequest<T>(
  url: string,
  options: RequestInit = {}
): Promise<T> {
  const response = await fetch(url, {
    ...options,
    headers: {
      Authorization: `Bearer ${liveTaskToken()}`,
      Accept: "application/json",
      ...(options.body
        ? { "Content-Type": "application/json" }
        : {}),
      ...options.headers,
    },
  });

  if (!response.ok) {
    const text = await response.text();
    let message = `Request failed (HTTP ${response.status}).`;

    try {
      const parsed = JSON.parse(text);
      message = parsed.message || parsed.error || message;
    } catch {
      if (text && !text.trim().startsWith("<")) {
        message = text;
      }
    }

    if (response.status === 401) {
      message = "Your session has expired. Please log in again.";
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

function teamMemberInitials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);

  if (parts.length === 0) return "AQ";

  if (parts.length === 1) {
    return parts[0].slice(0, 2).toUpperCase();
  }

  return (
    parts[0][0] + parts[parts.length - 1][0]
  ).toUpperCase();
}

export function TeamWorkspace({
  initialProject = "",
  initialAction = "",
  onActionConsumed,
}: {
  initialProject?: string;
  initialAction?: string;
  onActionConsumed?: () => void;
}) {
  const [projectList, setProjectList] =
    useState<StudentProject[]>([]);

  const [teams, setTeams] =
    useState<LiveTeamSummary[]>([]);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [refreshKey, setRefreshKey] = useState(0);

  // All groups are collapsed by default.
  const [expandedProjects, setExpandedProjects] =
    useState<number[]>([]);

  const [inviteOpen, setInviteOpen] = useState(
    initialAction === "invite"
  );

  const [inviteProjectId, setInviteProjectId] = useState(
    Number(initialProject) > 0
      ? Number(initialProject)
      : 0
  );

  const [studentQuery, setStudentQuery] = useState("");
  const [eligibleStudents, setEligibleStudents] =
    useState<LiveEligibleStudent[]>([]);

  const [selectedStudent, setSelectedStudent] =
    useState<LiveEligibleStudent | null>(null);

  const [searching, setSearching] = useState(false);
  const [searchError, setSearchError] = useState("");

  const [sending, setSending] = useState(false);
  const [inviteError, setInviteError] = useState("");
  const [inviteSuccess, setInviteSuccess] = useState("");

  useEffect(() => {
    const controller = new AbortController();

    async function load() {
      setLoading(true);
      setError("");

      try {
        const [projectsResponse, teamsResponse] =
          await Promise.all([
            getMyStudentProjects(controller.signal),
            teamApiRequest<LiveTeamSummary[]>(
              "/team/my-project-teams",
              { signal: controller.signal }
            ),
          ]);

        if (controller.signal.aborted) return;

        if (
          !Array.isArray(projectsResponse) ||
          !Array.isArray(teamsResponse)
        ) {
          throw new Error("Invalid project team response.");
        }

        setProjectList(projectsResponse);
        setTeams(teamsResponse);
      } catch (err) {
        if (controller.signal.aborted) return;

        setError(
          err instanceof Error
            ? err.message
            : "Unable to load project teams."
        );
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      }
    }

    load();

    return () => controller.abort();
  }, [refreshKey]);

  // Refresh when an invitation is accepted elsewhere.
  useEffect(() => {
    function refreshOnInvitation() {
      setRefreshKey((value) => value + 1);
    }

    window.addEventListener(
      "academiq:invitation-accepted",
      refreshOnInvitation
    );

    return () => {
      window.removeEventListener(
        "academiq:invitation-accepted",
        refreshOnInvitation
      );
    };
  }, []);

  const currentTeam =
    teams.find(
      (team) => team.projectId === inviteProjectId
    ) ?? null;

  // Search only when the selected project permits invitations.
  useEffect(() => {
    if (
      !inviteOpen ||
      !currentTeam?.canInvite ||
      studentQuery.trim().length < 2 ||
      inviteSuccess
    ) {
      setEligibleStudents([]);
      setSearching(false);
      setSearchError("");
      return;
    }

    const controller = new AbortController();

    setSearching(true);
    setSearchError("");

    const timeout = window.setTimeout(async () => {
      try {
        const results = await teamApiRequest<
          LiveEligibleStudent[]
        >(
          `/team/projects/${inviteProjectId}/search?query=${encodeURIComponent(
            studentQuery.trim()
          )}`,
          { signal: controller.signal }
        );

        if (!controller.signal.aborted) {
          if (!Array.isArray(results)) {
            throw new Error(
              "Unexpected eligible students response."
            );
          }

          setEligibleStudents(results);
        }
      } catch (err) {
        if (!controller.signal.aborted) {
          setEligibleStudents([]);
          setSearchError(
            err instanceof Error
              ? err.message
              : "Unable to search students."
          );
        }
      } finally {
        if (!controller.signal.aborted) {
          setSearching(false);
        }
      }
    }, 350);

    return () => {
      window.clearTimeout(timeout);
      controller.abort();
    };
  }, [
    inviteOpen,
    inviteProjectId,
    currentTeam?.canInvite,
    studentQuery,
    inviteSuccess,
  ]);

  function toggleProject(projectId: number) {
    setExpandedProjects((previous) =>
      previous.includes(projectId)
        ? previous.filter((id) => id !== projectId)
        : [...previous, projectId]
    );
  }

  function openInvite(projectId: number) {
    setInviteProjectId(projectId);
    setStudentQuery("");
    setSelectedStudent(null);
    setEligibleStudents([]);
    setSearchError("");
    setInviteError("");
    setInviteSuccess("");
    setInviteOpen(true);
  }

  function closeInvite() {
    if (sending) return;

    setInviteOpen(false);
    setStudentQuery("");
    setSelectedStudent(null);
    setEligibleStudents([]);
    setInviteError("");
    setInviteSuccess("");
    onActionConsumed?.();
  }

  async function sendInvitation(
    event: FormEvent<HTMLFormElement>
  ) {
    event.preventDefault();

    if (sending || inviteSuccess) return;

    setInviteError("");

    if (!currentTeam?.canInvite) {
      setInviteError(
        "Invitations are not available for this project."
      );
      return;
    }

    if (!selectedStudent) {
      setInviteError(
        "Please select an eligible student."
      );
      return;
    }

    setSending(true);

    try {
      await teamApiRequest<number>(
        "/invitation/inviteMember",
        {
          method: "POST",
          body: JSON.stringify({
            invitedUserId: selectedStudent.userId,
            projectId: inviteProjectId,
          }),
        }
      );

      setInviteSuccess(
        `Invitation sent to ${selectedStudent.fullName}.`
      );

      setSelectedStudent(null);
      setStudentQuery("");
      setEligibleStudents([]);

      // Retrieve new pending-invitation counts.
      setRefreshKey((value) => value + 1);
    } catch (err) {
      setInviteError(
        err instanceof Error
          ? err.message
          : "Unable to send invitation."
      );
    } finally {
      setSending(false);
    }
  }

  const totalMembers = teams
    .filter((team) =>
      projectList.some(
        (project) => project.projectId === team.projectId
      )
    )
    .reduce(
      (sum, team) => sum + team.memberCount,
      0
    );

  return (
    <main
      className="flex-1 overflow-y-auto"
      style={{
        background: "#f7f6f3",
        minWidth: 0,
      }}
    >
      <Header
        title="Team"
        subtitle="Ongoing projects and members"
      />

      <div
        className="px-8 py-6 space-y-4"
        style={{
          maxWidth: 1440,
          margin: "0 auto",
        }}
      >
        <div style={panel}>
          <div className="flex justify-between items-center flex-wrap gap-3">
            <div>
              <h2
                style={{
                  fontFamily: "var(--font-display)",
                  fontSize: 19,
                }}
              >
                Teams Across Your Projects
              </h2>

              <p
                style={{
                  ...muted,
                  marginTop: 6,
                }}
              >
                View accepted members, project leaders,
                and invitations for each academic project.
              </p>
            </div>

            <div className="flex items-center gap-2">
              <Tag>
                {loading
                  ? "Loading..."
                  : `${projectList.length} projects`}
              </Tag>

              {!loading && (
                <Tag color="#10b981">
                  {totalMembers} memberships
                </Tag>
              )}

              <button
                type="button"
                style={lightButton}
                disabled={loading}
                onClick={() =>
                  setRefreshKey((value) => value + 1)
                }
              >
                Refresh
              </button>
            </div>
          </div>
        </div>

        {loading && (
          <div style={panel}>
            <p style={muted}>
              Loading your project teams...
            </p>
          </div>
        )}

        {!loading && error && (
          <div
            role="alert"
            style={{
              ...panel,
              color: "#b91c1c",
              fontSize: 13,
            }}
          >
            {error}

            <button
              type="button"
              style={{
                ...lightButton,
                marginTop: 12,
              }}
              onClick={() =>
                setRefreshKey((value) => value + 1)
              }
            >
              Try Again
            </button>
          </div>
        )}

        {!loading &&
          !error &&
          projectList.length === 0 && (
            <div style={panel}>
              <Empty text="No academic projects are associated with your account." />
            </div>
          )}

        {!loading &&
          !error &&
          projectList.map((project) => {
            const team = teams.find(
              (item) =>
                item.projectId === project.projectId
            );

            const isExpanded =
              expandedProjects.includes(
                project.projectId
              );

            const members = team?.members ?? [];

            return (
              <section
                key={project.projectId}
                className="rounded-2xl overflow-hidden"
                style={{
                  background: "white",
                  boxShadow:
                    "0 1px 4px rgba(26,24,48,.06)",
                  border: "1px solid #e8e7f0",
                }}
              >
                {/* PROJECT GROUP HEADER */}
                <div
                  className="p-5 flex flex-wrap justify-between items-center gap-3"
                  style={{
                    background: isExpanded
                      ? "#f8f7ff"
                      : "white",
                  }}
                >
                  <button
                    type="button"
                    className="flex-1 text-left min-w-0"
                    aria-expanded={isExpanded}
                    onClick={() =>
                      toggleProject(project.projectId)
                    }
                  >
                    <h3
                      style={{
                        fontFamily:
                          "var(--font-display)",
                        fontSize: 18,
                        color: "#1a1830",
                        overflowWrap: "anywhere",
                      }}
                    >
                      {project.projectTitle}
                    </h3>

                    <p
                      style={{
                        ...muted,
                        marginTop: 5,
                      }}
                    >
                      {project.programmeSubjectName ||
                        "Academic Project"}
                      {" · "}
                      {project.currentStage
                        ?.replace(/_/g, " ") ||
                        "Stage unavailable"}
                    </p>
                  </button>

                  <div className="flex flex-wrap items-center gap-2">
                    {team ? (
                      <Tag
                        color={
                          team.availableSlots > 0
                            ? "#10b981"
                            : "#f59e0b"
                        }
                      >
                        {team.memberCount}/{team.maxMembers} students
                      </Tag>
                    ) : (
                      <Tag color="#ef4444">
                        Team unavailable
                      </Tag>
                    )}

                    {team &&
                      team.pendingInvitationCount > 0 && (
                        <Tag color="#f59e0b">
                          {team.pendingInvitationCount} pending
                        </Tag>
                      )}

                    {team?.canInvite && (
                      <button
                        type="button"
                        onClick={() =>
                          openInvite(project.projectId)
                        }
                        style={lightButton}
                      >
                        + Invite Member
                      </button>
                    )}

                    <button
                      type="button"
                      aria-label={
                        isExpanded
                          ? "Collapse team"
                          : "Expand team"
                      }
                      onClick={() =>
                        toggleProject(project.projectId)
                      }
                      style={{
                        color: "#6366f1",
                        fontSize: 19,
                        padding: 4,
                      }}
                    >
                      {isExpanded ? "⌃" : "⌄"}
                    </button>
                  </div>
                </div>

                {/* EXPANDED TEAM MEMBERS */}
                {isExpanded && (
                  <div
                    className="p-5 space-y-4"
                    style={{
                      borderTop:
                        "1px solid #e8e7f0",
                    }}
                  >
                    {!team ? (
                      <p
                        style={{
                          color: "#b91c1c",
                          fontSize: 12,
                        }}
                      >
                        Team information is unavailable for
                        this project.
                      </p>
                    ) : (
                      <>
                        <div className="flex flex-wrap justify-between gap-3">
                          <div>
                            <strong
                              style={{
                                fontSize: 13,
                                color: "#1a1830",
                              }}
                            >
                              Accepted Team Members
                            </strong>

                            <p
                              style={{
                                ...muted,
                                marginTop: 4,
                              }}
                            >
                              {team.isLeader
                                ? "You are the project leader."
                                : "You are a project team member."}
                            </p>
                          </div>

                          <Tag>
                            {members.length} members
                          </Tag>
                        </div>

                        {members.length === 0 ? (
                          <Empty text="No accepted team members found." />
                        ) : (
                          <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-3">
                            {members.map((member) => {
                              const isLeader =
                                member.role
                                  .toUpperCase()
                                  .includes("LEADER");

                              return (
                                <div
                                  key={member.memberId}
                                  style={{
                                    border:
                                      "1px solid #e8e7f0",
                                    borderRadius: 14,
                                    padding: 14,
                                    minWidth: 0,
                                  }}
                                >
                                  <div className="flex items-center gap-3">
                                    <div
                                      className="flex items-center justify-center rounded-full text-white font-semibold"
                                      style={{
                                        background:
                                          "#6366f1",
                                        width: 38,
                                        height: 38,
                                        flexShrink: 0,
                                        fontSize: 12,
                                      }}
                                    >
                                      {teamMemberInitials(
                                        member.fullName
                                      )}
                                    </div>

                                    <div className="min-w-0">
                                      <b
                                        style={{
                                          fontSize: 13,
                                          overflowWrap:
                                            "anywhere",
                                        }}
                                      >
                                        {member.fullName}
                                      </b>

                                      <div
                                        style={{
                                          ...muted,
                                          marginTop: 3,
                                        }}
                                      >
                                        {isLeader
                                          ? "Project Leader"
                                          : member.role.replace(
                                              /_/g,
                                              " "
                                            )}
                                      </div>
                                    </div>
                                  </div>

                                  <div className="mt-3">
                                    <Tag
                                      color={
                                        isLeader
                                          ? "#8b5cf6"
                                          : "#6366f1"
                                      }
                                    >
                                      {isLeader
                                        ? "Leader"
                                        : "Team Member"}
                                    </Tag>
                                  </div>
                                </div>
                              );
                            })}
                          </div>
                        )}

                        <div
                          className="rounded-xl p-4 flex flex-wrap items-center justify-between gap-3"
                          style={{
                            background: "#f7f6f3",
                          }}
                        >
                          <div>
                            <strong
                              style={{
                                fontSize: 12,
                              }}
                            >
                              Academic Supervisor
                            </strong>

                            <p
                              style={{
                                ...muted,
                                marginTop: 5,
                              }}
                            >
                              {project.supervisorName ||
                                "Not assigned"}
                            </p>
                          </div>

                          <div
                            className="text-right"
                            style={muted}
                          >
                            {team.availableSlots} available slots
                          </div>
                        </div>

                        {!team.canInvite && (
                          <p style={muted}>
                            Invitations are available only
                            to eligible project leaders
                            when team capacity permits.
                          </p>
                        )}
                      </>
                    )}
                  </div>
                )}
              </section>
            );
          })}
      </div>

      {/* INVITE STUDENT MODAL */}
      {inviteOpen && (
        <Modal
          title="Invite Student"
          onClose={closeInvite}
        >
          <form
            className="space-y-4"
            onSubmit={sendInvitation}
          >
            <label className="block text-sm">
              Project
              <select
                style={inputStyle}
                value={inviteProjectId || ""}
                disabled={sending || Boolean(inviteSuccess)}
                onChange={(event) => {
                  setInviteProjectId(
                    Number(event.target.value)
                  );
                  setStudentQuery("");
                  setSelectedStudent(null);
                  setEligibleStudents([]);
                  setInviteError("");
                }}
              >
                <option value="">
                  Select project
                </option>

                {projectList.map((project) => (
                  <option
                    key={project.projectId}
                    value={project.projectId}
                  >
                    {project.projectTitle}
                  </option>
                ))}
              </select>
            </label>

            {currentTeam && (
              <div
                className="rounded-xl p-4"
                style={{
                  background: "#f7f6f3",
                }}
              >
                <div className="flex justify-between items-center gap-3">
                  <div>
                    <strong
                      style={{
                        fontSize: 13,
                      }}
                    >
                      Team Capacity
                    </strong>

                    <p style={muted}>
                      {currentTeam.memberCount} of{" "}
                      {currentTeam.maxMembers} members
                    </p>
                  </div>

                  <Tag
                    color={
                      currentTeam.availableSlots > 0
                        ? "#10b981"
                        : "#ef4444"
                    }
                  >
                    {currentTeam.availableSlots} slots
                  </Tag>
                </div>

                {currentTeam.pendingInvitationCount > 0 && (
                  <p
                    style={{
                      ...muted,
                      marginTop: 8,
                    }}
                  >
                    {currentTeam.pendingInvitationCount} pending
                    invitation(s)
                  </p>
                )}
              </div>
            )}

            {inviteProjectId > 0 &&
              !currentTeam?.canInvite && (
                <p
                  role="alert"
                  style={{
                    color: "#b91c1c",
                    fontSize: 12,
                  }}
                >
                  You cannot invite students to this
                  project. Only the project leader may
                  invite eligible students when space
                  is available.
                </p>
              )}

            {currentTeam?.canInvite &&
              !inviteSuccess && (
                <>
                  <label className="block text-sm">
                    Search Eligible Students
                    <input
                      type="search"
                      style={inputStyle}
                      value={studentQuery}
                      placeholder="Search by name or email"
                      onChange={(event) => {
                        setStudentQuery(
                          event.target.value
                        );
                        setSelectedStudent(null);
                      }}
                    />
                  </label>

                  {searching && (
                    <p style={muted}>
                      Searching students...
                    </p>
                  )}

                  {searchError && (
                    <p
                      role="alert"
                      style={{
                        color: "#b91c1c",
                        fontSize: 12,
                      }}
                    >
                      {searchError}
                    </p>
                  )}

                  {!searching &&
                    !searchError &&
                    studentQuery.trim().length >= 2 &&
                    eligibleStudents.length === 0 && (
                      <p style={muted}>
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
                      {eligibleStudents.map((student) => (
                        <button
                          key={student.userId}
                          type="button"
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

                            <p style={muted}>
                              {student.email}
                            </p>
                          </div>

                          {selectedStudent?.userId ===
                            student.userId && (
                            <Tag color="#10b981">
                              Selected
                            </Tag>
                          )}
                        </button>
                      ))}
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
                          fontSize: 12,
                        }}
                      >
                        Invite {selectedStudent.fullName}
                      </strong>

                      <p
                        style={{
                          color: "#047857",
                          fontSize: 11,
                          marginTop: 5,
                        }}
                      >
                        {selectedStudent.email}
                      </p>
                    </div>
                  )}
                </>
              )}

            {inviteError && (
              <div
                role="alert"
                className="rounded-xl p-3"
                style={{
                  color: "#b91c1c",
                  background: "#fff1f2",
                  fontSize: 12,
                }}
              >
                {inviteError}
              </div>
            )}

            {inviteSuccess && (
              <div
                role="status"
                className="rounded-xl p-3"
                style={{
                  color: "#047857",
                  background: "#ecfdf5",
                  fontSize: 12,
                }}
              >
                {inviteSuccess}
              </div>
            )}

            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={closeInvite}
                disabled={sending}
                style={lightButton}
              >
                {inviteSuccess ? "Close" : "Cancel"}
              </button>

              {!inviteSuccess && (
                <button
                  type="submit"
                  disabled={
                    sending ||
                    !currentTeam?.canInvite ||
                    !selectedStudent
                  }
                  style={{
                    ...brandButton,
                    opacity:
                      sending ||
                      !currentTeam?.canInvite ||
                      !selectedStudent
                        ? 0.5
                        : 1,
                  }}
                >
                  {sending
                    ? "Sending..."
                    : "Send Invitation"}
                </button>
              )}
            </div>
          </form>
        </Modal>
      )}
    </main>
  );
}



type SubmissionMilestone = {
  milestoneId: number;
  title: string;
  description: string | null;
  deadline: string | null;
  status: string;
  progress: number | null;
};

async function submissionApiError(
  response: Response
): Promise<Error> {
  const body = await response.text();

  let message = `Request failed (HTTP ${response.status}).`;

  try {
    const parsed = JSON.parse(body);
    message = parsed.message || parsed.error || message;
  } catch {
    if (body && !body.trim().startsWith("<")) {
      message = body;
    }
  }

  if (response.status === 401) {
    message = "Your session has expired. Please sign in again.";
  }

  return new Error(message);
}

export function SubmissionsWorkspace({
  initialProject = "",
  initialAction = "",
  onActionConsumed,
}: {
  initialProject?: string;
  initialAction?: string;
  onActionConsumed?: () => void;
}) {
  const [projectList, setProjectList] =
    useState<StudentProject[]>([]);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [refreshKey, setRefreshKey] = useState(0);

  const [expandedProjects, setExpandedProjects] =
    useState<number[]>([]);

  const [showForm, setShowForm] = useState(
    initialAction === "submit"
  );

  const [uploadProject, setUploadProject] = useState(
    Number(initialProject) > 0 ? initialProject : ""
  );

  const [milestones, setMilestones] =
    useState<SubmissionMilestone[]>([]);

  const [milestonesLoading, setMilestonesLoading] =
    useState(false);

  const [milestoneError, setMilestoneError] =
    useState("");

  const [milestoneId, setMilestoneId] = useState("");
  const [file, setFile] = useState<File | null>(null);

  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState("");
  const [formSuccess, setFormSuccess] = useState("");

  // Load actual projects belonging to the current student.
  useEffect(() => {
    const controller = new AbortController();

    setLoading(true);
    setError("");

    getMyStudentProjects(controller.signal)
      .then((data) => {
        if (controller.signal.aborted) return;

        setProjectList(data);

        // Keep all project groups collapsed on initial load.
        // Preserve the user's selections during refresh.
        setExpandedProjects((previous) => previous);

        setUploadProject((previous) => {
          if (
            data.some(
              (project) =>
                String(project.projectId) === previous
            )
          ) {
            return previous;
          }

          return data.length === 1
            ? String(data[0].projectId)
            : "";
        });
      })
      .catch((err) => {
        if (controller.signal.aborted) return;

        setError(
          err instanceof Error
            ? err.message
            : "Unable to load your projects."
        );
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      });

    return () => controller.abort();
  }, [refreshKey, initialProject]);

  // Load real milestones for the project selected for upload.
  useEffect(() => {
    if (!showForm || !uploadProject) {
      setMilestones([]);
      setMilestoneId("");
      setMilestoneError("");
      return;
    }

    const controller = new AbortController();

    async function loadMilestones() {
      setMilestonesLoading(true);
      setMilestoneError("");
      setMilestones([]);
      setMilestoneId("");

      try {
        const response = await fetch(
          `/api/projects/${Number(uploadProject)}/milestones`,
          {
            method: "GET",
            signal: controller.signal,
            headers: {
              Authorization: `Bearer ${liveTaskToken()}`,
              Accept: "application/json",
            },
          }
        );

        if (!response.ok) {
          throw await submissionApiError(response);
        }

        const data: unknown = await response.json();

        if (!Array.isArray(data)) {
          throw new Error(
            "Unexpected project milestones response."
          );
        }

        if (!controller.signal.aborted) {
          setMilestones(data as SubmissionMilestone[]);
        }
      } catch (err) {
        if (!controller.signal.aborted) {
          setMilestoneError(
            err instanceof Error
              ? err.message
              : "Unable to load milestones."
          );
        }
      } finally {
        if (!controller.signal.aborted) {
          setMilestonesLoading(false);
        }
      }
    }

    loadMilestones();

    return () => controller.abort();
  }, [showForm, uploadProject]);

  function toggleProject(projectId: number) {
    setExpandedProjects((previous) =>
      previous.includes(projectId)
        ? previous.filter((id) => id !== projectId)
        : [...previous, projectId]
    );
  }

  function openSubmissionForm(projectId?: number) {
    setUploadProject(
      projectId
        ? String(projectId)
        : projectList.length === 1
          ? String(projectList[0].projectId)
          : ""
    );

    setFile(null);
    setMilestoneId("");
    setFormError("");
    setFormSuccess("");
    setShowForm(true);
  }

  function closeSubmissionForm() {
    if (saving) return;

    setShowForm(false);
    setFormError("");
    setFormSuccess("");
    setFile(null);
    setMilestoneId("");
    onActionConsumed?.();
  }

  async function submitPdf(
    event: FormEvent<HTMLFormElement>
  ) {
    event.preventDefault();

    if (saving || formSuccess) return;

    setFormError("");
    setFormSuccess("");

    const projectId = Number(uploadProject);
    const selectedMilestoneId = Number(milestoneId);

    if (
      !Number.isInteger(projectId) ||
      projectId <= 0 ||
      !projectList.some(
        (project) => project.projectId === projectId
      )
    ) {
      setFormError("Please select a valid project.");
      return;
    }

    if (
      !Number.isInteger(selectedMilestoneId) ||
      !milestones.some(
        (milestone) =>
          milestone.milestoneId === selectedMilestoneId
      )
    ) {
      setFormError("Please select a valid milestone.");
      return;
    }

    if (!file) {
      setFormError("Please select a PDF file.");
      return;
    }

    if (
      file.type !== "application/pdf" &&
      !file.name.toLowerCase().endsWith(".pdf")
    ) {
      setFormError("Only PDF files are accepted.");
      return;
    }

    setSaving(true);

    try {
      const body = new FormData();
      body.append("file", file);

      const response = await fetch(
        `/submissions/students/projects/${projectId}/milestones/${selectedMilestoneId}`,
        {
          method: "POST",
          headers: {
            Authorization: `Bearer ${liveTaskToken()}`,
            Accept: "application/json",
          },
          body,
        }
      );

      if (!response.ok) {
        throw await submissionApiError(response);
      }

      setFormSuccess(
        "Milestone PDF submitted successfully."
      );

      setFile(null);

      // Refresh the submitted project's history.
      setRefreshKey((value) => value + 1);

      // Ensure the submitted project is visible.
      setExpandedProjects((previous) =>
        previous.includes(projectId)
          ? previous
          : [...previous, projectId]
      );
    } catch (err) {
      setFormError(
        err instanceof Error
          ? err.message
          : "Unable to submit the PDF."
      );
    } finally {
      setSaving(false);
    }
  }

  return (
    <main
      className="flex-1 overflow-y-auto"
      style={{
        background: "#f7f6f3",
        minWidth: 0,
      }}
    >
      <Header
        title="Submissions"
        subtitle="Academic deliverables and supervisor review"
        action="New Submission"
        onAction={() => openSubmissionForm()}
      />

      <div className="px-8 py-6 space-y-5">
        {/* PAGE INTRODUCTION */}
        <div style={panel}>
          <div className="flex items-center justify-between flex-wrap gap-3">
            <div>
              <h2
                style={{
                  fontFamily: "var(--font-display)",
                  fontSize: 19,
                  color: "#1a1830",
                }}
              >
                Project Submissions
              </h2>

              <p
                style={{
                  ...muted,
                  marginTop: 7,
                  lineHeight: 1.6,
                }}
              >
                Browse milestone PDF submissions,
                document versions, review decisions,
                and supervisor feedback by project.
              </p>
            </div>

            <div className="flex items-center gap-2">
              <Tag>
                {loading
                  ? "Loading..."
                  : `${projectList.length} projects`}
              </Tag>

              <button
                type="button"
                disabled={loading}
                onClick={() =>
                  setRefreshKey((value) => value + 1)
                }
                style={{
                  ...lightButton,
                  opacity: loading ? 0.6 : 1,
                }}
              >
                Refresh
              </button>
            </div>
          </div>
        </div>

        {loading && (
          <div style={panel}>
            <p style={muted}>
              Loading your academic projects...
            </p>
          </div>
        )}

        {error && (
          <div
            role="alert"
            style={{
              ...panel,
              color: "#b91c1c",
              fontSize: 13,
            }}
          >
            {error}

            <button
              type="button"
              onClick={() =>
                setRefreshKey((value) => value + 1)
              }
              style={{
                ...lightButton,
                marginTop: 12,
                display: "block",
              }}
            >
              Try Again
            </button>
          </div>
        )}

        {!loading &&
          !error &&
          projectList.length === 0 && (
            <div style={panel}>
              <Empty text="No academic projects are associated with your account." />
            </div>
          )}

        {/* PROJECT GROUPS */}
        {!loading &&
          !error &&
          projectList.map((project) => {
            const isOpen = expandedProjects.includes(
              project.projectId
            );

            return (
              <section
                key={project.projectId}
                className="rounded-2xl overflow-hidden"
                style={{
                  background: "white",
                  boxShadow:
                    "0 1px 4px rgba(26,24,48,.06)",
                  border: "1px solid #e8e7f0",
                }}
              >
                <div
                  className="p-5 flex items-center justify-between gap-3"
                  style={{
                    background: isOpen
                      ? "#f8f7ff"
                      : "white",
                  }}
                >
                  <button
                    type="button"
                    aria-expanded={isOpen}
                    onClick={() =>
                      toggleProject(project.projectId)
                    }
                    className="flex-1 min-w-0 text-left"
                  >
                    <h3
                      style={{
                        fontFamily:
                          "var(--font-display)",
                        fontSize: 17,
                        color: "#1a1830",
                        overflowWrap: "anywhere",
                      }}
                    >
                      {project.projectTitle}
                    </h3>

                    <p
                      style={{
                        ...muted,
                        marginTop: 5,
                      }}
                    >
                      {isOpen
                        ? "Hide submission history"
                        : "View submission history"}
                    </p>
                  </button>

                  <div className="flex items-center gap-3">
                    <button
                      type="button"
                      onClick={() =>
                        openSubmissionForm(project.projectId)
                      }
                      style={lightButton}
                    >
                      + Submit PDF
                    </button>

                    <button
                      type="button"
                      onClick={() =>
                        toggleProject(project.projectId)
                      }
                      aria-label={
                        isOpen
                          ? "Collapse project"
                          : "Expand project"
                      }
                      style={{
                        color: "#6366f1",
                        fontSize: 19,
                        padding: 4,
                      }}
                    >
                      {isOpen ? "⌃" : "⌄"}
                    </button>
                  </div>
                </div>

                {isOpen && (
                  <div
                    className="p-5"
                    style={{
                      borderTop: "1px solid #e8e7f0",
                    }}
                  >
                    <ProjectSubmissionHistory
                      projectId={project.projectId}
                      refreshKey={refreshKey}
                      onNewSubmission={() =>
                        openSubmissionForm(
                          project.projectId
                        )
                      }
                    />
                  </div>
                )}
              </section>
            );
          })}
      </div>

      {/* REAL PDF SUBMISSION FORM */}
      {showForm && (
        <Modal
          title="New Milestone Submission"
          onClose={closeSubmissionForm}
        >
          <form
            className="space-y-4"
            onSubmit={submitPdf}
          >
            <p style={muted}>
              Select a project and milestone, then
              upload a PDF for supervisor review.
            </p>

            <label className="block text-sm">
              Project
              <select
                required
                style={inputStyle}
                value={uploadProject}
                disabled={saving || Boolean(formSuccess)}
                onChange={(event) => {
                  setUploadProject(event.target.value);
                  setMilestoneId("");
                  setFile(null);
                  setFormError("");
                }}
              >
                <option value="">
                  Select project
                </option>

                {projectList.map((project) => (
                  <option
                    key={project.projectId}
                    value={String(project.projectId)}
                  >
                    {project.projectTitle}
                  </option>
                ))}
              </select>
            </label>

            <label className="block text-sm">
              Milestone
              <select
                required
                style={inputStyle}
                value={milestoneId}
                disabled={
                  !uploadProject ||
                  milestonesLoading ||
                  saving ||
                  Boolean(formSuccess)
                }
                onChange={(event) =>
                  setMilestoneId(event.target.value)
                }
              >
                <option value="">
                  Select milestone
                </option>

                {milestones.map((milestone) => (
                  <option
                    key={milestone.milestoneId}
                    value={milestone.milestoneId}
                  >
                    {milestone.title}
                  </option>
                ))}
              </select>
            </label>

            {milestonesLoading && (
              <p style={muted}>
                Loading project milestones...
              </p>
            )}

            {milestoneError && (
              <p
                role="alert"
                style={{
                  color: "#b91c1c",
                  fontSize: 12,
                }}
              >
                {milestoneError}
              </p>
            )}

            {!milestonesLoading &&
              !milestoneError &&
              uploadProject &&
              milestones.length === 0 && (
                <p style={muted}>
                  No milestones are available for
                  this project.
                </p>
              )}

            <label className="block text-sm">
              PDF Document
              <input
                key={`${uploadProject}-${formSuccess}`}
                type="file"
                accept=".pdf,application/pdf"
                required={!formSuccess}
                disabled={saving || Boolean(formSuccess)}
                style={inputStyle}
                onChange={(event) =>
                  setFile(
                    event.target.files?.[0] ?? null
                  )
                }
              />
            </label>

            <p style={muted}>
              The PDF will be submitted for
              supervisor review. Submission does
              not automatically mean approval.
            </p>

            {formError && (
              <div
                role="alert"
                className="rounded-xl p-3"
                style={{
                  background: "#fff1f2",
                  color: "#b91c1c",
                  fontSize: 12,
                }}
              >
                {formError}
              </div>
            )}

            {formSuccess && (
              <div
                role="status"
                className="rounded-xl p-3"
                style={{
                  background: "#ecfdf5",
                  color: "#047857",
                  fontSize: 12,
                }}
              >
                {formSuccess}
              </div>
            )}

            <div className="flex justify-end gap-3 pt-3">
              <button
                type="button"
                disabled={saving}
                onClick={closeSubmissionForm}
                style={lightButton}
              >
                {formSuccess ? "Close" : "Cancel"}
              </button>

              {!formSuccess && (
                <button
                  type="submit"
                  disabled={
                    saving ||
                    milestonesLoading ||
                    Boolean(milestoneError) ||
                    !uploadProject ||
                    !milestoneId ||
                    !file
                  }
                  style={{
                    ...brandButton,
                    opacity:
                      saving ||
                      milestonesLoading ||
                      !uploadProject ||
                      !milestoneId ||
                      !file
                        ? 0.6
                        : 1,
                  }}
                >
                  {saving
                    ? "Submitting..."
                    : "Submit PDF"}
                </button>
              )}
            </div>
          </form>
        </Modal>
      )}
    </main>
  );
}
