import "jest-canvas-mock";
import "@testing-library/jest-dom";
import { TextEncoder } from "util";
import "whatwg-fetch";

global.TextEncoder = TextEncoder;
global.scrollTo = jest.fn();
