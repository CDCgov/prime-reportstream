import { Alert } from "@trussworks/react-uswds";
import React from "react";
import Helmet from "react-helmet";

import { ErrorName } from "../../components/RSErrorBoundary";

import { NotFound } from "./NotFound";
import { UnsupportedBrowser } from "./UnsupportedBrowser";

interface ErrorPageProps {
    code?: ErrorName;
    // TODO: Remove, let page handle messaging
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

const ErrorMessageWrapper = (
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
        [ErrorName.UNSUPPORTED_BROWSER]: <UnsupportedBrowser />,
        [ErrorName.NOT_FOUND]: <NotFound />,
        [ErrorName.UNKNOWN]: <GenericErrorPage />, // Only used for default to keep ts happy. expand?
    };

    const code = props.code || ErrorName.UNKNOWN;

    if (Object.keys(CODES_MAP).includes(code as string)) {
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
