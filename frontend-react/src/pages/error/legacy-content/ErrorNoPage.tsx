import { Helmet } from "react-helmet";
import DOMPurify from "dompurify";
import React from "react";

import site from "../../../content/site.json";

export const ErrorNoPage = () => {
    return (
        <>
            <Helmet>
                <title>Page Not Found | {process.env.REACT_APP_TITLE}</title>
            </Helmet>
            <div
                data-testid={"error-page-wrapper"}
                className="usa-section padding-top-6"
            >
                <div className="grid-container">
                    <div className="grid-row grid-gap">
                        <div className="usa-prose">
                            <h1>Page not found</h1>
                            <p className="usa-intro">
                                We’re sorry, we can’t find the page you're
                                looking for. It might have been removed, changed
                                names, or is otherwise unavailable.
                            </p>
                            <p>
                                If you typed the URL directly, check your
                                spelling and capitalization. Our URLs look like
                                this:{" "}
                                <strong>
                                    reportstream.cdc.gov/example-one
                                </strong>
                                .
                            </p>
                            <p>
                                Visit our homepage or contact us at{" "}
                                {site.orgs.RS.email} and we’ll point you in the
                                right direction.{" "}
                            </p>
                            <div className="margin-y-5">
                                <ul className="usa-button-group">
                                    <li className="usa-button-group__item">
                                        <a href="./" className="usa-button">
                                            Visit homepage
                                        </a>
                                    </li>
                                    <li className="usa-button-group__item">
                                        <a
                                            className="usa-button usa-button--outline"
                                            href={
                                                "mailto:" +
                                                DOMPurify.sanitize(
                                                    site.orgs.RS.email
                                                )
                                            }
                                        >
                                            Contact us
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
