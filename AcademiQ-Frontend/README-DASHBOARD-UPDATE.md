# AcademiQ Landing and Dashboard Revision

## Landing
- On navigation to `/` or `/login`, scroll is reset to the top so the navbar is not clipped by a retained portal scroll position.
- Final scoped CSS sets explicit navbar height and aligns hero content with the product imagery.
- The preview collage remains responsive and is kept separate from Student Portal styles.

## Student dashboard
- The single-project featured hero is replaced by a multi-project portfolio preview.
- Its project cards open My Projects rather than assuming any one project is the current project.
- Upcoming milestones show associated project names.
- Stats, chart, notifications, feedback and Quick Actions retain their UI.
- These portfolio values and milestone data are demo data pending API integration.

## Running
`npm install` then `npm run dev`. Browser hard refresh with Ctrl+Shift+R.
