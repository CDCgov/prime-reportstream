import React from "react";
import { useOktaAuth } from "@okta/okta-react";
import { SiteAlert } from "@trussworks/react-uswds";
import { Tokens } from "@okta/okta-auth-js";
import { Navigate } from "react-router-dom";

import OktaSignInWidget from "../components/OktaSignInWidget";
import { oktaSignInConfig } from "../oktaConfig";
import { useSessionContext } from "../contexts/SessionContext";
import { MembershipActionType } from "../hooks/UseOktaMemberships";
import { USLink } from "../components/USLink";

const MonitoringAlert = () => {
    return (
        <SiteAlert
            variant="info"
            heading="This is a U.S. government service"
            className="margin-top-neg-4 desktop:margin-top-neg-5 margin-bottom-3 tablet:margin-bottom-6"
        >
            Your use indicates your consent to monitoring, recording, and no
            expectation of privacy. Misuse is subject to criminal and civil
            penalties. By logging in, you are agreeing to our{" "}
            <USLink href="/terms-of-service">terms of service.</USLink>
        </SiteAlert>
    );
};

export const Login = () => {
    const { oktaAuth, authState } = useOktaAuth();
    const { dispatch } = useSessionContext();

    const onSuccess = (tokens: Tokens | undefined) => {
        oktaAuth.handleLoginRedirect(tokens);
    };

    const onError = (err: any) => {
        dispatch({
            type: MembershipActionType.RESET,
        });
        console.error("error logging in", err);
    };

    if (authState?.isAuthenticated) {
        return <Navigate replace to={"/"} />;
    }

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
