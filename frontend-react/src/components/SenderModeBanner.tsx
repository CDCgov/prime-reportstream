import { ReactElement } from "react";
import { useOktaAuth } from "@okta/okta-react";

const SenderModeBanner = (): ReactElement | null => {
    const auth = useOktaAuth();

    if (auth.authState?.isAuthenticated) {
        return <div>Test</div>;
    }

    return null;
};

export default SenderModeBanner;
