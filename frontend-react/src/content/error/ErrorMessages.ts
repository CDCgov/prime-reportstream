import { ErrorDisplayConfig } from "../../pages/error/ErrorPage";

/** This file houses error display configs for when errors are thrown inside ReportStream
 *
 *  A custom error type will provide an ErrorDisplayConfig, and the ErrorDisplay component
 *  parses this to hydrate either a page-styled template OR a banner-styled template.
 *  NON-RS errors will _NOT_ include this functionality, so the default template, <GenericError>,
 *  is set up to handle non-RS errors by displaying fallbacks: GENERIC_ERROR_STRING for a message
 *  UI and GENERIC_ERROR_PAGE_CONFIG for a page UI. */

/** Default message for an error  */
export const GENERIC_ERROR_STRING: ErrorDisplayConfig =
    "Our apologies, there was an error loading this content.";
/** Default content for an error page */
export const GENERIC_ERROR_PAGE_CONFIG: ErrorDisplayConfig = {
    header: "An error has occurred",
    paragraph: `The application has encountered an unknown error. 
    It doesn't appear to have affected your data, but our technical staff 
    have been automatically notified and will be looking into this with the utmost 
    urgency.`,
};
