import React, { createContext, useContext, useMemo, useState } from "react";
import { IOktaContext } from "@okta/okta-react/bundles/types/OktaContext";
import { AccessToken } from "@okta/okta-auth-js";

import {
    MembershipSettings,
    useOktaMemberships,
    MembershipAction,
    MemberType,
} from "../hooks/UseOktaMemberships";

export interface RSSessionContext {
    memberships?: Map<string, MembershipSettings>;
    activeMembership?: MembershipSettings | null;
    oktaToken?: Partial<AccessToken>;
    dispatch: React.Dispatch<MembershipAction>;
    initialized: boolean;
    isAdminStrictCheck?: boolean;
    sessionStartTime: Date;
    setSessionStartTime: (date: Date) => void;
    sessionTimeAggregate: number;
    setSessionTimeAggregate: (timeInSeconds: number) => void;
}

export type OktaHook = (_init?: Partial<IOktaContext>) => IOktaContext;

interface ISessionProviderProps {
    oktaHook: OktaHook;
}

export const SessionContext = createContext<RSSessionContext>({
    oktaToken: {} as Partial<AccessToken>,
    memberships: new Map(),
    activeMembership: {} as MembershipSettings,
    dispatch: () => {},
    initialized: false,
    isAdminStrictCheck: false,
    sessionStartTime: new Date(),
    setSessionStartTime: () => {},
    sessionTimeAggregate: 0,
    setSessionTimeAggregate: () => {},
});

// accepts `oktaHook` as a parameter in order to allow mocking of this provider's okta based
// behavior for testing. In non test cases this hook will be the `useOktaAuth` hook from
// `okta-react`
const SessionProvider = ({
    children,
    oktaHook,
}: React.PropsWithChildren<ISessionProviderProps>) => {
    const { authState } = oktaHook();

    const {
        state: { memberships, activeMembership, initialized },
        dispatch,
    } = useOktaMemberships(authState);
    /* This logic is a for when admins have other orgs present on their Okta claims
     * that interfere with the activeMembership.memberType "soft" check */
    const isAdminStrictCheck = useMemo(() => {
        return activeMembership?.memberType === MemberType.PRIME_ADMIN;
    }, [activeMembership?.memberType]);

    const updateSessionStartTime = (date: Date) => {
        setState((prevState) => {
            return { ...prevState, sessionStartTime: date };
        });
    };

    const updateSessionTimeAggregate = (timeInSeconds: number) => {
        setState((prevState) => {
            return { ...prevState, sessionTimeAggregate: timeInSeconds };
        });
    };

    const [state, setState] = useState<RSSessionContext>({
        oktaToken: authState?.accessToken,
        memberships,
        activeMembership,
        isAdminStrictCheck,
        dispatch,
        initialized: authState !== null && !!initialized,
        sessionStartTime: new Date(),
        setSessionStartTime: updateSessionStartTime,
        sessionTimeAggregate: 0,
        setSessionTimeAggregate: updateSessionTimeAggregate,
    });

    return (
        <SessionContext.Provider value={state}>
            {children}
        </SessionContext.Provider>
    );
};

export const useSessionContext = () => useContext(SessionContext);

export default SessionProvider;
