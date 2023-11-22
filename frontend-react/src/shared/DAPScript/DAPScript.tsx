import { Helmet } from "react-helmet-async";
import React from "react";

import { appRoutes } from "../../AppRouter";

export interface DAPScriptProps {
    pathname?: string;
}

// We only want the DAP Script to appear on non-authenticated pages
// so running through the appRoutes array, we can see any routes that
// require an auth prop, which could be a boolean or a string, and
// if so, that means it's an authenticated route.
const isAuthenticatedPath = (pathname: string) => {
    const basePath = pathname.split("/")[1];

    const matchedRoute = appRoutes[0].children?.find((route) => {
        return route.path?.includes(basePath);
    });

    if (!matchedRoute || !React.isValidElement(matchedRoute.element)) {
        return false;
    }

    return !!matchedRoute.element.props?.auth;
};

export const DAPScript = ({ pathname }: DAPScriptProps) => {
    if (pathname && isAuthenticatedPath(pathname)) {
        return null;
    }

    return (
        <Helmet>
            <script
                async
                type="text/javascript"
                src="https://dap.digitalgov.gov/Universal-Federated-Analytics-Min.js?agency=HHS&subagency=CDC"
                id="_fed_an_ua_tag"
            ></script>
        </Helmet>
    );
};

export default DAPScript;
