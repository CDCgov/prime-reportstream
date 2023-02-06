import { OktaAuth } from "@okta/okta-auth-js";

import { OKTA_AUTH } from "../oktaConfig";

let sessionBroadcastChannel: BroadcastChannel | null = null;

const SESSION_CHANNEL_NAME = "session";

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
    getSessionBroadcastChannel()?.postMessage(SessionEvent.LOGOUT);
    localStorage.clear();
}

export function initializeSessionBroadcastChannel(oktaAuth: OktaAuth) {
    if (sessionBroadcastChannel) {
        return sessionBroadcastChannel; // don't reinitialize
    }

    sessionBroadcastChannel = new BroadcastChannel(SESSION_CHANNEL_NAME);
    sessionBroadcastChannel.onmessage = function () {
        oktaAuth.authStateManager.updateAuthState();
    };

    return sessionBroadcastChannel;
}

export function closeSessionBroadcastChannel() {
    sessionBroadcastChannel?.close();
    sessionBroadcastChannel = null;
}

export function getSessionBroadcastChannel() {
    return sessionBroadcastChannel;
}

export { logout };
