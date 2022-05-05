import React from "react";
import { Helmet } from "react-helmet";
import { useLocation } from "react-router-dom";

const urlsForDAP = [
    "/login",
    "/getting-started/",
    "/how-it-works/",
    "/terms-of-service",
];

const useDAP = () => {
    const location = useLocation();
    let currentPathname = location.pathname;

    return process.env.REACT_APP_ENV === "production" &&
        (currentPathname === "/" ||
            urlsForDAP.some((url) => currentPathname.startsWith(url)));
};

export const DAPHeader = () => {
    if (!useDAP()) return <></>;

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
