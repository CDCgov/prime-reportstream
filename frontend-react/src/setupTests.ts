import "jest-canvas-mock";
import "@testing-library/jest-dom";
import { TextEncoder } from "util";
import "whatwg-fetch";

import ResizeObserver from "resize-observer-polyfill";
import type { Config } from "@jest/types";

global.TextEncoder = TextEncoder;

// Sync object
const config: Config.InitialOptions = {
    verbose: true,
};
export default config;

global.scrollTo = jest.fn();
global.ResizeObserver = ResizeObserver;
