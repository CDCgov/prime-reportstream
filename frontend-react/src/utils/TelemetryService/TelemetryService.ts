import { ReactPlugin as _ReactPlugin } from "@microsoft/applicationinsights-react-js";
import {
    ApplicationAnalytics,
    ApplicationInsights,
    IConfig,
    IConfiguration,
    PropertiesPlugin,
} from "@microsoft/applicationinsights-web";

export let appInsights: ApplicationInsights | undefined;

export const PropertiesPluginIdentifier = "AppInsightsPropertiesPlugin";

/**
 * Fixed typings and access to other needed plugins. Going through the
 * ApplicationInsights instance object itself would be faster but we're
 * trying to stick to the officially provided patterns (aka going through
 * the ReactPlugin). Also exposes a `customProperties` object bag for
 * outgoing telemetry.
 */
export class ReactPlugin extends _ReactPlugin {
    customProperties: Record<string, any> = {};

    initialize(...args: Parameters<InstanceType<typeof _ReactPlugin>["initialize"]>): void {
        super.initialize(...args);
        this.appInsights.addTelemetryInitializer((item) => {
            item.data = {
                ...item.data,
                ...this.customProperties,
            };
        });
    }

    get appInsights() {
        return super.getAppInsights() as unknown as ApplicationAnalytics;
    }
    get properties() {
        return this.core.getPlugin<PropertiesPlugin>(PropertiesPluginIdentifier).plugin;
    }
}

/**
 * Handles maintaining a singular app insights object.
 */
export function createTelemetryService(config: IConfiguration & IConfig) {
    if (appInsights) void appInsights.unload(false);
    const plugin = new ReactPlugin();
    // Create insights
    appInsights = new ApplicationInsights({
        config: {
            ...config,
            extensions: [plugin],
        },
    });

    // Initialize for use in ReportStream
    appInsights.loadAppInsights();

    return { appInsights, reactPlugin: plugin };
}
