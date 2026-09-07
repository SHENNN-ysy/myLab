# React 18 + TypeScript + Vite

Blog frontend SPA built with React 18, TypeScript, and Vite. State is managed with zustand, routing with react-router-dom, animations with GSAP, and styling with Tailwind CSS 4 plus CSS Modules.

- `npm run dev` — dev server (port 5173, `/api` proxied to :8000)
- `npm run build` — `tsc -b` type check + production build
- `npm run lint` — ESLint

Directory layout: `src/pages/` route views, `src/components/` section and UI components, `src/stores/` zustand stores, `src/hooks/`, `src/api/`, `src/data/`, `src/types/`, `src/utils/`.
