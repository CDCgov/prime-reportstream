import React, { useMemo } from "react";

import { ErrorName } from "../../components/RSErrorBoundary";

import { GenericMessage, GenericPage } from "./content-elements/GenericPage";

interface ErrorPageProps {
    code?: ErrorName;
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

export const ErrorPage = ({
    code,
    errorInfo,
    type,
}: React.PropsWithChildren<ErrorPageProps>) => {
    /** Easy for ternary checks in the errorContent memo hook */
    const asPage = useMemo(() => type === "page", [type]);
    /** Handles mapping to the right page or message content */
    const errorContent = useMemo(() => {
        switch (code) {
            default:
                return asPage ? <GenericPage /> : <GenericMessage />;
        }
    }, [asPage, code]);

    return asPage ? (
        <ErrorPageWrapper>{errorContent}</ErrorPageWrapper>
    ) : (
        <ErrorMessageWrapper>{errorContent}</ErrorMessageWrapper>
    );
};

ErrorPage.defaultProps = {
    type: "message",
};
