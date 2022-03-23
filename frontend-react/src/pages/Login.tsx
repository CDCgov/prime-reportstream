import React, { useContext } from "react";
import { Link, Redirect } from "react-router-dom";
import { useOktaAuth } from "@okta/okta-react";
import { SiteAlert } from "@trussworks/react-uswds";
import { Tokens } from "@okta/okta-auth-js";

import OktaSignInWidget from "../components/OktaSignInWidget";
import { getOrganizationFromAccessToken } from "../webreceiver-utils";
import { parseOrgs, setStoredOktaToken } from "../contexts/SessionStorageTools";
import { oktaSignInConfig } from "../oktaConfig";
import { SessionStorageContext } from "../contexts/SessionStorageContext";
import { PERMISSIONS } from "../resources/PermissionsResource";

export const Login = () => {
    const { oktaAuth, authState } = useOktaAuth();
    const { updateSessionStorage } = useContext(SessionStorageContext);

    const onSuccess = (tokens: Tokens | undefined) => {
        const parsedOrgs = parseOrgs(
            getOrganizationFromAccessToken(tokens?.accessToken)
        );
        const newOrg = parsedOrgs[0].org || "";
        const newSender = parsedOrgs[0].senderName || undefined;
        updateSessionStorage({
            // Sets admins to `ignore` org
            org: newOrg.includes(PERMISSIONS.PRIME_ADMIN) ? "ignore" : newOrg,
            senderName: newSender,
        });
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
