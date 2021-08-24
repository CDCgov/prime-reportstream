import { Redirect } from "react-router-dom";
import OktaSignInWidget from "../components/OktaSignInWidget";
import { useOktaAuth } from "@okta/okta-react";
import { groupToOrg } from "../webreceiver-utils";

export const Login = ({ config }) => {
    const { oktaAuth, authState } = useOktaAuth();

    const onSuccess = (tokens) => {
        oktaAuth.handleLoginRedirect(tokens);
        console.log(tokens);
        let organization = tokens?.accessToken?.claims?.organization[0];
        console.log(`organization = ${organization}`);

        console.log(`g2o = ${groupToOrg(organization)}`);
    };

    const onError = (err) => {
        console.log("error logging in", err);
    };

    return authState && authState.isAuthenticated ? (
        <Redirect to={{ pathname: "/" }} />
    ) : (
        <OktaSignInWidget
            config={config}
            onSuccess={onSuccess}
            onError={onError}
        />
    );
};
