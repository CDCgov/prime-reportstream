import Helmet from "react-helmet";
import React from "react";
import { Alert } from "@trussworks/react-uswds";

export const GenericMessage = () => {
    return (
        <Alert type="error">
            Our apologies, there was an error loading this content.
        </Alert>
    );
};

export const GenericPage = () => {
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
