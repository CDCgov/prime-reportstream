import { OKTA_AUTH } from "../oktaConfig";

export enum SessionEvent {
    LOGIN = "login",
    LOGOUT = "logout",
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
