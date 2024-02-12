import "jest-canvas-mock";
import "@testing-library/jest-dom";
import { cleanup } from "@testing-library/react";
import { createMocks } from "react-idle-timer";
import { TextEncoder } from "util";
import "whatwg-fetch";
import { MessageChannel } from "worker_threads";

beforeAll(() => {
    createMocks();
    // @ts-expect-error ignore global
    global.MessageChannel = MessageChannel;
});

afterAll(cleanup);

global.TextEncoder = TextEncoder;
global.scrollTo = jest.fn();

afterEach(() => cleanup());
