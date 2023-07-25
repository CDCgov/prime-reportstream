import React from "react";
import { Alert, Button } from "@trussworks/react-uswds";
import { useNavigate } from "react-router-dom";
import { Helmet } from "react-helmet-async";

import {
    ErrorDisplayMessage,
    GENERIC_ERROR_PAGE_CONFIG,
    GENERIC_ERROR_STRING,
    ParagraphWithTitle,
} from "../../content/error/ErrorMessages";

export const StringErrorDisplay = ({ message }: { message?: string }) => {
    return (
        <Alert headingLevel="h4" type="error" role="alert">
            {message ? message : GENERIC_ERROR_STRING}
        </Alert>
    );
};

export const ParagraphErrorDisplay = ({
    config = GENERIC_ERROR_PAGE_CONFIG as ParagraphWithTitle,
}: {
    config?: ParagraphWithTitle;
}) => {
    const navigate = useNavigate();
    return (
        <>
            <Helmet>
                <title>Error</title>
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
                                        <Button
                                            type="button"
                                            onClick={() => navigate("/")}
                                        >
                                            Visit homepage
                                        </Button>
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
    displayConfig?: ErrorDisplayMessage;
}
export const BasicErrorDisplay = ({
    displayAsPage,
    displayConfig,
}: GenericErrorProps) => {
    if (!displayConfig) {
        // For back-compat with older uses
        // TODO: Remove when we stop using GenericError outside of RSErrorBoundary
        return displayAsPage ? (
            <ParagraphErrorDisplay />
        ) : (
            <StringErrorDisplay />
        );
    } else {
        // For use with RSNetworkError
        // Error message/page configs are designed in `/src/content/error/ErrorMessages.ts`
        return typeof displayConfig === "string" ? (
            <StringErrorDisplay message={displayConfig} />
        ) : (
            <ParagraphErrorDisplay config={displayConfig} />
        );
    }
};
