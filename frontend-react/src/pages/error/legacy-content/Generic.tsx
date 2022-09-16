import Helmet from "react-helmet";
import React from "react";
import { Alert } from "@trussworks/react-uswds";

import {
    GENERIC_ERROR_PAGE_CONFIG,
    GENERIC_ERROR_STRING,
} from "../../../content/error/ErrorMessages";
import { ErrorDisplayConfig, ErrorPageContentConfig } from "../ErrorPage";

export const GenericMessage = ({ message }: { message?: string }) => {
    return (
        <Alert type="error">{message ? message : GENERIC_ERROR_STRING}</Alert>
    );
};

export const GenericPage = ({
    config = GENERIC_ERROR_PAGE_CONFIG as ErrorPageContentConfig,
}: {
    config?: ErrorPageContentConfig;
}) => {
    return (
        <>
            <Helmet>
                <title>Error | {process.env.REACT_APP_TITLE}</title>
            </Helmet>
            <div
                data-testid={"error-page-wrapper"}
                className="usa-section padding-top-6"
            >
                <div className="grid-container">
                    <div className="grid-row grid-gap">
                        <div className="usa-prose">
                            <h1>{config.header}</h1>
                            <p>{config.paragraph}</p>
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
                    </div>
                </div>
            </div>
        </>
    );
};

export interface GenericErrorProps {
    displayAsPage?: boolean;
    displayConfig?: ErrorDisplayConfig;
}
export const GenericError = ({
    displayAsPage,
    displayConfig,
}: GenericErrorProps) => {
    if (!displayConfig) {
        // For back-compat with older uses
        // TODO: Remove when we stop using GenericError outside of RSErrorBoundary
        return displayAsPage ? <GenericPage /> : <GenericMessage />;
    } else {
        // For use with RSNetworkError
        // Error message/page configs are designed in `/src/content/error/ErrorMessages.ts`
        return typeof displayConfig === "string" ? (
            <GenericMessage message={displayConfig as string} />
        ) : (
            <GenericPage config={displayConfig as ErrorPageContentConfig} />
        );
    }
};
