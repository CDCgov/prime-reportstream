import { test as base } from "@playwright/test";

export interface TestLogin {
    username: string;
    password: string;
    totpCode: string;
}

export type TestOptions = {
    adminLogin: TestLogin;
    senderLogin: TestLogin;
    receiverLogin: TestLogin;
};

export const test = base.extend<TestOptions>({
    // Define an option and provide a default value.
    // We can later override it in the config.
    adminLogin: [
        { password: "", username: "", totpCode: "" },
        { option: true },
    ],
    senderLogin: [
        { password: "", username: "", totpCode: "" },
        { option: true },
    ],
    receiverLogin: [
        { password: "", username: "", totpCode: "" },
        { option: true },
    ],
});
