// @ts-nocheck // TODO: fix types in this file
import React from 'react'
import Helmet from 'react-helmet'

import { NotFound } from "./NotFound";
import { UnsupportedBrowser } from "./UnsupportedBrowser";

interface ErrorPageProps {
    code?: string;
    error?: string;
    errorInfo?: React.ErrorInfo;
}

/* INFO
   For consistency, when passing the code prop, please use these values
   e.g. <ErrorPage code={CODES.NOT_FOUND_404} />
 */
export enum CODES {
    UNSUPPORTED_BROWSER = "unsupported-browser",
    NOT_FOUND_404 = "not-found",
}

function ErrorPageWrapper({ children }: JSX.Element) {
    return (
        <div className="usa-section padding-top-6">
            <div className="grid-container">
                <div className="grid-row grid-gap">
                    {children}
                </div>
            </div>
        </div>
    )
}

function GenericErrorContent(): JSX.Element {
    return (
        <>
            <Helmet>
                <title>Error</title>
            </Helmet>
            <div className="usa-prose">
                <h1>An error has occurred</h1>
                <p>
                    The application has encountered an unknown error.
                    It doesn't appear to have affected your data, but our technical staff have been automatically
                    notified and will be looking into this with the utmost urgency.
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
    )
}

export function ErrorPage(props: ErrorPageProps) {
    const codes = {
        "not-found": <NotFound />,
        "unsupported-browser": <UnsupportedBrowser />,
    };
    const content = codes[props.code];

    if (content) {
        return (
            <ErrorPageWrapper>
                {content}
            </ErrorPageWrapper>
        );
    }
    if (props.error !== undefined && props.error !== undefined) {
        return (
            <ErrorPageWrapper>
                {/* <GenericErrorContent error={props.error.error} errorInfo={props.error.errorInfo} /> */}
                <GenericErrorContent />
            </ErrorPageWrapper>
        )
    }
    return (
        <ErrorPageWrapper>
            <GenericErrorContent />
        </ErrorPageWrapper>
    )
}
