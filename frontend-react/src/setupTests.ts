import "@testing-library/jest-dom/vitest";
import { TextEncoder } from "util";

import "whatwg-fetch";
import { render, renderHook } from "./utils/CustomRenderUtils";

global.TextEncoder = TextEncoder;
vi.stubGlobal("scrollTo", vi.fn());

vi.mock("@okta/okta-react");
vi.mock("./contexts/Session");
vi.mock("./contexts/AppInsights");
vi.mock("./contexts/FeatureFlags");
vi.mock("./contexts/Toast");
//vi.mock("./utils/console");

vi.stubGlobal("render", render);
vi.stubGlobal("renderHook", renderHook);
