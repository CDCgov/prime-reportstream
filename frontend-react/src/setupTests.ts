//import "jest-canvas-mock";
import "@testing-library/jest-dom/vitest";
import { TextEncoder } from "util";
import "whatwg-fetch";

global.TextEncoder = TextEncoder;
vi.stubGlobal("scrollTo", vi.fn());
