import "jest-canvas-mock";
import "@testing-library/jest-dom";
import { cleanup } from "@testing-library/react";
import { TextEncoder } from "util";
import "whatwg-fetch";

global.TextEncoder = TextEncoder;
global.scrollTo = jest.fn();

afterEach(() => cleanup());
