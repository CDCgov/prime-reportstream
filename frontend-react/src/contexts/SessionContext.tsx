import React, { createContext, useContext, useMemo } from "react";
import { IOktaContext } from "@okta/okta-react/bundles/types/OktaContext";
import { AccessToken } from "@okta/okta-auth-js";

import {
    MembershipSettings,
    useOktaMemberships,
    MembershipAction,
    MemberType,
} from "../hooks/UseOktaMemberships";
import {
    ServiceAction,
    ServiceSettings,
    useServiceSettings,
} from "../hooks/UseServiceSettings";

export interface RSSessionContext {
    activeMembership?: MembershipSettings | null; // Org name and Membership type
    oktaToken?: Partial<AccessToken>;
    services?: ServiceSettings; // Sender and Receiver settings
    updateServices: React.Dispatch<ServiceAction>; // Dispatch for services
    updateMembership: React.Dispatch<MembershipAction>; // Dispatch for membership
    initialized: boolean;
    isAdminStrictCheck?: boolean;
}

export type OktaHook = (_init?: Partial<IOktaContext>) => IOktaContext;

interface ISessionProviderProps {
    oktaHook: OktaHook;
}

export const SessionContext = createContext<RSSessionContext>({
    oktaToken: {} as Partial<AccessToken>,
    activeMembership: {} as MembershipSettings,
    services: undefined,
    updateServices: () => {},
    updateMembership: () => {},
    initialized: false,
    isAdminStrictCheck: false,
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
        state: { activeMembership, initialized },
        dispatch: updateMembership,
    } = useOktaMemberships(authState);
    const { state: services, dispatch: updateServices } = useServiceSettings();
    /* This logic is a for when admins have other orgs present on their Okta claims
     * that interfere with the activeMembership.memberType "soft" check */
    const isAdminStrictCheck = useMemo(() => {
        return activeMembership?.memberType === MemberType.PRIME_ADMIN;
    }, [activeMembership?.memberType]);

    return (
        <SessionContext.Provider
            value={{
                oktaToken: authState?.accessToken,
                activeMembership,
                updateMembership,
                services,
                updateServices,
                isAdminStrictCheck,
                initialized: authState !== null && !!initialized,
            }}
        >
            {children}
        </SessionContext.Provider>
    );
};

export const useSessionContext = () => useContext(SessionContext);

export default SessionProvider;
