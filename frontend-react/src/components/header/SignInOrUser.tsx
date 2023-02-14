import { Button } from "@trussworks/react-uswds";

import { logout } from "../../utils/UserUtils";
import config from "../../config";
import { USLinkButton } from "../USLink";
import { useSessionContext } from "../../contexts/SessionContext";

const { IS_PREVIEW } = config;

const SignInOrUser = () => {
    const { user } = useSessionContext();
    const userDisplay = user?.email ?? "unknown user";

    return user ? (
        <div className="prime-user-account">
            <span id="emailUser">{userDisplay}</span>
            <br />
            {/* Okta handles our logoutRedirectUri, this should _not_ be an anchor tag! */}
            <Button id="logout" type="button" unstyled onClick={logout}>
                Log out
            </Button>
        </div>
    ) : (
        <USLinkButton href="/login" inverse={IS_PREVIEW}>
            Log in {IS_PREVIEW ? "via OktaPreview" : ""}
        </USLinkButton>
    );
};

export { SignInOrUser };
