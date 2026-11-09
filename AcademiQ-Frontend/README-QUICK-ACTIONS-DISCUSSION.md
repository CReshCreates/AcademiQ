# AcademiQ MVP UI update — Quick Actions and Discussion

- Original `src/index.css`, sidebar colors, and My Projects listing / opening flow have not been changed.
- Quick Actions and the top Create Task button within Project Details now open an in-place modal. Closing/canceling the modal does not navigate. Submitting a valid task or submission demo form navigates to the respective workspace.
- The created demonstration task or submission is stored in `sessionStorage` and appears in the destination workspace. This is prototype-only, not database persistence. The selected document's *filename only* is stored; file bytes are not uploaded.
- Invite Member presents an in-place form. The demonstration project currently has three members; submission is disabled to respect the group limit. No fake invitation is sent.
- Open Project Chat opens a preview dialog. Use the explicit 'Open Discussion' button to navigate to the new independent Discussion sidebar page.
- Discussion has project-specific conversations, project search, message bubbles and a composer. Enter sends, Shift+Enter inserts a newline. New messages exist only in React state until refresh; no server messages are sent.
- The old Discussion tab has been removed from Project Details to prevent duplicate destinations.
- Backend integration and real authorization are outside the scope of this UI-only iteration.

## Windows PowerShell

```powershell
npm.cmd install
npm.cmd run dev
npm.cmd run build
```

TypeScript check passed in the authoring environment; Vite production build could not run there because the existing dependency installation lacks a platform-specific Rolldown native binding. Re-install dependencies on Windows for a platform-correct build.
