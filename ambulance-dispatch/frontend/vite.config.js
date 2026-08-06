/*
 * Imports Vite's defineConfig helper.
 *
 * defineConfig provides:
 *
 * 1. Better editor suggestions.
 * 2. Validation and type information for Vite settings.
 * 3. A clear structure for the Vite configuration object.
 */
import { defineConfig } from "vite";

/*
 * Imports the official React plugin for Vite.
 *
 * This plugin enables:
 *
 * 1. React JSX transformation.
 * 2. Fast Refresh while developing.
 * 3. Automatic browser updates when React files change.
 */
import react from "@vitejs/plugin-react";

/*
 * Exports the configuration used by the Vite development server
 * and production build process.
 */
export default defineConfig({

  /*
   * Registers the React plugin with Vite.
   *
   * Without this plugin, Vite would not provide the complete React
   * development experience for JSX and Fast Refresh.
   */
  plugins: [react()],

  /*
   * Configures the local Vite development server.
   *
   * The React frontend normally runs on:
   *
   *     http://localhost:5173
   *
   * The Spring Boot backend normally runs on:
   *
   *     http://localhost:8080
   */
  server: {

    /*
     * Fixes the frontend development port at 5173.
     *
     * strictPort prevents Vite from silently moving to another port
     * when 5173 is already occupied. Instead, it reports an error so
     * we immediately know that another process is using the port.
     */
    port: 5173,
    strictPort: true,

    /*
     * Proxies requests beginning with /api to Spring Boot.
     *
     * Example React request:
     *
     *     fetch("/api/state")
     *
     * Browser sends request to:
     *
     *     http://localhost:5173/api/state
     *
     * Vite forwards it to:
     *
     *     http://localhost:8080/api/state
     *
     * This allows the React code to use relative API paths instead of
     * hard-coding the Spring Boot server address throughout the
     * application.
     */
    proxy: {

      /*
       * Every URL beginning with /api uses this proxy rule.
       */
      "/api": {

        /*
         * The destination Spring Boot backend server.
         */
        target: "http://localhost:8080",

        /*
         * Rewrites the request's host header so it matches the target
         * backend server.
         *
         * This is useful when the backend checks or relies on the host
         * associated with the incoming request.
         */
        changeOrigin: true,
      },
    },
  },
});