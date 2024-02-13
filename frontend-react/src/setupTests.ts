import "@testing-library/jest-dom";
import { cleanup } from "@testing-library/react";
import { createMocks } from "react-idle-timer";
import { MessageChannel } from "worker_threads";

vi.stubGlobal("scrollTo", vi.fn());
vi.mock("./oktaConfig");

beforeAll(() => {
    createMocks();
    // @ts-expect-error ignore global
    global.MessageChannel = MessageChannel;
});

afterAll(cleanup);
