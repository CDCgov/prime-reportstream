import { Alert } from "@trussworks/react-uswds";
import React from "react";
import Helmet from "react-helmet";

import { NotFound } from "./NotFound";
import { UnsupportedBrowser } from "./UnsupportedBrowser";

/* INFO
   For consistency, when passing the code prop, please use these values
   e.g. <ErrorPage code={CODES.NOT_FOUND_404} />
 */
export enum CODES {
    UNSUPPORTED_BROWSER = "unsupported-browser",
    NOT_FOUND_404 = "not-found",
    UNKNOWN = "unknown-error",
}

interface ErrorPageProps {
    code?: CODES;
    error?: string;
    errorInfo?: React.ErrorInfo;
    type?: "page" | "message";
}

const ErrorPageWrapper = (props: React.PropsWithChildren<ErrorPageProps>) => {
    return (
        <div className="usa-section padding-top-6">
            <div className="grid-container">
                <div className="grid-row grid-gap">{props.children}</div>
            </div>
        </div>
    );
};

export const ErrorMessageWrapper = (
    props: React.PropsWithChildren<ErrorPageProps>
) => {
    return <div className="grid-container">{props.children}</div>;
};

const GenericErrorMessage = () => {
    return (
        <Alert type="error">
            Our apologies, there was an error loading this content.
        </Alert>
    );
};

const GenericErrorPage = () => {
    return (
        <>
            <Helmet>
                <title>Error | {process.env.REACT_APP_TITLE}</title>
            </Helmet>
            <div className="usa-prose">
                <h1>An error has occurred</h1>
                <p>
                    The application has encountered an unknown error. It doesn't
                    appear to have affected your data, but our technical staff
                    have been automatically notified and will be looking into
                    this with the utmost urgency.
                </p>
                <div className="margin-y-5">
                    <ul className="usa-button-group">
                        <li className="usa-button-group__item">
                            <a href="./" className="usa-button">
                                Visit homepage
                            </a>
                        </li>
                    </ul>
                </div>
            </div>
        </>
    );
};

export const ErrorPage = (props: React.PropsWithChildren<ErrorPageProps>) => {
    const CODES_MAP = {
        [CODES.UNSUPPORTED_BROWSER]: <UnsupportedBrowser />,
        [CODES.NOT_FOUND_404]: <NotFound />,
        [CODES.UNKNOWN]: <GenericErrorPage />, // Only used for default to keep ts happy. expand?
    };

    const code = props.code || CODES.UNKNOWN;

    if (Object.keys(CODES_MAP).includes(code)) {
        return <ErrorPageWrapper>{CODES_MAP[code] || ""}</ErrorPageWrapper>;
    }
    if (props.type === "message") {
        return (
            <ErrorMessageWrapper>
                <GenericErrorMessage />
            </ErrorMessageWrapper>
        );
    } else {
        return (
            <ErrorPageWrapper>
                <GenericErrorPage />
            </ErrorPageWrapper>
        );
    }
};

ErrorPage.defaultProps = {
    type: "message",
};
