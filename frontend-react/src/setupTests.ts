import "jest-canvas-mock";
import "@testing-library/jest-dom";
import { TextEncoder } from "util";
import "whatwg-fetch";
import { cleanup } from "@testing-library/react";

global.TextEncoder = TextEncoder;
global.scrollTo = jest.fn();

afterEach(() => cleanup());
