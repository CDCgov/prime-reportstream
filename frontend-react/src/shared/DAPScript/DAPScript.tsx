import { Helmet } from "react-helmet-async";

export interface DAPScriptProps {
    env?: string;
    pathname?: string;
}

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
    if (env !== "production" || pathname !== "/") {
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
