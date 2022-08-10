import React, { createContext, useContext } from "react";
import { IOktaContext } from "@okta/okta-react/bundles/types/OktaContext";
import { AccessToken } from "@okta/okta-auth-js";

// import useSessionStorage, {
//     SessionController,
// } from "../hooks/UseSessionStorage";
// import {
//     MembershipController,
//     useSession,
//     MembershipAction,
// } from "../hooks/UseSession";
import {
    MembershipSettings,
    // MembershipController,
    useOktaMemberships,
    MembershipAction,
} from "../hooks/UseOktaMemberships";

export interface ISessionContext {
    memberships?: Map<string, MembershipSettings>;
    // store: SessionController;
    // org: string;
    // senderName: string;
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
    // store: {} as SessionController,
});

// accepts `oktaHook` as a parameter in order to allow mocking of this provider's okta based
// behavior for testing. In non test cases this hook will be the `useOktaAuth` hook from
// `okta-react`
const SessionProvider = ({
    children,
    oktaHook,
}: React.PropsWithChildren<ISessionProviderProps>) => {
    // todo, watch authState to look for logouts
    const { authState } = oktaHook();

    // const store = useSessionStorage();
    // const memberships = useOktaMemberships(authState?.accessToken);

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
                // store: store,
            }}
        >
            {children}
        </SessionContext.Provider>
    );
};

export const useSessionContext = () => useContext(SessionContext);

export default SessionProvider;
