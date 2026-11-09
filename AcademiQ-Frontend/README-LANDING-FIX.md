# AcademiQ landing page — isolated layout fix

This build fixes the previous landing header clipping and layout CSS collisions.

## Replacing an older extracted version

1. Stop the current Vite server (Ctrl+C in its terminal).
2. Extract this ZIP to a **new** folder, or remove the old `AcademiQ-Frontend` directory first.
3. Open the *new* `AcademiQ-Frontend` directory in VS Code.
4. Run `npm install`, then `npm run dev`.
5. Open the exact URL printed by Vite and perform a hard refresh (Ctrl+Shift+R).

## Technical details

- `src/auth/Login.jsx`: new landing-specific CSS class names and full-size header.
- `src/auth/LandingPage.css`: a single responsive layout for desktop/tablet/mobile.
- `src/auth/Login.css`: retains only the sign-in modal styles.
- `src/responsive.css` / `src/landing-timeline.css`: removed obsolete landing-specific rules; portal and milestone styles remain.
- React UI remains demo-only for login and registration until backend integration.

## Verification

TypeScript check and JSX parsing were successful. A fully running Vite browser build could not be validated in this sandbox because a required native dependency was unavailable; run locally for final visual verification.
