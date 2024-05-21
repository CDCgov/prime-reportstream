import type { Tokens } from "@okta/okta-auth-js";
import { useCallback, useMemo } from "react";
import { Helmet } from "react-helmet-async";
import { Navigate, useLocation, useSearchParams } from "react-router-dom";
import type { Location } from "react-router-dom";

import { USLink } from "../components/USLink";
import useSessionContext from "../contexts/Session/useSessionContext";
import OktaSignInWidget from "../shared/OktaSignInWidget/OktaSignInWidget";

export function Login() {
    const { oktaAuth, authState, config } = useSessionContext();
    const location: Location<{ originalUrl?: string } | undefined> =
        useLocation();
    const [searchParams] = useSearchParams();
    const finalConfig = useMemo(
        () => ({
            ...config.OKTA_WIDGET,
            otp: searchParams.get("otp"),
            token: searchParams.get("token"),
        }),
        [config.OKTA_WIDGET, searchParams],
    );

    const onSuccess = useCallback(
        (tokens: Tokens) => {
            void oktaAuth.handleLoginRedirect(
                tokens,
                location.state?.originalUrl ?? "/",
            );
            return tokens;
        },
        [location.state?.originalUrl, oktaAuth],
    );

    const onError = useCallback((_: any) => void 0, []);

    if (authState.isAuthenticated) {
        return <Navigate replace to={"/"} />;
    }

    return (
        <>
            <Helmet>
                <title>ReportStream login</title>
                <meta
                    property="og:image"
                    content="/assets/img/opengraph/reportstream.png"
                />
                <meta
                    property="og:image:alt"
                    content='"ReportStream" surrounded by an illustration of lines and boxes connected by colorful dots.'
                />
            </Helmet>
            <OktaSignInWidget
                className="margin-top-6 margin-x-auto width-mobile-lg padding-x-8"
                config={finalConfig}
                onSuccess={onSuccess}
                onError={onError}
            >
                <div className="margin-bottom-5 font-sans-3xs">
                    This is a U.S. government service. Your use indicates your
                    consent to monitoring, recording, and no expectation of
                    privacy. Misuse is subject to criminal and civil penalties.
                    By logging in, you are agreeing to our{" "}
                    <USLink href="/terms-of-service">terms of service.</USLink>
                </div>
            </OktaSignInWidget>
        </>
    );
}

export default Login;
