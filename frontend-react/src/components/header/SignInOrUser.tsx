import { useOktaAuth } from "@okta/okta-react";
import { Button } from "@trussworks/react-uswds";
import { useEffect, useState } from "react";
import { NavLink } from "react-router-dom";

import { logout } from "../../utils/UserUtils";

const SignInOrUser = () => {
    const { oktaAuth, authState } = useOktaAuth();
    const [user, setUser] = useState("");

    useEffect(() => {
        if (authState?.isAuthenticated) {
            oktaAuth
                .getUser()
                .then((cl) => setUser(cl.email ? cl.email : "unknown user"));
        }
    });
    const isPreview =
        `${process.env.REACT_APP_OKTA_URL}`.match(/oktapreview.com/) !== null;

    return authState?.isAuthenticated ? (
        <div className="prime-user-account">
            <span id="emailUser">{user}</span>
            <br />
            <a
                href="/"
                id="logout"
                onClick={() => {
                    logout(oktaAuth);
                }}
                className="usa-link"
            >
                Log out
            </a>
        </div>
    ) : (
        <NavLink to="/daily-data">
            <Button type="button" inverse={isPreview}>
                Log in {isPreview ? "via OktaPreview" : ""}
            </Button>
        </NavLink>
    );
};

export { SignInOrUser };
