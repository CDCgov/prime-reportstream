import { ApplicationInsights } from "@microsoft/applicationinsights-web";
import { ReactPlugin } from "@microsoft/applicationinsights-react-js";
import { createBrowserHistory } from "history";

// @ts-ignore
const browserHistory = createBrowserHistory({ basename: "" });
export const reactPlugin = new ReactPlugin();
const telemetryService = new ApplicationInsights({
    config: {
        connectionString: "YOUR_CONNECTION_STRING_GOES_HERE",
        extensions: [reactPlugin],
        extensionConfig: {
            [reactPlugin.identifier]: { history: browserHistory },
        },
    },
});

telemetryService.loadAppInsights();
