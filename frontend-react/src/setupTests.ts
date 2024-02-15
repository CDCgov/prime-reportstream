/* eslint-disable @typescript-eslint/no-var-requires */
/* eslint-disable import/order */
import "jest-canvas-mock";
import "@testing-library/jest-dom";
import { cleanup } from "@testing-library/react";
import { createMocks } from "react-idle-timer";
import { MessageChannel } from "worker_threads";

beforeAll(() => {
    createMocks();
    // @ts-expect-error ignore global
    global.MessageChannel = MessageChannel;
    jest.mock("@microsoft/applicationinsights-react-js");
    jest.mock("@okta/okta-react");
});

afterAll(cleanup);

global.scrollTo = jest.fn();

/**
 * @see https://mswjs.io/docs/migrations/1.x-to-2.x/#requestresponsetextencoder-is-not-defined-jest
 * @note The block below contains polyfills for Node.js globals
 * required for Jest to function when running JSDOM tests.
 * These HAVE to be require's and HAVE to be in this exact
 * order, since "undici" depends on the "TextEncoder" global API.
 *
 * Consider migrating to a more modern test runner if
 * you don't want to deal with this.
 */
const { TextDecoder, TextEncoder } = require("node:util");

Object.defineProperties(globalThis, {
    TextDecoder: { value: TextDecoder },
    TextEncoder: { value: TextEncoder },
});

const { Blob, File } = require("node:buffer");
const {
    fetch,
    Headers,
    FormData,
    Request,
    Response,
    FileReader,
} = require("undici");

Object.defineProperties(globalThis, {
    fetch: { value: fetch, writable: true },
    Blob: { value: Blob },
    File: { value: File },
    Headers: { value: Headers },
    FormData: { value: FormData },
    Request: { value: Request },
    Response: { value: Response },
    FileReader: { value: FileReader },
});
