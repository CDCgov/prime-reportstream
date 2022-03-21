import React from "react";
import { Link, Redirect } from "react-router-dom";
import { useOktaAuth } from "@okta/okta-react";
import { SiteAlert } from "@trussworks/react-uswds";
import { Tokens } from "@okta/okta-auth-js";

import OktaSignInWidget from "../components/OktaSignInWidget";
import { getOrganizationFromAccessToken } from "../webreceiver-utils";
import {
    parseOrgs,
    setStoredOktaToken,
    storeParsedOrg,
} from "../contexts/SessionStorageTools";
import { oktaSignInConfig } from "../oktaConfig";

export const Login = () => {
    const { oktaAuth, authState } = useOktaAuth();

    const onSuccess = (tokens: Tokens | undefined) => {
        const parsed = parseOrgs(
            getOrganizationFromAccessToken(tokens?.accessToken)
        );
        storeParsedOrg(parsed[0]);
        setStoredOktaToken(tokens?.accessToken?.accessToken || "");
        oktaAuth.handleLoginRedirect(tokens);
    };

    const onError = (err: any) => {
        setStoredOktaToken(""); // clear on error.
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
                <Link to="/terms-of-service">terms of service.</Link>
            </SiteAlert>
        );
    };

    return authState && authState.isAuthenticated ? (
        <Redirect to={{ pathname: "/" }} />
    ) : (
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
