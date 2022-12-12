import DOMPurify from "dompurify";

import site from "../../../content/site.json";
import { BasicHelmet } from "../../../components/header/BasicHelmet";
import { ResourcesDirectories } from "../../../content/resources";

/* eslint-disable jsx-a11y/anchor-has-content */
export const GettingStartedPhd = () => {
    return (
        <>
            <BasicHelmet
                pageTitle={`${ResourcesDirectories.GETTING_STARTED_PHD} | Resources`}
            />
            <h1 id="anchor-top">{ResourcesDirectories.GETTING_STARTED_PHD}</h1>
            <h2>
                A step-by-step process for connecting your lab or facility to
                ReportStream
            </h2>
            <hr />
            <h3>Overview</h3>
            <p>
                Setting up an ELR connection to ReportStream will allow you to
                automatically report disease data to the appropriate
                jurisdiction(s) through an API. If you want to submit data
                manually, then you'll need to create an account with
                SimpleReport (linked) instead. Follow the steps outlined below
                to get started with an ELR connection.
            </p>
            <p>There are three basic steps to the ELR connection process.</p>
            <section>
                <ol className="usa-process-list">
                    <li className="usa-process-list__item">
                        <h4 className="usa-process-list__heading">
                            Schedule kick-off:
                        </h4>
                        <p className="margin-top-05">
                            Let us know you're interested by filling out and
                            submitting{" "}
                            <a
                                href={DOMPurify.sanitize(
                                    site.forms.intakeElr.url
                                )}
                                className="usa-link"
                            >
                                this form
                            </a>
                            . We'll be in touch within a week to schedule a
                            kick-off meeting and start the integration process,
                            which officially begins once you create and email us
                            a sample file with dummy test data (no PII).
                        </p>
                    </li>
                    <li className="usa-process-list__item">
                        <h4 className="usa-process-list__heading">
                            Connect and test:
                        </h4>
                        <p>
                            Once we've approved your data model (using your
                            sample file) we'll onboard you to ReportStream's
                            staging environment. You'll then be able to post
                            test submissions (again, no PII) as well as test
                            your code using ReportStream's staging API. We'll
                            review your test data and help you correct any
                            errors.
                        </p>
                    </li>
                    <li className="usa-process-list__item">
                        <h4 className="usa-process-list__heading">
                            Launch and roll out:
                        </h4>
                        <p>
                            Once all errors in data and/or code are addressed,
                            we'll enable you in full production mode. Moving
                            forward, you'll be submitting real data to the
                            correct public health jurisdictions.
                        </p>
                    </li>
                </ol>
                <hr />
                <h3 id="connecting-to-testing-facilities">
                    Reporting to public health entities
                </h3>
                <p>
                    New testing facilities join ReportStream on a regular basis
                    through our partner SimpleReport or through other public
                    health data aggregators and apps. After they join, their
                    data will be sent automatically to the correct public health
                    department(s) in their jurisdiction who are part of
                    ReportStream. Data is accessible via our web application or
                    the ELR connection.
                </p>
            </section>
            <section>
                <h3>Get started with ReportStream</h3>
                <p>
                    Ready to bring ReportStream to your jurisdiction, or just
                    have more questions? Fill out{" "}
                    <a
                        href={DOMPurify.sanitize(site.forms.intakeElr.url)}
                        className="usa-link"
                    >
                        the form
                    </a>{" "}
                    and we'll be in touch!
                </p>
            </section>
        </>
    );
};
