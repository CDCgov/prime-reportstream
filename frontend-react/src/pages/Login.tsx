import React from "react";
import { useOktaAuth } from "@okta/okta-react";
import { SiteAlert } from "@trussworks/react-uswds";
import { Tokens } from "@okta/okta-auth-js";

import OktaSignInWidget from "../components/OktaSignInWidget";
import { oktaSignInConfig } from "../oktaConfig";
import { useSessionContext } from "../contexts/SessionContext";
import { MembershipActionType } from "../hooks/UseOktaMemberships";
import { USLink } from "../components/USLink";
import { getSessionBroadcastChannel, SessionEvent } from "../utils/UserUtils";

export const Login = () => {
    const { oktaAuth } = useOktaAuth();
    const { dispatch } = useSessionContext();

    const onSuccess = (tokens: Tokens | undefined) => {
        oktaAuth.handleLoginRedirect(tokens).finally(() => {
            getSessionBroadcastChannel()?.postMessage(SessionEvent.LOGIN);
        });
    };

    const onError = (err: any) => {
        dispatch({
            type: MembershipActionType.RESET,
        });
        console.log("error logging in", err);
    };

    const MonitoringAlert = () => {
        return (
            <SiteAlert
                variant="info"
                heading="This is a U.S. government service"
                className="margin-top-neg-4 desktop:margin-top-0 margin-bottom-3 tablet:margin-bottom-6"
            >
                Your use indicates your consent to monitoring, recording, and no
                expectation of privacy. Misuse is subject to criminal and civil
                penalties. By logging in, you are agreeing to our{" "}
                <USLink href="/terms-of-service">terms of service.</USLink>
            </SiteAlert>
        );
    };

    return (
        <>
            <MonitoringAlert />
            <OktaSignInWidget
                config={oktaSignInConfig}
                onSuccess={onSuccess}
                onError={onError}
            />
        </>
    );
};
