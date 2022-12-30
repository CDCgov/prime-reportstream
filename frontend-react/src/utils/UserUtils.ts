import { OktaAuth } from "@okta/okta-auth-js";

let sessionBroadcastChannel: BroadcastChannel | null = null;

const SESSION_CHANNEL_NAME = "session";

export enum SessionEvent {
    LOGIN = "login",
    LOGOUT = "logout",
}

function logout(oktaAuth: OktaAuth): void {
    if (oktaAuth?.signOut) {
        try {
            oktaAuth.signOut().finally(() => {
                getSessionBroadcastChannel()?.postMessage(SessionEvent.LOGOUT);
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
