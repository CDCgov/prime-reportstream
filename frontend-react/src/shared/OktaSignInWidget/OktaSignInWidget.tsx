import React from "react";
import OktaSignIn, { WidgetOptions } from "@okta/okta-signin-widget";
import type { Tokens } from "@okta/okta-auth-js";

import "./OktaSignInWidget.scss";

async function showSignInToGetTokens(
    widget: OktaSignIn,
    el: string,
    onSuccess?: (value: Tokens) => void,
    onFailure?: (reason: any) => void,
): Promise<void> {
    try {
        const tokens = await widget.showSignInToGetTokens({
            el,
        });

        onSuccess?.(tokens);
    } catch (e: any) {
        onFailure?.(e);
        console.error("error logging in", e);
    }
}

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
    ...props
}: OktaSigninWidgetProps) => {
    const containerId = "okta-wrapper";
    const widget = React.useMemo(() => new OktaSignIn(config), [config]);
    const onRef = React.useCallback<React.RefCallback<HTMLElement>>(
        (el) => {
            // Render widget only once when we have an
            // element reference
            if (!el || el.querySelector("#okta-sign-in")) return;

            // not awaited as its side effects
            // are expected to be async so as not
            // to block rendering
            showSignInToGetTokens(
                widget,
                `#${containerId}`,
                onSuccess,
                onError,
            );
        },
        [widget, onSuccess, onError],
    );

    return (
        <article ref={onRef} id={containerId} {...props}>
            {children}
        </article>
    );
};
export default OktaSignInWidget;
