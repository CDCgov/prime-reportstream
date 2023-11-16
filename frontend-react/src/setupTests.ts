import "@testing-library/jest-dom/vitest";
import { TextEncoder } from "util";

import "whatwg-fetch";

global.TextEncoder = TextEncoder;
vi.stubGlobal("scrollTo", vi.fn());

vi.mock("@okta/okta-react");
vi.mock("react-router-dom");
vi.mock("react-helmet-async");
vi.mock("./contexts/Session");
vi.mock("./contexts/AppInsights");
vi.mock("./contexts/FeatureFlags");
vi.mock("./contexts/Toast");
//vi.mock("./utils/console");
