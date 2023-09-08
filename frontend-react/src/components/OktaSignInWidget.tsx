import { useEffect } from "react";
import OktaSignIn, { WidgetOptions } from "@okta/okta-signin-widget";
import "@okta/okta-signin-widget/css/okta-sign-in.min.css";
import { Tokens } from "@okta/okta-auth-js";

export interface OktaSigninWidgetProps
    extends React.PropsWithChildren<React.HTMLAttributes<HTMLElement>> {
    config: WidgetOptions;
    onSuccess: (value: Tokens) => Tokens | PromiseLike<Tokens>;
    onError: (reason: any) => PromiseLike<void> | void;
}

const OktaSignInWidget = ({
    config,
    onSuccess,
    onError,
    ...props
}: OktaSigninWidgetProps) => {
    useEffect(() => {
        const widget = new OktaSignIn(config);

        widget
            .showSignInToGetTokens({
                el: "#okta-wrapper",
            })
            .then(onSuccess)
            .catch(onError);

        return () => widget.remove();
    }, [config, onSuccess, onError]);

    return <section id="okta-wrapper" {...props} />;
};
export default OktaSignInWidget;
