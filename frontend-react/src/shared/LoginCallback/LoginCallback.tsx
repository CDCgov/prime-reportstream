import type { OnAuthResumeFunction } from "@okta/okta-react/bundles/types/OktaContext";
import { ComponentType, FC, ReactElement, useEffect, useState } from "react";

import useSessionContext from "../../contexts/Session/useSessionContext";

const OktaError: FC<{ error: Error }> = ({ error }) => {
    if (error.name && error.message) {
        return (
            <p>
                {error.name}: {error.message}
            </p>
        );
    }
    return <p>Error: {error.toString()}</p>;
};

interface LoginCallbackProps {
    errorComponent?: ComponentType<{ error: Error }>;
    onAuthResume?: OnAuthResumeFunction;
    loadingElement?: ReactElement;
}

let handledRedirect = false;

/**
 * Copied from unreleased okta-react 6.8.0. Fixes double render on
 * React18. Remove and redirect all references to library's version
 * once published.
 * @see https://github.com/okta/okta-react/pull/266
 */
const LoginCallback: FC<LoginCallbackProps> = ({ errorComponent, loadingElement = null, onAuthResume }) => {
    const { oktaAuth, authState } = useSessionContext();
    const [callbackError, setCallbackError] = useState(null);

    const ErrorReporter = errorComponent ?? OktaError;
    useEffect(() => {
        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
        // @ts-ignore OKTA-464505: backward compatibility support for auth-js@5
        const isInteractionRequired =
            oktaAuth.idx.isInteractionRequired || (oktaAuth as any).isInteractionRequired?.bind(oktaAuth);
        if (onAuthResume && isInteractionRequired()) {
            onAuthResume();
            return;
        }
        // OKTA-635977: Prevents multiple calls of handleLoginRedirect() in React18 StrictMode
        // Top-level variable solution follows: https://react.dev/learn/you-might-not-need-an-effect#initializing-the-application
        if (!handledRedirect) {
            oktaAuth.handleLoginRedirect().catch((e) => {
                setCallbackError(e);
            });
            handledRedirect = true;
        }
    }, [oktaAuth, onAuthResume]);

    const authError = authState?.error;
    const displayError = callbackError ?? authError;
    if (displayError) {
        return <ErrorReporter error={displayError} />;
    }

    return loadingElement;
};

export default LoginCallback;
