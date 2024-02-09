import "@testing-library/jest-dom";
import { cleanup } from "@testing-library/react";

vi.stubGlobal("scrollTo", vi.fn());
vi.mock("./oktaConfig");

afterEach(() => cleanup());
