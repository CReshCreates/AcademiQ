# AcademiQ responsive spacing fix

## Main cause

The landing-page `src/auth/Login.css` used a global unlayered `* { margin:0; padding:0; }` reset. Tailwind CSS v4 utilities are inside CSS layers, so the unlayered reset overwrote utility spacing on **all** portal pages. This caused cards, columns and panels to touch, as shown in the screenshot.

## Changes

- Removed the global landing-page margin/padding reset; kept a local box-sizing rule.
- Preserved Tailwind utility padding and gaps throughout Student and Supervisor portals.
- Added consistent sizing, overflow safeguards and adaptive grid rules.
- Improved landing-page max width, interior spacing, column proportions, illustration sizing, and mobile stacking.
- Preserved existing components, routes, and demo-only authentication.

## Run

```bash
npm install
npm run dev
```

Clear the browser's cached CSS with Ctrl+Shift+R after updating.

Frontend integration with Spring Boot remains a separate upcoming step.
