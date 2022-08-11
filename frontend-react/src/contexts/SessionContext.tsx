import React, { createContext, useContext, useEffect } from "react";
import { IOktaContext } from "@okta/okta-react/bundles/types/OktaContext";
import { AccessToken, OktaAuth } from "@okta/okta-auth-js";

import {
    MembershipSettings,
    useOktaMemberships,
    MembershipAction,
} from "../hooks/UseOktaMemberships";

export interface ISessionContext {
    memberships?: Map<string, MembershipSettings>;
    activeMembership?: MembershipSettings;
    oktaToken?: Partial<AccessToken>;
    dispatch: React.Dispatch<MembershipAction>;
}

export type OktaHook = (_init?: Partial<IOktaContext>) => IOktaContext;

interface ISessionProviderProps {
    oktaHook: OktaHook;
}

export const SessionContext = createContext<ISessionContext>({
    oktaToken: {} as Partial<AccessToken>,
    memberships: new Map(),
    activeMembership: {} as MembershipSettings,
    dispatch: () => {},
});

const setOktaListeners = (oktaAuth: OktaAuth) => {
    // TOKEN MANAGER EVENTS
    oktaAuth.tokenManager.on("added", () =>
        console.log("TOKEN MANAGER: added")
    );
    oktaAuth.tokenManager.on("expired", () =>
        console.log("TOKEN MANAGER: expired")
    );
    oktaAuth.tokenManager.on("error", () =>
        console.log("TOKEN MANAGER: error")
    );
    oktaAuth.tokenManager.on("renewed", () =>
        console.log("TOKEN MANAGER: renewed")
    );
    oktaAuth.tokenManager.on("removed", () =>
        console.log("TOKEN MANAGER: removed")
    );

    // AUTH STATE MANAGER EVENTS
    oktaAuth.authStateManager.subscribe((state) =>
        console.log("AUTH STATE MANAGER: state change", state)
    );
};

// accepts `oktaHook` as a parameter in order to allow mocking of this provider's okta based
// behavior for testing. In non test cases this hook will be the `useOktaAuth` hook from
// `okta-react`
const SessionProvider = ({
    children,
    oktaHook,
}: React.PropsWithChildren<ISessionProviderProps>) => {
    const { authState, oktaAuth } = oktaHook();

    let oktaInitialized = false;

    useEffect(() => {
        if (oktaAuth) {
            oktaInitialized = true;
        }
    }, [oktaAuth]);
    useEffect(() => {
        if (oktaInitialized) {
            setOktaListeners(oktaAuth);
        }
    }, [oktaInitialized]);

    const {
        state: { memberships, activeMembership },
        dispatch,
    } = useOktaMemberships(authState);
    return (
        <SessionContext.Provider
            value={{
                oktaToken: authState?.accessToken,
                memberships,
                activeMembership,
                dispatch,
            }}
        >
            {children}
        </SessionContext.Provider>
    );
};

export const useSessionContext = () => useContext(SessionContext);

export default SessionProvider;
