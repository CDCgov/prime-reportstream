'use client'
 
import dynamic from 'next/dynamic'
import { lazy } from 'react'
import { RouterProvider } from "react-router";

import { createRouter } from "../../AppRouter";
import config from "../../config";
import AppInsightsContextProvider from "../../contexts/AppInsights";
import { OKTA_AUTH } from "../../oktaConfig";
import { aiConfig, createTelemetryService } from "../../TelemetryService";

import "../../global.scss";

const App = dynamic(() => import('../../App'), { ssr: false });
const appInsights = createTelemetryService(aiConfig);
const router = createRouter(
    // eslint-disable-next-line @typescript-eslint/require-await
    lazy(async () => {
        const MainLayout = lazy(() => import("../../layouts/Main/MainLayout"));
        return {
            default: () => (
                <App Layout={MainLayout} config={config} oktaAuth={OKTA_AUTH} />
            ),
        };
    }),
);
 
export function ClientOnly() {
  return <AppInsightsContextProvider value={appInsights}>
        <RouterProvider router={router} />
    </AppInsightsContextProvider>
}