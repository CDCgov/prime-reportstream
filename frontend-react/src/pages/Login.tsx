import { Link, Redirect } from "react-router-dom";
import OktaSignInWidget from "../components/OktaSignInWidget";
import { useOktaAuth } from "@okta/okta-react";
import { groupToOrg } from "../webreceiver-utils";
import { SiteAlert } from "@trussworks/react-uswds";
import { useGlobalContext } from "../components/GlobalContextProvider";
import { PERMISSIONS } from '../resources/PermissionsResource'

export const Login = ({ config }) => {
    const { oktaAuth, authState } = useOktaAuth();
    const { updateOrganization } = useGlobalContext();

    const onSuccess = (tokens) => {
        oktaAuth.handleLoginRedirect(tokens);

        console.log(tokens);
        let organization = groupToOrg(tokens?.accessToken?.claims?.organization[0]);
        if (organization === PERMISSIONS.PRIME_ADMIN) organization = groupToOrg(tokens?.accessToken?.claims?.organization[1]);
        updateOrganization(organization)
        console.log(`organization = ${organization}`);
        console.log(`g2o = ${groupToOrg(organization)}`);
    };

    const onError = (err) => {
        console.log("error logging in", err);
    };

    const MonitoringAlert = () => {
        return (
            <SiteAlert variant="info" heading="This is a U.S. government service" className="margin-bottom-3 tablet:margin-bottom-6" >
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
