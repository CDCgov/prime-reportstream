import {
    test as base,
    PlaywrightTestArgs,
    PlaywrightTestOptions,
    PlaywrightWorkerArgs,
    PlaywrightWorkerOptions,
} from "@playwright/test";
import { join } from "node:path";

export interface TestLogin {
    username: string;
    password: string;
    totpCode?: string;
    path: string;
    landingPage: string;
}

export interface CustomFixtures {
    adminLogin: TestLogin;
    senderLogin: TestLogin;
    receiverLogin: TestLogin;
    isMockDisabled: boolean;
}

export type PlaywrightAllTestArgs = PlaywrightTestArgs &
    PlaywrightTestOptions &
    PlaywrightWorkerArgs &
    PlaywrightWorkerOptions;

const isMockDisabled = Boolean(process.env.MOCK_DISABLED);

function createLogins<const T extends string[]>(
    loginTypes: T,
): {
    [K in T extends readonly (infer U)[] ? U : never]: {
        username: string;
        password: string;
        totpCode: string;
        path: string;
    };
} {
    const logins = Object.fromEntries(
        loginTypes.map((type) => {
            const username = process.env[`TEST_${type.toUpperCase()}_USERNAME`];
            const password = process.env[`TEST_${type.toUpperCase()}_PASSWORD`];
            const totpCode =
                process.env[`TEST_${type.toUpperCase()}_TOTP_CODE`];

            if (!username)
                throw new TypeError(`Missing username for login type: ${type}`);
            if (!password)
                throw new TypeError(`Missing password for login type: ${type}`);

            return [
                type,
                {
                    username,
                    password,
                    totpCode,
                    path: join(import.meta.dirname, `.auth/${type}.json`),
                },
            ];
        }),
    );
    return logins as any;
}

export const logins = createLogins(["admin", "receiver", "sender"]);

export const test = base.extend<CustomFixtures>({
    adminLogin: {
        ...logins.admin,
        landingPage: "/admin/settings",
    },
    senderLogin: {
        ...logins.sender,
        landingPage: "/submissions",
    },
    receiverLogin: {
        ...logins.receiver,
        landingPage: "/",
    },
    isMockDisabled,
});

export type TestArgs<P extends keyof PlaywrightAllTestArgs> = Pick<
    PlaywrightAllTestArgs,
    P
> &
    CustomFixtures;

export { expect } from "@playwright/test";
