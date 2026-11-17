# R-CHECK frontend

Use Node.js 20 or newer. Run `npm ci` in this directory to install the locked dependencies.

- `npm start` serves the editor at http://localhost:3000. The Java GUI launcher runs this command after starting the backend.
- `npm test` runs the frontend tests. Install the test browser once with `npx playwright install chromium`, or set `PLAYWRIGHT_CHANNEL=chrome` to use an installed Google Chrome.
- `npm run build` writes the production frontend to `build/`.
