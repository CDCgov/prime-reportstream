import { Helmet } from "react-helmet-async";

import { appRoutes } from "../../AppRouter";
import { isAuthenticatedPath } from "../../utils/PermissionsUtils";

export interface DAPScriptProps {
    pathname?: string;
}

// We only want the DAP Script to appear on non-authenticated pages
// so running through the appRoutes array, we can see any routes that
// require an auth prop, which could be a boolean or a string, and
// if so, that means it's an authenticated route.

const DAPScript = ({ pathname }: DAPScriptProps) => {
    if (pathname && isAuthenticatedPath(pathname, appRoutes)) {
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
