import React, {
    createContext,
    useContext,
    useEffect,
    useMemo,
    useState,
} from "react";
import { IOktaContext } from "@okta/okta-react/bundles/types/OktaContext";
import { AccessToken, CustomUserClaims, UserClaims } from "@okta/okta-auth-js";

import {
    MembershipSettings,
    useOktaMemberships,
    MembershipAction,
    MemberType,
} from "../hooks/UseOktaMemberships";

export interface RSSessionContext {
    activeMembership?: MembershipSettings | null;
    oktaToken?: Partial<AccessToken>;
    dispatch: React.Dispatch<MembershipAction>;
    initialized: boolean;
    isAdminStrictCheck?: boolean;
    user?: UserClaims<CustomUserClaims>;
}

export type OktaHook = (_init?: Partial<IOktaContext>) => IOktaContext;

interface ISessionProviderProps {
    oktaHook: OktaHook;
}

export const SessionContext = createContext<RSSessionContext>({
    oktaToken: {} as Partial<AccessToken>,
    activeMembership: {} as MembershipSettings,
    dispatch: () => {},
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
    const { authState, oktaAuth } = oktaHook();
    const {
        state: { activeMembership, initialized },
        dispatch,
    } = useOktaMemberships(authState);
    /* This logic is a for when admins have other orgs present on their Okta claims
     * that interfere with the activeMembership.memberType "soft" check */
    const isAdminStrictCheck = useMemo(() => {
        return activeMembership?.memberType === MemberType.PRIME_ADMIN;
    }, [activeMembership?.memberType]);
    const [user, setUser] = useState<UserClaims<CustomUserClaims>>();

    useEffect(() => {
        if (authState?.isAuthenticated) {
            const getUser = async () => {
                setUser(await oktaAuth.getUser());
            };
            getUser();
        } else {
            setUser(undefined);
        }
    }, [authState?.isAuthenticated, oktaAuth]);

    const context = useMemo(
        () => ({
            oktaToken: authState?.accessToken,
            activeMembership,
            isAdminStrictCheck,
            dispatch,
            initialized: authState !== null && !!initialized,
            user,
        }),
        [
            activeMembership,
            authState,
            dispatch,
            initialized,
            isAdminStrictCheck,
            user,
        ]
    );

    return (
        <SessionContext.Provider value={context}>
            {children}
        </SessionContext.Provider>
    );
};

export const useSessionContext = () => useContext(SessionContext);

export default SessionProvider;
