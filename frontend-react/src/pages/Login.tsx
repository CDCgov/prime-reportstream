import React from "react";
import { useOktaAuth } from "@okta/okta-react";
import { Navigate } from "react-router-dom";
import type { Tokens } from "@okta/okta-auth-js";

import Alert from "../shared/Alert/Alert";
import { oktaSignInConfig } from "../oktaConfig";
import { useSessionContext } from "../contexts/SessionContext";
import { USLink } from "../components/USLink";
import { MembershipActionType } from "../hooks/UseOktaMemberships";
import OktaSignInWidget from "../shared/OktaSignInWidget/OktaSignInWidget";

export const Login = () => {
    const { oktaAuth, authState } = useOktaAuth();
    const { dispatch } = useSessionContext();

    const onSuccess = React.useCallback(
        (tokens: Tokens) => {
            oktaAuth.handleLoginRedirect(tokens);
            return tokens;
        },
        [oktaAuth],
    );

    const onError = React.useCallback(
        (_: any) => {
            dispatch({
                type: MembershipActionType.RESET,
            });
        },
        [dispatch],
    );

    if (authState?.isAuthenticated) {
        return <Navigate replace to={"/"} />;
    }

    return (
        <>
            <Alert type="info" heading="Changes to ReportStream login">
                Your login information may have expired due to recent updates to
                ReportStream's system. <br />
                Check your email for an activation link and more information.
            </Alert>
            <OktaSignInWidget
                className="margin-top-6 margin-x-auto width-mobile-lg padding-x-8"
                config={oktaSignInConfig}
                onSuccess={onSuccess}
                onError={onError}
            >
                <div className="margin-bottom-5 font-sans-3xs">
                    This is a U.S. government service. Your use indicates your
                    consent to monitoring, recording, and no expectation of
                    privacy. Misuse is subject to criminal and civil penalties.
                    By logging in, you are agreeing to our{" "}
                    <USLink href="/terms-of-service">terms of service.</USLink>
                </div>
            </OktaSignInWidget>
        </>
    );
};
