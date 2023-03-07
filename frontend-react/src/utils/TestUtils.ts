import { AccessToken } from "@okta/okta-auth-js";

export const mockToken = (mock?: Partial<AccessToken>): AccessToken => {
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
        .spyOn(console, "error")
        .mockImplementation((message: any) => {
            if (
                !matchers.find((matcher) =>
                    message.toString().includes(matcher)
                )
            ) {
                origConsole.error(message);
            }
        });
    const jestWarn = jest
        .spyOn(console, "warn")
        .mockImplementation((message: any) => {
            if (
                !matchers.find((matcher) =>
                    message.toString().includes(matcher)
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

/**
 * Future updates to browser Intl will eventually include a change to strings where
 * some spaces will be unicode no-break spaces (u+202F / charCode 8239) instead of
 * regular spaces (charCode 32). This apparently already happened in the browser
 * space but was quickly reverted due to breaking WebCompat. Node 18.14.2
 * implements this change which will break tests doing string comparisons of Intl
 * output with developer-crafted comparisons. A future update to Node may revert
 * this change as well if this was reverted in V8 itself. In either case, this
 * helper will be important for the eventual transition period if they persue this
 * again. Use this helper function preferably within a mock implementation or
 * proxy object handler so the strings are passed on naturally.
 */
export function replaceUnicodeNarrowNoBreakSpaces(str: string) {
    return str.replaceAll(String.fromCharCode(8239), String.fromCharCode(32));
}

/**
 * Create a proxy of a Node DateTimeFormat object so we can replace unicode no-break
 * strings before returning.
 */
export function createProxyNodeDateTimeFormatter(
    formatter: Intl.DateTimeFormat
) {
    return new Proxy(formatter, {
        get(target, p) {
            const targets: (string | symbol)[] = [
                "format",
                "formatRange",
            ] as (keyof Intl.DateTimeFormat)[];

            if (targets.includes(p)) {
                return (...args: any[]) =>
                    replaceUnicodeNarrowNoBreakSpaces(target.format(...args));
            }

            return (target as any)[p];
        },
    });
}
