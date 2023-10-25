import React, {
    createContext,
    useCallback,
    useContext,
    useEffect,
    useMemo,
    useState,
} from "react";
import {
    ReactPlugin,
    AppInsightsContext as AppInsightsContextOrig,
} from "@microsoft/applicationinsights-react-js";
import { ApplicationInsights } from "@microsoft/applicationinsights-web";

import type { MembershipSettings } from "../hooks/UseOktaMemberships";

export enum EventName {
    TABLE_FILTER = "Table Filter",
    SESSION_DURATION = "Session Duration",
    TABLE_PAGINATION = "Table Pagination",
    FILE_VALIDATOR = "File Validator",
}

export interface ReactPluginWithSDK extends ReactPlugin {
    sdk: ApplicationInsights;
}

export interface AppInsightsCustomProperties {
    activeMembership?: MembershipSettings;
    isUserAgentOutdated?: boolean;
}

export type AppInsightsSetCustomPropertyFn = <
    T extends keyof AppInsightsCustomProperties,
>(
    key: T,
    value: AppInsightsCustomProperties[T],
) => void;

export interface AppInsightsCtx {
    appInsights?: ReactPluginWithSDK;
    telemetryCustomProperties: AppInsightsCustomProperties;
    setTelemetryCustomProperty: AppInsightsSetCustomPropertyFn;
    fetchHeaders: Record<string, string | undefined | number>;
}

export const AppInsightsContext = createContext<AppInsightsCtx>({} as any);

export interface AppInsightsContextProviderProps
    extends React.PropsWithChildren {
    value?: ApplicationInsights;
}

/**
 * Replacement for AppInsights-React's context provider. Handles creating a proxy
 * that contains a reference to that instance, and creating our custom AppInsights
 * context.
 */
export function AppInsightsContextProvider({
    children,
    value: appInsights,
}: AppInsightsContextProviderProps) {
    const reactPlugin = useMemo(() => {
        if (!appInsights) return undefined;
        const reactPlugin = appInsights.getPlugin("ReactPlugin");
        /**
         * Proxy of AppInsight ReactPlugin that provides access to root sdk object
         * via `sdk` property.
         */
        const reactPluginProxy = new Proxy(reactPlugin.plugin, {
            get(target, p, receiver) {
                if (p === "sdk") {
                    return appInsights;
                }
                return Reflect.get(target, p, receiver);
            },
        }) as ReactPluginWithSDK;
        return reactPluginProxy;
    }, [appInsights]);

    const [telemetryCustomProperties, setTelemetryCustomProperties] =
        useState<AppInsightsCustomProperties>({});
    const setTelemetryCustomProperty =
        useCallback<AppInsightsSetCustomPropertyFn>(
            (key, value) =>
                setTelemetryCustomProperties((p) => {
                    // Freely use setter without worrying about causing unneeded rerenders
                    if (p[key] === value) return p;
                    return { ...p, [key]: value };
                }),
            [],
        );

    const ctx = useMemo<AppInsightsCtx>(
        () => ({
            appInsights: reactPlugin,
            telemetryCustomProperties,
            setTelemetryCustomProperty,
            fetchHeaders: reactPlugin
                ? {
                      "x-ms-session-id":
                          reactPlugin.sdk.context.getSessionId() ?? "",
                  }
                : {},
        }),
        [reactPlugin, telemetryCustomProperties, setTelemetryCustomProperty],
    );

    useEffect(() => {
        // using dependency as using the `properties` method is preferred way
        const handler = appInsights?.addTelemetryInitializer((item) => {
            item.data = {
                ...item.data,
                ...telemetryCustomProperties,
            };
        });

        return () => handler?.remove();
    }, [appInsights, telemetryCustomProperties]);

    if (!reactPlugin) {
        return (
            <AppInsightsContext.Provider value={ctx}>
                {children}
            </AppInsightsContext.Provider>
        );
    }
    return (
        <AppInsightsContextOrig.Provider value={reactPlugin}>
            <AppInsightsContext.Provider value={ctx}>
                {children}
            </AppInsightsContext.Provider>
        </AppInsightsContextOrig.Provider>
    );
}

export const useAppInsightsContext = () => useContext(AppInsightsContext);

export default AppInsightsContextProvider;
