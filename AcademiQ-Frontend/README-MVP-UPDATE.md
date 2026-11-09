# AcademiQ MVP frontend update

This builds on `AcademiQ-original-style-UX-update.zip`, preserves `src/index.css` and the indigo/lavender design palette, and leaves the My Projects card/create-project navigation structure intact.

## What changed

- Project Details > Quick Actions now routes Create Task to the preselected task creation modal; Submit Milestone and Upload Document to the preselected submission form; Invite Member to the preselected invitation modal; Open Project Chat to Discussion tab. The header Create Task button works as well.
- Milestones sidebar page now has a project selector and a three-stage/six-milestone academic timeline with review states and links to the correct project's submission form.
- Team sidebar page shows all three demo ongoing projects and member lists together. Each project has capacity indicators and its own invite action.
- Calendar and Resources are removed from both student and supervisor sidebars for MVP. Dashboard shortcuts to those pages are replaced with Milestones and My Projects.
- Existing CSS and My Projects card/create-project flow are maintained.

## Demo restrictions

All data and actions are local React demo state. Refresh resets changes; no JWT/API/database is connected; file selection displays a filename only. Milestone approval is supervisor-controlled in the eventual backend. Project Details content is still based on the original design's network-monitoring sample, even if another My Projects card is opened; making the detail content fully project-dynamic is a separate integration step. Other preexisting placeholder controls may still be nonfunctional.

## Run on Windows PowerShell

```powershell
npm.cmd install
npm.cmd run dev
npm.cmd run build
```

## Verification

TypeScript `tsc --noEmit` passed using dependencies extracted from original SPM ZIP. A production Vite build could not be confirmed in this container due to the original dependency archive lacking the required platform-specific Rolldown binding. Build locally following fresh npm install.
