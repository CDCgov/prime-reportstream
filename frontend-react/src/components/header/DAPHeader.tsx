import React from "react";
import { Helmet } from "react-helmet";
import { useLocation } from "react-router-dom";

const urlsForDAP = [
    "/login",
    "/getting-started/",
    "/how-it-works/",
    "/terms-of-service",
];

export const useDAP = (env: string | undefined) => {
    const location = useLocation();
    let currentPathname = location.pathname;

    return (
        env === "production" &&
        (currentPathname === "/" ||
            urlsForDAP.some((url) => currentPathname.startsWith(url)))
    );
};

export interface DAPHeaderProps {
    env: string | undefined;
}

export const DAPHeader = (params: DAPHeaderProps) => {
    if (!useDAP(params.env)) return <></>;

    return (
        <>
            <Helmet>
                <script
                    async
                    type="text/javascript"
                    src="https://dap.digitalgov.gov/Universal-Federated-Analytics-Min.js?agency=HHS&subagency=CDC"
                    id="_fed_an_ua_tag"
                ></script>
            </Helmet>
        </>
    );
};
