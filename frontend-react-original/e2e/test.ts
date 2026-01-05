import {
    test as base,
    PlaywrightTestArgs,
    PlaywrightTestOptions,
    PlaywrightWorkerArgs,
    PlaywrightWorkerOptions,
} from "@playwright/test";
import { join } from "node:path";

// eslint-disable-next-line import/export
export * from "@playwright/test";

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
    frontendWarningsLogPath: string;
    isFrontendWarningsLog: boolean;
}

export type PlaywrightAllTestArgs = PlaywrightTestArgs &
    PlaywrightTestOptions &
    PlaywrightWorkerArgs &
    PlaywrightWorkerOptions;

const e2eDataPath = join(import.meta.dirname, "../e2e-data");
const isCI = Boolean(process.env.CI);
const frontendWarningsPath = join(e2eDataPath, "frontend-warnings.json");
const isMockDisabled = Boolean(process.env.MOCK_DISABLED);

function createLogins<const T extends string[]>(loginTypes: T): Record<string, TestLogin> {
    const logins = Object.fromEntries(
        loginTypes.map((type) => {
            const username = process.env[`TEST_${type.toUpperCase()}_USERNAME`];
            const password = process.env[`TEST_${type.toUpperCase()}_PASSWORD`];
            const totpCode = process.env[`TEST_${type.toUpperCase()}_TOTP_CODE`];

            if (!username) throw new TypeError(`Missing username for login type: ${type}`);
            if (!password) throw new TypeError(`Missing password for login type: ${type}`);

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
    return logins as Record<string, TestLogin>;
}

export const logins = createLogins(["admin", "receiver", "sender"]);

// eslint-disable-next-line import/export
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
    frontendWarningsLogPath: frontendWarningsPath,
    isFrontendWarningsLog: isCI,
});

export type TestArgs<P extends keyof PlaywrightAllTestArgs> = Pick<PlaywrightAllTestArgs, P> & CustomFixtures;
