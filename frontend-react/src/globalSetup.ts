import process from "node:process";

/**
 * Fill in needed env vars that would normally be injected via vite.
 * Won't be needed with vitest.
 */
function setup() {
    process.env.NEXT_PUBLIC_BACKEND_URL = "http://localhost";
}

export default setup;
