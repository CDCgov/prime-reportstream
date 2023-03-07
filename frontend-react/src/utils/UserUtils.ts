import { OKTA_AUTH } from "../oktaConfig";

export enum SessionEvent {
    LOGIN = "login",
    LOGOUT = "logout",
}

export async function getUserEmail(): Promise<string | undefined> {
    const { idToken } = await OKTA_AUTH.tokenManager.getTokens();
    const { email } = idToken?.claims ?? {};
    return email;
}

async function logout(): Promise<void> {
    try {
        await OKTA_AUTH.signOut();
    } catch (e) {
        console.trace(e);
    }
    localStorage.clear();
}

export { logout };
