export interface ErrorPageContentConfig {
    header: string;
    paragraph: string;
}
export type ErrorMessage = ErrorPageContentConfig | string;
/** Default message for an error  */
export const GENERIC_ERROR_STRING: ErrorMessage =
    "Our apologies, there was an error loading this content.";
/** Default content for an error page */
export const GENERIC_ERROR_PAGE_CONFIG: ErrorMessage = {
    header: "An error has occurred",
    paragraph: `The application has encountered an unknown error. 
    It doesn't appear to have affected your data, but our technical staff 
    have been automatically notified and will be looking into this with the utmost 
    urgency.`,
};
