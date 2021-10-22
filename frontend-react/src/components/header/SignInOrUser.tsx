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
            <Button type="button" outline>
                Log in
            </Button>
        </NavLink>
    );
};

export { SignInOrUser };
