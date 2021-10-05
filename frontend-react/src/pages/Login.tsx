// @ts-nocheck // TODO: fix types in this file
import { Link, Redirect } from "react-router-dom";
import OktaSignInWidget from "../components/OktaSignInWidget";
import { useOktaAuth } from "@okta/okta-react";
import { groupToOrg } from "../webreceiver-utils";
import { SiteAlert } from "@trussworks/react-uswds";
import { useGlobalContext } from "../components/GlobalContextProvider";
import { PERMISSIONS } from '../resources/PermissionsResource'
import { Tokens } from "@okta/okta-auth-js";

export const Login = ({ config }) => {
    const { oktaAuth, authState } = useOktaAuth();
    const { updateOrganization } = useGlobalContext();

    const onSuccess = (tokens: Tokens | undefined) => {
        oktaAuth.handleLoginRedirect(tokens);
        let organization = groupToOrg(tokens?.accessToken?.claims?.organization[0]);
        if (organization === PERMISSIONS.PRIME_ADMIN) organization = groupToOrg(tokens?.accessToken?.claims?.organization[1]);
        updateOrganization(organization)
    };

    const onError = (err: any) => {
        console.log("error logging in", err);
    };

    const MonitoringAlert = () => {
        return (
            <SiteAlert variant="info" heading="This is a United States government service" className="margin-bottom-3 tablet:margin-bottom-6" >
                Your use indicates your consent to monitoring, recording, and no expectation of privacy. Misuse is subject to criminal and civil penalties. By logging in, you are agreeing to our <Link to="/terms-of-service">terms of service.</Link>
            </SiteAlert>
        )
    }

    return authState && authState.isAuthenticated ? (
        <Redirect to={{ pathname: "/" }} />
    ) : (
        <>
            <MonitoringAlert />
            <OktaSignInWidget
                config={config}
                onSuccess={onSuccess}
                onError={onError}
            />
        </>
    );
};
