import "jest-canvas-mock";
import "@testing-library/jest-dom";
import type { Config } from "@jest/types";

// Sync object
const config: Config.InitialOptions = {
    verbose: true,
};
export default config;

global.scrollTo = jest.fn();
