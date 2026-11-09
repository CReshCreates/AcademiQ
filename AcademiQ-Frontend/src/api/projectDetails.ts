
export interface ProjectKpiCards {
  totalTasks: number | null;
  completedTasks: number | null;
  totalMilestones: number | null;
  completedMilestones: number | null;
}

export interface ProjectTeamMember {
  fullName: string;
  role: string;
  taskedAssignedNumber: number | null;
}

export interface ProjectMilestoneDeadline {
  milestoneName: string | null;
  milestoneDescription: string | null;
  milestoneDeadline: string | null;
  progressPercentage: number | null;
}

export interface ProjectFeedback {
  supervisorsFullname: string | null;
  milestoneName: string | null;
  feedbackDescription: string | null;
  activityCreatedTime: string | null;
}

export interface ProjectRecentActivity {
  activityDescription: string;
  activityCreatedTime: string | null;
}

export interface ProjectDetailsResponse {
  projectId: number;
  projectTitle: string;
  projectDescription: string | null;
  projectStatus: string;
  createdAt: string | null;
  overallProgress: number | null;

  supervisorId: number | null;
  supervisorName: string | null;

  projectKPICards: ProjectKpiCards | null;
  teamMembers: ProjectTeamMember[];
  milestoneDeadlineCard: ProjectMilestoneDeadline | null;
  feedbackCard: ProjectFeedback | null;
  recentActivitiesCard: ProjectRecentActivity[];
}

export async function getProjectDetails(
  projectId: number,
  signal?: AbortSignal
): Promise<ProjectDetailsResponse> {
  const stored = sessionStorage.getItem("academiq_auth");

  if (!stored) {
    throw new Error("Please sign in again.");
  }

  let token: unknown;

  try {
    token = JSON.parse(stored).token;
  } catch {
    throw new Error("Invalid login session.");
  }

  if (typeof token !== "string" || !token.trim()) {
    throw new Error("Authentication token is missing.");
  }

  if (!Number.isInteger(projectId) || projectId <= 0) {
    throw new Error("Invalid project ID.");
  }

  const response = await fetch(
    `/projects/students/getProjectDetails/${projectId}`,
    {
      method: "GET",
      signal,
      headers: {
        Authorization: `Bearer ${token}`,
        Accept: "application/json",
      },
    }
  );

  if (!response.ok) {
    if (response.status === 401 || response.status === 403) {
      throw new Error(
        "You do not have permission to view this project."
      );
    }

    if (response.status === 404) {
      throw new Error("Project not found.");
    }

    throw new Error(
      `Unable to load project details (HTTP ${response.status}).`
    );
  }

  const data: ProjectDetailsResponse = await response.json();

  if (!data || data.projectId !== projectId) {
    throw new Error("Unexpected project details response.");
  }

  return {
    ...data,
    teamMembers: data.teamMembers ?? [],
    recentActivitiesCard: data.recentActivitiesCard ?? [],
  };
}
