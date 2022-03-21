import { ReactElement } from "react";
import { useOktaAuth } from "@okta/okta-react";

import useSenderMode from "../hooks/UseSenderMode";

const isNotActive = (val: string): boolean => {
    return val === "testing" || val === "inactive";
};

const SenderModeBanner = (): ReactElement | null => {
    const auth = useOktaAuth();
    const { status } = useSenderMode();

    if (auth.authState?.isAuthenticated && isNotActive(status)) {
        return <div>User is in testing mode</div>;
    }

    return null;
};

export default SenderModeBanner;
