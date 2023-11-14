import React, { createContext, useContext } from "react";
import OktaAuth, {
    CustomUserClaims,
    UserClaims,
    AuthState,
} from "@okta/okta-auth-js";
import { Security } from "@okta/okta-react";

import { RSUserPermissions } from "../../utils/PermissionsUtils";
import { MembershipSettings } from "../../utils/OrganizationUtils";
import type { AppConfig } from "../../config";
import site from "../../content/site.json";
import { RSConsole } from "../../utils/console";

import SessionAuthStateGate from "./SessionAuthStateGate";

export interface SessionCtx {
    oktaAuth: OktaAuth;
    authState: AuthState;
    activeMembership?: MembershipSettings;
    _activeMembership?: MembershipSettings;
    user: {
        claims?: UserClaims<CustomUserClaims>;
        isAdminStrictCheck: boolean;
        isUserTransceiver: boolean;
    } & RSUserPermissions;
    logout: () => void;
    setActiveMembership: (value: Partial<MembershipSettings> | null) => void;
    config: AppConfig;
    site: typeof site;
    rsconsole: RSConsole;
}

export const SessionContext = createContext<SessionCtx>({
    activeMembership: {} as MembershipSettings,
    logout: () => void 0,
    setActiveMembership: () => void 0,
} as any);

export interface SessionProviderProps
    extends React.ComponentProps<typeof Security> {
    config: AppConfig;
}

function SessionProvider({ children, config, ...props }: SessionProviderProps) {
    return (
        <Security {...props}>
            <SessionAuthStateGate config={config}>
                {children}
            </SessionAuthStateGate>
        </Security>
    );
}

export const useSessionContext = () => useContext(SessionContext);

export default SessionProvider;
