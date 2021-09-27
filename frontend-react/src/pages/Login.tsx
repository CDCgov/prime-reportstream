import { Link, Redirect } from "react-router-dom";
import OktaSignInWidget from "../components/OktaSignInWidget";
import { useOktaAuth } from "@okta/okta-react";
import { groupToOrg } from "../webreceiver-utils";
import { Alert } from "@trussworks/react-uswds";
import {Tokens} from "@okta/okta-auth-js";
import {oktaSignInConfig} from "../oktaConfig";

export const Login = () => {
    const { oktaAuth, authState } = useOktaAuth();

    const onSuccess = (tokens:Tokens) => {
        oktaAuth.handleLoginRedirect(tokens);
        console.log(tokens);
        let organization = tokens?.accessToken?.claims?.organization[0];
        console.log(`organization = ${organization}`);

        console.log(`g2o = ${groupToOrg(organization)}`);
    };

    const onError = (err: Error) => {
        console.log("error logging in", err);
    };

    const MonitoringAlert = () => {
        return (
            <Alert type="info" heading="This is a U.S. government service" className="grid-container">
                Your use indicates your consent to monitoring, recording, and no expectation of privacy. Misuse is subject to criminal and civil penalties. By logging in, you are agreeing to our <Link to="/terms-of-service">terms of service.</Link>
            </Alert>
        )
    }

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
