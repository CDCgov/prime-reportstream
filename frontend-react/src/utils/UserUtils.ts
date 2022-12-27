import { OktaAuth } from "@okta/okta-auth-js";

let sessionBroadcastChannel: BroadcastChannel | null = null;

export enum SESSION_CHANNEL {
    NAME = "session",
    LOGIN = "login",
    LOGOUT = "logout",
}

function logout(oktaAuth: OktaAuth): void {
    if (oktaAuth?.signOut) {
        try {
            oktaAuth.signOut().finally(() => {
                getSessionBroadcastChannel()?.postMessage(
                    SESSION_CHANNEL.LOGOUT
                );
            });
        } catch (e) {
            console.trace(e);
        }
    }
    localStorage.clear();
}

export function initializeSessionBroadcastChannel(oktaAuth: OktaAuth) {
    if (sessionBroadcastChannel) {
        return sessionBroadcastChannel; // don't reinitialize
    }

    sessionBroadcastChannel = new BroadcastChannel(SESSION_CHANNEL.NAME);
    sessionBroadcastChannel.onmessage = function (e) {
        oktaAuth.authStateManager.updateAuthState();
        if (e.data === "logout") closeSessionBroadcastChannel();
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
