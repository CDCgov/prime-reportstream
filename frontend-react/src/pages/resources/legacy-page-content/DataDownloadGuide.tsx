import { useNavigate } from "react-router-dom";
import { Button } from "@trussworks/react-uswds";
import { Helmet } from "react-helmet-async";

import site from "../../../content/site.json";
import { ResourcesDirectories } from "../../../content/resources";
import { USExtLink, USLink } from "../../../components/USLink";

export const DataDownloadGuideIa = () => {
    const navigate = useNavigate();
    return (
        <>
            <Helmet>
                <title>{`${ResourcesDirectories.DOWNLOAD_GUIDE} | Resources`}</title>
            </Helmet>
            <h1 id="anchor-top">{ResourcesDirectories.DOWNLOAD_GUIDE}</h1>
            <h2>
                Instructions for public health departments to download data
                manually from the ReportStream application.
            </h2>

            <section>
                <div className="usa-alert usa-alert--info margin-bottom-6 measure-6">
                    <div className="usa-alert__body">
                        <h3 className="usa-alert__heading font-body-md margin-top-05">
                            Why download data manually from ReportStream?
                        </h3>
                        <p className="margin-top-3">
                            <strong>
                                Get started without an ELR connection
                            </strong>
                        </p>
                        <p>
                            Download data in standard CSV and HL7 formats
                            without setting up an ELR feed.
                        </p>
                        <p>
                            <strong>Access back-up data</strong>
                        </p>
                        <p>
                            Get your jurisdiction data manually in the case of a
                            temporary connection failure.
                        </p>
                        <p>
                            <strong>Secure and personalized </strong>
                        </p>
                        <p>
                            Set up secure, individual logins for each member of
                            your team.
                        </p>
                    </div>
                </div>
                <h1>Download data from ReportStream</h1>
                <ol className="usa-process-list">
                    <li className="usa-process-list__item">
                        <h3 className="usa-process-list__heading">
                            Create an account
                        </h3>
                        <p className="margin-top-05">
                            To gain access to the ReportStream application,
                            follow the steps outlined in the{" "}
                            <USLink href="/resources/account-registration-guide">
                                Account Registration Guide
                            </USLink>
                            .
                        </p>
                    </li>
                    <li className="usa-process-list__item">
                        <h3 className="usa-process-list__heading">
                            Log in to ReportStream
                        </h3>

                        <p>
                            Visit{" "}
                            <USExtLink href={site.orgs.RS.url}>
                                reportstream.cdc.gov
                            </USExtLink>{" "}
                            using a modern desktop web browser (ex: Chrome,
                            Firefox, Safari, Edge) and log in.
                        </p>
                        <p>
                            Please note: ReportStream does not support Internet
                            Explorer 11 or below.
                        </p>
                    </li>
                    <li className="usa-process-list__item">
                        <h3 className="usa-process-list__heading">
                            Access your jurisdiction's data
                        </h3>
                        <p>
                            Once logged in to ReportStream, visit{" "}
                            <USLink href="/daily-data" key="daily">
                                reportstream.cdc.gov/daily-data
                            </USLink>
                            .
                        </p>
                        <p>
                            You will be able to download the most recently
                            reported test result data, as well as up to 30 days
                            of previously reported data. After 30 days has
                            passed, the file will be permanently deleted and
                            will not be recoverable.
                        </p>
                        <p>
                            For this reason, please download data and ingest it
                            into your system as soon as possible.
                        </p>
                    </li>
                </ol>

                <h4>Support</h4>
                <p>
                    Do you have questions, problems, or bugs to report? Contact
                    us for help.
                </p>
                <Button
                    type="button"
                    outline
                    onClick={() => navigate("/support/contact")}
                >
                    Contact us
                </Button>
                <p className="margin-top-8">
                    <USLink href="#anchor-top">Back to top</USLink>
                </p>
            </section>
        </>
    );
};
