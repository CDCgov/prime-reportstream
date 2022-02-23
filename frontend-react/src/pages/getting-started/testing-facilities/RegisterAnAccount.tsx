import { Helmet } from "react-helmet";

/* eslint-disable jsx-a11y/anchor-has-content */
export const RegisterAnAccount = () => {
    return (
        <>
            <Helmet>
                <title>
                    Register an account | Organizations and testing facilities |
                    Getting started | {process.env.REACT_APP_TITLE}
                </title>
            </Helmet>
            <section id="anchor-top">
                <h2 className="margin-top-0">
                    Register for a free ReportStream account
                </h2>
                <p className="usa-intro text-base">
                    This guide provides instructions for registering for a free
                    user account with ReportStream. Steps in this guide:
                </p>
                <ul>
                    <li>Contact ReportStream</li>
                    <li>Recieve activation email</li>
                    <li>Set up password</li>
                    <li>Set up security question</li>
                    <li>Set up two-factor authentication</li>
                    <li>Close window</li>
                    <li>Log in to ReportStream</li>
                </ul>
                <p>[info alert about SR and RS connection]</p>
            </section>

            <section>
                <ol className="usa-process-list">
                    <li className="usa-process-list__item">
                        <h4 className="usa-process-list__heading">
                            Contact ReportStream
                        </h4>
                        <p className="margin-top-05">
                            Send an email to reportstream@cdc.gov. Example
                            message template:
                        </p>
                        <blockquote>
                            to: reportstream@cdc.gov subject: Register a new
                            organization or testing facility account Hi, I'd
                            like to get a user account set up to use
                            reportstream. First name Last Name Job title
                            Organization Email Phone number
                        </blockquote>
                    </li>
                    <li className="usa-process-list__item">
                        <h4 className="usa-process-list__heading">
                            Recieve activation email
                        </h4>
                        <p>
                            Important: SimpleReport and ReportStream are both
                            part of the Pandemic-Ready Interoperability
                            Modernization Effort initiative provided by CDC and
                            USDS to strengthen data quality and information
                            technology systems in state and local health
                            departments.
                        </p>
                        <ul>
                            <li>
                                An email will arrive in your inbox from
                                support@simplereport.gov with the subject line
                                “Welcome to SimpleReport” (Important: It could
                                take up to 1 business day to receive the
                                activation email){" "}
                            </li>
                            <li>Open email</li>
                            <li>Click Activate your account</li>
                        </ul>
                        <p>[img]</p>
                    </li>
                    <li className="usa-process-list__item">
                        <h4 className="usa-process-list__heading">
                            Set up password
                        </h4>
                        <p>
                            Important: SimpleReport and ReportStream are both
                            part of the Pandemic-Ready Interoperability
                            Modernization Effort initiative provided by CDC and
                            USDS to strengthen data quality and information
                            technology systems in state and local health
                            departments.
                        </p>
                        <ul>
                            <li>
                                A prompt will appear for you to create a
                                password (must be at least 8 characters, include
                                an uppercase and lowercase letter, and a number.{" "}
                            </li>
                            <li>
                                Enter the password again in the Confirm password
                                field{" "}
                            </li>
                            <li>Click Continue </li>
                        </ul>
                        <p>[img]</p>
                    </li>
                    <li className="usa-process-list__item">
                        <h4 className="usa-process-list__heading">
                            Set up security question
                        </h4>
                        <p>
                            Important: SimpleReport and ReportStream are both
                            part of the Pandemic-Ready Interoperability
                            Modernization Effort initiative provided by CDC and
                            USDS to strengthen data quality and information
                            technology systems in state and local health
                            departments.
                        </p>
                        <ul>
                            <li>
                                A prompt will appear for you to select your
                                security question{" "}
                            </li>
                            <li>
                                Click the drop-down list to select your
                                preferred security question{" "}
                            </li>
                            <li>Enter your response in the Answer field </li>
                            <li>Click Continue </li>
                        </ul>
                        <p>[img]</p>
                    </li>
                    <li className="usa-process-list__item">
                        <h4 className="usa-process-list__heading">
                            Set up two-factor authentication [required]
                        </h4>
                        <p>
                            Important: SimpleReport and ReportStream are both
                            part of the Pandemic-Ready Interoperability
                            Modernization Effort initiative provided by CDC and
                            USDS to strengthen data quality and information
                            technology systems in state and local health
                            departments.
                        </p>
                        <ul>
                            <li>
                                A prompt will appear for you to set up a second
                                layer of security to protect your account{" "}
                            </li>
                            <li>Click the option you prefer </li>
                        </ul>
                        <p>[img]</p>
                    </li>
                    <li className="usa-process-list__item">
                        <h4 className="usa-process-list__heading">
                            Close window
                        </h4>
                        <p>
                            Important: SimpleReport and ReportStream are both
                            part of the Pandemic-Ready Interoperability
                            Modernization Effort initiative provided by CDC and
                            USDS to strengthen data quality and information
                            technology systems in state and local health
                            departments.
                        </p>
                        <ul>
                            <li>
                                A prompt will appear confirming your account set
                                up is complete (Important: It could take up to 1
                                business day before your account receives access
                                to upload CSV files.){" "}
                            </li>
                            <li>
                                Ignore the “Continue to SimpleReport” button{" "}
                            </li>
                            <li>Close out of the window </li>
                        </ul>
                        <p>[img]</p>
                    </li>
                    <li className="usa-process-list__item">
                        <h4 className="usa-process-list__heading">
                            Log in to ReportStream
                        </h4>

                        <ul>
                            <li>Go to https://reportstream.cdc.gov/login </li>
                            <li>Enter your username and password </li>
                            <li>
                                Contact reportstream@cdc.gov if you run into
                                login issues{" "}
                            </li>
                        </ul>
                        <p>[img]</p>
                    </li>
                </ol>
            </section>
        </>
    );
};
