import { useEffect, useRef } from "react";
import OktaSignIn from "@okta/okta-signin-widget";
import "@okta/okta-signin-widget/css/okta-sign-in.min.css";

const OktaSignInWidget = ({ config, onSuccess, onError }: any) => {
    const widgetRef = useRef();
    useEffect(() => {
        if (!widgetRef.current) return undefined;

        const widget = new OktaSignIn(config);

        widget
            .showSignInToGetTokens({
                el: widgetRef.current,
            })
            .then(onSuccess)
            .catch(onError);

        return () => widget.remove();
    }, [config, onSuccess, onError]);

    return <div ref={widgetRef as any} />;
};
export default OktaSignInWidget;
