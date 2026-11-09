export interface AvailableProjectSubject {
  subjectId: number;
  subjectName: string;
  programmeSubjectId: number;
}

export interface AvailableProjectSupervisor {
  subjectSupervisorId: number;
  supervisorName: string;
  supervisorId: number;
}

export interface CreatedProject {
  projectId: number;
  projectTitle: string;
}

function token(): string {
  const stored = sessionStorage.getItem('academiq_auth');
  if (!stored) throw new Error('Please log in again.');
  let parsed: unknown;
  try { parsed = JSON.parse(stored); } catch { throw new Error('Invalid login session.'); }
  const value = (parsed as { token?: unknown } | null)?.token;
  if (typeof value !== 'string' || !value.trim()) throw new Error('Please log in again.');
  return value;
}

async function parseError(response: Response): Promise<Error> {
  let detail = '';
  try {
    const body: unknown = await response.json();
    if (body && typeof body === 'object') {
      const obj = body as Record<string, unknown>;
      detail = [obj.message, obj.detail, obj.error].find(
        (item): item is string => typeof item === 'string' && item.trim().length > 0
      ) || '';
    }
  } catch { /* Server may send an empty/non-JSON error response. */ }
  if (response.status === 401) return new Error('Your session expired. Please log in again.');
  if (response.status === 403) return new Error(detail || 'You do not have permission to create this project.');
  return new Error(detail || `Project request failed (HTTP ${response.status}).`);
}

async function getJson<T>(url: string, signal?: AbortSignal): Promise<T> {
  const response = await fetch(url, {
    headers: { Authorization: `Bearer ${token()}`, Accept: 'application/json' },
    signal,
  });
  if (!response.ok) throw await parseError(response);
  return response.json() as Promise<T>;
}

export function getEligibleProjectSubjects(signal?: AbortSignal) {
  return getJson<AvailableProjectSubject[]>('/projects/students/getSubjects', signal);
}

export function getProjectSupervisors(programmeSubjectId: number, signal?: AbortSignal) {
  return getJson<AvailableProjectSupervisor[]>(
    `/projects/students/getSupervisor/${encodeURIComponent(programmeSubjectId)}`,
    signal
  );
}

export async function createAcademicProject(input: {
  projectTitle: string;
  projectDescription: string;
  programmeSubjectId: number;
  supervisorId: number;
  uploadFile: File;
}): Promise<CreatedProject> {
  const form = new FormData();
  form.append('projectTitle', input.projectTitle.trim());
  form.append('projectDescription', input.projectDescription.trim());
  form.append('programmeSubjectId', String(input.programmeSubjectId));
  form.append('supervisorId', String(input.supervisorId));
  form.append('uploadFile', input.uploadFile);

  const response = await fetch('/projects/students/createProject', {
    method: 'POST',
    headers: { Authorization: `Bearer ${token()}` },
    body: form,
  });
  // Do not set Content-Type: the browser must provide the multipart boundary.
  if (!response.ok) throw await parseError(response);
  return response.json() as Promise<CreatedProject>;
}
