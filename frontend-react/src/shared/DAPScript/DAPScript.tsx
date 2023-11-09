import { Helmet } from "react-helmet-async";

import { appRoutes } from "../../AppRouter";

export interface DAPScriptProps {
    env?: string;
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

    return !!(matchedRoute?.element as React.ReactElement).props?.auth;
};

export const DAPScript = ({
    env = "development",
    pathname,
}: DAPScriptProps) => {
    /*
        NOTE: we originally allowed all known public-facing pages at this point,
        i.e. login, TOS and all the Getting Started and How it Works pages
        but then found that these would not be triggered due this site being SPA (vs. static)
        i.e. only on true page loads- visiting the site initially or manually refreshing a page
        For now, we'll only track visits to the main homepage, and allow App Insights to track
        more detailed analytics.
     */
    if (env !== "production" || (pathname && isAuthenticatedPath(pathname))) {
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
