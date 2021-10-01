import { useOktaAuth } from "@okta/okta-react";
import { Button, Link } from "@trussworks/react-uswds";
import { useEffect, useState } from "react";

const SignInOrUser = () => {
    const { oktaAuth, authState } = useOktaAuth();
    const [user, setUser] = useState("");

    useEffect(() => {
        if (authState && authState.isAuthenticated) {
            oktaAuth
                .getUser()
                .then((cl) => setUser(cl.email ? cl.email : "unknown user"));
        }
    });

    return authState && authState.isAuthenticated ? (
        <div className="prime-user-account">
            <span id="emailUser">{user ? user : ""}</span>
            <br />
            <a
                href="/"
                id="logout"
                onClick={() => oktaAuth.signOut()}
                className="usa-link"
            >
                Log out
            </a>
        </div>
    ) : (
        <Link href="/daily-data">
            <Button type="button" outline>
                Log in
            </Button>
        </Link>
    );
};

export { SignInOrUser }