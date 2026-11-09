# AcademiQ frontend — project-context UX update

This update is based on the **original SPM.zip**, not the separately redesigned frontend.

## Preserved
- Original `src/index.css` and its indigo/violet palette are unchanged.
- `StudentProjects` (My Projects), project creation modal, and `ProjectDetails.tsx` navigation are unchanged.
- Sidebar, dashboard, milestones, supervisor screens, and other original modules remain in place.

## Updated only in student sidebar
- **Tasks:** defaults to **Assigned to me / All projects**; select one project and optionally view **All team tasks**. Search, open a task, check subtasks, and create demo tasks.
- **Team:** select a project; show that project's members, responsibilities and available invitations (3-student maximum including pending reservations). Demo invitation form.
- **Submissions:** select a project; view milestone-linked history; optional consolidated history across projects; demo submission form with file picker, explicit no-upload notice.

## File changes
- New `src/StudentWorkspaces.tsx` contains these three student modules.
- `src/App.tsx` imports and renders the new modules for the existing `tasks`, `team`, and `submissions` sidebar entries. The `StudentProjects` component and `ProjectDetails` flow were **not modified**.
- `src/Supervisor.tsx` contains a narrow typing fix for the existing stats display (same UI).
- `src/index.css` and `src/ProjectDetails.tsx` are unchanged.

## Run (Windows PowerShell)
```powershell
npm.cmd install
npm.cmd run dev
```
Then open the URL shown by Vite (usually http://localhost:5173).

## Status / API handoff
- This is an **interactive UI prototype**, not a backend-integrated app. Data is illustrative and resets on refresh. Selecting a file does not upload it.
- Project IDs are demo strings. Real requests should use numeric IDs from the Spring Boot API.
- Suggested backend resources:
  - authenticated task overview across projects (future endpoint) and `GET /api/projects/{projectId}/tasks`
  - task `GET`, `POST`, `PATCH`, `DELETE` under `/api/projects/{projectId}/tasks`
  - subtasks under `/api/projects/{projectId}/tasks/{taskId}/subtasks`
  - project member listing and invitation creation under `/api/projects/{projectId}/members` and `/invitations`
  - milestone-scoped submissions and review history under `/api/projects/{projectId}/submissions`
- Authorization must be enforced in Spring Boot. UI filters are not security controls.
- The prototype's create task dialog omits `milestoneId`; production UI must obtain a valid milestone ID before POSTing.
- The invitation preview uses an email address; the real API needs to resolve it to the validated target user ID.
- The file picker is a prototype; production should use multipart upload with file validation and authenticated download.

## Validation
- TypeScript: `node node_modules/typescript/bin/tsc --noEmit` passed using bundled dependencies from original ZIP.
- Vite production build was attempted but could not run in this Linux environment because the original node_modules archive lacked the platform's native Rolldown binding. Run `npm.cmd install` and `npm.cmd run build` on Windows.
