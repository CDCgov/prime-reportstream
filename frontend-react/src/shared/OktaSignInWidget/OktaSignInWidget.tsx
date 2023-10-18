import React from "react";
import OktaSignIn, { WidgetOptions } from "@okta/okta-signin-widget";
import type { Tokens } from "@okta/okta-auth-js";

import "./OktaSignInWidget.scss";

export interface OktaSigninWidgetProps
    extends React.PropsWithChildren<React.HTMLAttributes<HTMLElement>> {
    config: WidgetOptions;
    onSuccess: (
        value: Tokens,
    ) => Tokens | PromiseLike<Tokens> | void | Promise<void>;
    onError: (reason: any) => PromiseLike<void> | void | Promise<void>;
}

/**
 * Handles showing the okta sign-in widget. The widget is
 * a framework-agnostic so it must be initialized and pointed
 * towards a DOM element so that it can render.
 */
const OktaSignInWidget = ({
    config,
    onSuccess,
    onError,
    children,
    id = "osw-container",
    ...props
}: OktaSigninWidgetProps) => {
    const widgetRef = React.useRef<HTMLElement | null>(null);
    React.useEffect(() => {
        if (!widgetRef.current) return void 0;

        const widget = new OktaSignIn(config);

        widget
            .showSignInToGetTokens({
                el: `#${widgetRef.current.id}`,
            })
            .then(onSuccess)
            .catch((e: any) => {
                onError?.(e);
                console.error("error logging in", e);
            });

        return () => widget.remove();
    }, [config, onError, onSuccess]);

    return (
        <article ref={widgetRef} {...props} id={id}>
            {children}
        </article>
    );
};
export default OktaSignInWidget;
