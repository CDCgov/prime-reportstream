import { Button } from "@trussworks/react-uswds";
import { Helmet } from "react-helmet-async";
import { useNavigate } from "react-router-dom";

import { pageNotFound } from "../../../content/error/ErrorMessages";
import site from "../../../content/site.json";

export const ErrorNoPage = () => {
    const navigate = useNavigate();
    return (
        <>
            <Helmet>
                <title>
                    {pageNotFound} | {import.meta.env.VITE_TITLE}
                </title>
            </Helmet>
            <div data-testid={"error-page-wrapper"} className="usa-section padding-top-6">
                <div className="grid-container">
                    <div className="grid-row grid-gap">
                        <div className="usa-prose">
                            <h1>{pageNotFound}</h1>
                            <p className="usa-intro">
                                We’re sorry, we can’t find the page you&apos;re looking for. It might have been removed,
                                changed names, or is otherwise unavailable.
                            </p>
                            <p>
                                If you typed the URL directly, check your spelling and capitalization. Our URLs look
                                like this: <strong>reportstream.cdc.gov/example-one</strong>.
                            </p>
                            <p>
                                Visit our homepage or contact us at {site.orgs.RS.email} and we’ll point you in the
                                right direction.{" "}
                            </p>
                            <div className="margin-y-5">
                                <ul className="usa-button-group">
                                    <li className="usa-button-group__item">
                                        <Button type="button" onClick={() => navigate("/")}>
                                            Visit homepage
                                        </Button>
                                    </li>
                                    <li className="usa-button-group__item">
                                        <Button
                                            type="button"
                                            outline
                                            onClick={() => window.open(`mailto:${site.orgs.RS.email}`)}
                                        >
                                            Contact us
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

export default ErrorNoPage;
