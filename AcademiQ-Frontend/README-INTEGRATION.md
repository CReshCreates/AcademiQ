# AcademiQ — Landing, Registration and Student Portal UI

Merged from `AcademiQ Frontend.zip` (landing/sign-in/registration) and `AcademiQ-MVP-layout-fixes.zip` (student + supervisor portal UI).

## Run

Requires Node.js 22+.

```bash
npm install
npm run dev
```

Open the Vite URL (typically http://localhost:5173).

## Pages

- `/` or `/login`: uploaded landing page with Sign In modal
- `/signup`: uploaded registration page
- `/student`: existing Student Portal UI
- `/supervisor`: existing Supervisor Portal UI

## Integration status

**UI demo only:** The original login form did not call a backend and the registration form prevented submission. To make the screens navigable, submitting Sign In opens the Student Portal demo, and submitting registration returns to login. This does **not** create users or authenticate them with Spring Boot. The dashboard, chat, and most other portal data remain mock UI pending backend integration.

Both uploaded designs and image assets are preserved. Additional styles in `src/responsive.css` adjust authentication pages and portal layouts for smaller screens. For a production app, replace demo submit handlers with JWT auth API calls and protect portal routes.
