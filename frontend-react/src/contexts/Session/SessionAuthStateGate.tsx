import { useOktaAuth } from "@okta/okta-react";

import SessionProviderBase from "./SessionProviderBase";

import { SessionProviderProps } from ".";

export interface SessionAuthStateGateProps
    extends React.PropsWithChildren<Pick<SessionProviderProps, "config">> {}

export default function SessionAuthStateGate({
    children,
    config,
}: SessionAuthStateGateProps) {
    const { authState, ...props } = useOktaAuth();

    if (!authState) return null;

    return (
        <SessionProviderBase authState={authState} config={config} {...props}>
            {children}
        </SessionProviderBase>
    );
}
