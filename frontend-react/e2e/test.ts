import {
    test as base,
    PlaywrightTestArgs,
    PlaywrightTestOptions,
    PlaywrightWorkerArgs,
    PlaywrightWorkerOptions,
} from "@playwright/test";

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

export type TestArgs<P extends keyof PlaywrightAllTestArgs> = Pick<
    PlaywrightAllTestArgs,
    P
> &
    CustomFixtures;

export const test = base.extend<CustomFixtures>({
    // Define an option and provide a default value.
    // We can later override it in the config.
    adminLogin: [
        {
            password: "",
            username: "",
            totpCode: "",
            path: "e2e/.auth/admin.json",
            landingPage: "/admin/settings",
        },
        { option: true },
    ],
    senderLogin: [
        {
            password: "",
            username: "",
            totpCode: "",
            path: "e2e/.auth/admin.json",
            landingPage: "/submissions",
        },
        { option: true },
    ],
    receiverLogin: [
        {
            password: "",
            username: "",
            totpCode: "",
            path: "e2e/.auth/admin.json",
            landingPage: "/",
        },
        { option: true },
    ],
    isMockDisabled: false,
});

export { expect } from "@playwright/test";
