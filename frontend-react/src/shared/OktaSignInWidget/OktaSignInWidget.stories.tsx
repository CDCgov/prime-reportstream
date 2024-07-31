// AutoUpdateFileChromatic
import OktaSignInWidget from "./OktaSignInWidget";
import { oktaSignInConfig } from "../../oktaConfig";

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
