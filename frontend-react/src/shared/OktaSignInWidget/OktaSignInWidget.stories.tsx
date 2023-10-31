// AutoUpdateFileChromatic
import { oktaSignInConfig } from "../../oktaConfig";

import OktaSignInWidget from "./OktaSignInWidget";

export default {
    title: "components/OktaSignInWidget",
    component: OktaSignInWidget,
};

export const Default = {
    args: {
        config: { ...oktaSignInConfig },
        onSuccess: () => void 1,
        onError: () => void 1,
    },
};
