import { ReadonlyDeep } from "type-fest";

import { RSRouteObject } from "../../../utils/UsefulTypes";

export const reportStreamApiNav = {
    path: "reportstream-api",
    title: "ReportStream API",
    children: [
        { path: "#onboarding-overview", title: "Onboarding overview" },
        { path: "#about-our-api", title: "About our API" },
    ],
} as const satisfies ReadonlyDeep<RSRouteObject>;

export const gettingStartedNav = {
    path: "getting-started",
    title: "Getting started",
    children: [
        {
            path: "#format-and-validate-a-fake-data-file",
            title: "Format and validate a fake data file",
        },
        {
            path: "#set-up-authentication-and-test-your-api-connection",
            title: "Set up authentication and test your api connection",
        },
        {
            path: "#test-real-data-in-production",
            title: "Test real data in production",
        },
        {
            path: "#start-sending-data-in-production",
            title: "Start sending data in production",
        },
    ],
} as const satisfies ReadonlyDeep<RSRouteObject>;

export const documentationNav = {
    path: "documentation",
    title: "Documentation",
    children: [
        {
            path: "data-model",
            children: [
                {
                    path: "#common-errors",
                    title: "Common errors",
                },
                {
                    path: "#legend",
                    title: "Legend",
                },
                {
                    path: "#patient-data-elements",
                    title: "Patient data elements",
                },
                {
                    path: "#order-and-result-data-elements",
                    title: "Order and result data elements",
                },
                {
                    path: "#specimen-data-elements",
                    title: "Specimen data elements",
                },
                {
                    path: "#ordering-provider-data-elements",
                    title: "Ordering provider data elements",
                },
                {
                    path: "#testing-facility-data-elements",
                    title: "Testing facility data elements",
                },
                {
                    path: "#ask-on-entry",
                    title: "Ask-On-Entry (AOEs)",
                },
                {
                    path: "#report-and-ordering-facility-elements",
                    title: "Report and ordering facility elements",
                },
            ],
            title: "Data model",
        },
        {
            path: "responses-from-reportstream",
            children: [
                {
                    path: "#errors-and-warnings",
                    title: "Errors and warnings",
                },
                {
                    path: "#response-messages",
                    title: "Response messages",
                },
                {
                    path: "#json-error-responses",
                    title: "JSON error responses",
                },
            ],
            title: "Responses from ReportStream",
        },
        {
            path: "sample-payloads-and-output",
            title: "Sample payloads and output",
            children: [
                {
                    path: "#sample-csv-payload-and-output",
                    title: "Sample CSV payload and output",
                },
                {
                    path: "#sample-hl7-v2.5.1-payload-and-output",
                    title: "Sample HL7 v2.5.1 payload and output",
                },
                {
                    path: "#example-data-models",
                    title: "Example data models",
                },
            ],
        },
    ],
} as const satisfies ReadonlyDeep<RSRouteObject>;

const routes = {
    path: "api-programmers-guide",
    children: [reportStreamApiNav, gettingStartedNav, documentationNav],
} as const satisfies ReadonlyDeep<RSRouteObject>;

export default routes;
