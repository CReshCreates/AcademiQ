
export interface StudentProject {
  projectId: number;
  projectTitle: string;
  supervisorName: string | null;
  semesterName: string | null;
  programmeSubjectName: string | null;
  programmeSubjectId: number;
  progress: number | null;
  currentStage: string | null;
  createdDate: string | null;
}

export async function getMyStudentProjects(
  signal?: AbortSignal
): Promise<StudentProject[]> {
  const stored = sessionStorage.getItem("academiq_auth");

  if (!stored) {
    throw new Error("Please log in again.");
  }

  let token: unknown;

  try {
    token = JSON.parse(stored).token;
  } catch {
    throw new Error("Invalid login session. Please log in again.");
  }

  if (typeof token !== "string" || !token.trim()) {
    throw new Error("Your login session is missing a token.");
  }

  const response = await fetch(
    "/projects/students/getMyProjects",
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
        "Project access was denied. Please check your login and workspace."
      );
    }

    throw new Error(
      `Unable to load projects (HTTP ${response.status}).`
    );
  }

  const data: unknown = await response.json();

  if (!Array.isArray(data)) {
    throw new Error("Invalid project data received from server.");
  }

  return data as StudentProject[];
}
