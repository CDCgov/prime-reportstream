import { AccessToken } from "@okta/okta-auth-js";

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
    const origConsole = jest.requireActual("console");
    const jestError = jest
        .spyOn(origConsole, "error")
        .mockImplementation((message: any) => {
            if (
                !matchers.find((matcher) =>
                    message.toString().includes(matcher),
                )
            ) {
                origConsole.error(message);
            }
        });
    const jestWarn = jest
        .spyOn(origConsole, "warn")
        .mockImplementation((message: any) => {
            if (
                !matchers.find((matcher) =>
                    message.toString().includes(matcher),
                )
            ) {
                origConsole.warn(message);
            }
        });

    return () => {
        jestError.mockRestore();
        jestWarn.mockRestore();
    };
}
