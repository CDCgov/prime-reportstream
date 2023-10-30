import { AccessToken } from "@okta/okta-auth-js";
import { vi } from "vitest";

export const mockAccessToken = (mock?: Partial<AccessToken>): AccessToken => {
    return {
        authorizeUrl: mock?.authorizeUrl || "",
        expiresAt: mock?.expiresAt || 0,
        scopes: mock?.scopes || [],
        userinfoUrl: mock?.userinfoUrl || "",
        accessToken: mock?.accessToken || "",
        claims: mock?.claims || { sub: "" },
        tokenType: mock?.tokenType || "",
    };
};

export const mockEvent = (mock?: Partial<any>) => {
    return {
        response: mock?.response || null,
    };
};

export function conditionallySuppressConsole(...matchers: string[]) {
    const { error, warn } = global.console;
    const mockError = vi.fn(),
        mockWarn = vi.fn();
    global.console = {
        ...global.console,
        error: mockError,
        warn: mockWarn,
    };
    const jestError = mockError.mockImplementation((message: any) => {
        if (!matchers.find((matcher) => message.toString().includes(matcher))) {
            error(message);
        }
    });
    const jestWarn = mockWarn.mockImplementation((message: any) => {
        if (!matchers.find((matcher) => message.toString().includes(matcher))) {
            warn(message);
        }
    });

    return () => {
        jestError.mockRestore();
        jestWarn.mockRestore();
    };
}
