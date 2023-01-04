import { useOktaAuth } from "@okta/okta-react";
import { Button } from "@trussworks/react-uswds";
import { useEffect, useState } from "react";

import { logout } from "../../utils/UserUtils";
import config from "../../config";
import { USLink } from "../USLink";

const { IS_PREVIEW } = config;

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
            {/* Okta handles our logoutRedirectUri, this should _not_ be an anchor tag! */}
            <Button
                id="logout"
                type="button"
                unstyled
                onClick={() => {
                    logout(oktaAuth);
                }}
            >
                Log out
            </Button>
        </div>
    ) : (
        <USLink href="/login">
            {/* Does not need to be a NavLink, as "Login" does not present the need for activeClassName utility */}
            <Button type="button" inverse={IS_PREVIEW}>
                Log in {IS_PREVIEW ? "via OktaPreview" : ""}
            </Button>
        </USLink>
    );
};

export { SignInOrUser };
