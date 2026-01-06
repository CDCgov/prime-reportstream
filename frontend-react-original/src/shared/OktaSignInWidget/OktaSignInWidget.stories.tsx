// AutoUpdateFileChromatic
import OktaSignInWidget from "./OktaSignInWidget";
import config from "../../config";

export default {
    title: "components/OktaSignInWidget",
    component: OktaSignInWidget,
};

export const Default = {
    args: {
        config: { ...config.OKTA_WIDGET },
        onSuccess: () => void 1,
        onError: () => void 1,
    },
};
