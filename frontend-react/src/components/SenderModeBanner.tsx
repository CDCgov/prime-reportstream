import { ReactElement, useContext } from "react";
import { useOktaAuth } from "@okta/okta-react";

import SenderProvider, { SenderContext } from "../contexts/SenderContext";

const BannerContent = (): ReactElement | null => {
    const auth = useOktaAuth();
    const senderContext = useContext(SenderContext);

    if (auth.authState?.isAuthenticated) {
        return <div>Test</div>;
    }

    return null;
};

const SenderModeBanner = () => {
    return (
        <SenderProvider>
            <BannerContent />
        </SenderProvider>
    );
};

export default SenderModeBanner;
