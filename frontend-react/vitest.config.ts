/// <reference types="vitest" />
import { defineConfig } from "vitest/config";
import dotenv from "dotenv";

// Force usage of custom environment files
dotenv.config({ path: `.env.${process.env.NODE_ENV}` });

export default defineConfig({
    test: {
        globals: true,
        environment: "jsdom",
        setupFiles: ["src/setupTests.ts"],
        mockReset: true,
    },
});
