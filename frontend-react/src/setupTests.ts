import "jest-canvas-mock";
import "@testing-library/jest-dom";
import { TextEncoder } from "util";
import "whatwg-fetch";
import { MessageChannel } from "worker_threads";

import { createMocks } from "react-idle-timer";
import { cleanup } from "@testing-library/react";

beforeAll(() => {
    createMocks();
    // @ts-ignore
    global.MessageChannel = MessageChannel;
});

afterAll(cleanup);

global.TextEncoder = TextEncoder;
global.scrollTo = jest.fn();
