import { useOktaAuth } from "@okta/okta-react";
import { Button } from "@trussworks/react-uswds";
import { useEffect, useState } from "react";
import { NavLink } from "react-router-dom";
import { GLOBAL_STORAGE_KEYS } from "../GlobalContextProvider";

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

    const clearGlobalContext = () => {
        for (let key in localStorage) {
            if (key.includes(GLOBAL_STORAGE_KEYS.GLOBAL_BASE)) localStorage.removeItem(key)
        }
    }

    return authState?.isAuthenticated ? (
        <div className="prime-user-account">
            <span id="emailUser">{user}</span>
            <br />
            <a
                href="/"
                id="logout"
                onClick={() => {
                    clearGlobalContext()// Clears items stored for GlobalContext
                    oktaAuth.signOut()
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

export { SignInOrUser }