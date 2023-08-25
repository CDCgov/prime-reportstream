import { useEffect, useRef } from "react";
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

    return <div id="okta-wrapper" ref={widgetRef as any} {...props} />;
};
export default OktaSignInWidget;
