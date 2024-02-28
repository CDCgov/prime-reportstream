/** Config to suit page-style templates */
export interface ParagraphWithTitle {
    header: string;
    paragraph: string;
}

/** A custom error type, such as {@link RSNetworkError}, will provide the new error boundary,
 *  {@link RSErrorBoundary}, with a display message. The type is checked at runtime in the boundary.
 *  If you only provide a string, it will render using our string-only interface, and if you provide a
 *  paragraph/header it'll render with a paragraph/titled interface. */
export type ErrorDisplayMessage = ParagraphWithTitle | string;

/** Default message for an error  */
export const GENERIC_ERROR_STRING =
    "Our apologies, there was an error loading this content.";
/** Default content for an error page */
export const GENERIC_ERROR_PAGE_CONFIG: ErrorDisplayMessage = {
    header: "An error has occurred",
    paragraph: `The application has encountered an unknown error. 
    It doesn't appear to have affected your data, but our technical staff 
    have been automatically notified and will be looking into this with the utmost 
    urgency.`,
};
