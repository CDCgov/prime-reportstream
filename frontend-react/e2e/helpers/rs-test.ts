import { test as base } from "@playwright/test";

export interface TestLogin {
    username: string;
    password: string;
    totpCode?: string;
    path: string;
    landingPage: string;
}

export interface TestOptions {
    adminLogin: TestLogin;
    senderLogin: TestLogin;
    receiverLogin: TestLogin;
}

export const test = base.extend<TestOptions>({
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
});
