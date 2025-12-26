import type { Tokens } from "@okta/okta-auth-js";
import { OktaSignIn, WidgetOptions } from "@okta/okta-signin-widget";
import { HTMLAttributes, PropsWithChildren, useEffect, useRef } from "react";

import "@okta/okta-signin-widget/css/okta-sign-in.min.css";
import "./OktaSignInWidget.scss";

export interface OktaSigninWidgetProps extends Omit<PropsWithChildren<HTMLAttributes<HTMLElement>>, "onError"> {
    config: WidgetOptions;
    onSuccess: (value: Tokens) => Tokens | PromiseLike<Tokens> | void | Promise<void>;
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
    const widgetRef = useRef<HTMLElement | null>(null);
    useEffect(() => {
        if (!widgetRef.current) return void 0;

        const widget = new OktaSignIn(config);

        widget
            .showSignInToGetTokens({
                el: `#${widgetRef.current.id}`,
            })
            .then(onSuccess)
            .catch((e: any) => {
                void onError?.(e);
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
