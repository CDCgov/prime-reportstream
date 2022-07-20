import { Helmet } from "react-helmet";
import { Link } from "react-router-dom";

export const Faq = () => {
    return (
        <>
            <Helmet>
                <title>FAQ | Support | {process.env.REACT_APP_TITLE}</title>
            </Helmet>
            <h1>Frequently asked questions</h1>
            <h2>Answers to common questions about ReportStream</h2>
            <hr />
            <h3>How much does ReportStream cost?</h3>
            <p>
                ReportStream was developed by the CDC for COVID-19 test data
                reporting and is 100% free.
            </p>
            <hr />
            <h3>Where can you use ReportStream?</h3>
            <p>
                ReportStream is currently live or getting set up in
                jurisdictions across the United States. Take a look at the
                complete{" "}
                <a className="usa-link" href="/product/where-were-live">
                    list of ReportStream partners
                </a>
                .
            </p>
            <hr />
            <h3>Which browser is recommended for using ReportStream?</h3>
            <p>
                Our application works best on a modern desktop browser (ex:{" "}
                <a
                    href="https://www.google.com/chrome/"
                    target="_blank"
                    rel="noreferrer"
                    className="usa-link"
                >
                    Chrome
                </a>
                ,{" "}
                <a
                    href="https://www.mozilla.org/en-US/firefox/new/"
                    target="_blank"
                    rel="noreferrer"
                    className="usa-link"
                >
                    Firefox
                </a>
                ,{" "}
                <a
                    href="https://www.apple.com/safari/"
                    target="_blank"
                    rel="noreferrer"
                    className="usa-link"
                >
                    Safari
                </a>
                ,{" "}
                <a
                    href="https://www.microsoft.com/en-us/edge"
                    target="_blank"
                    rel="noreferrer"
                    className="usa-link"
                >
                    Edge
                </a>
                ). ReportStream doesn't support Internet Explorer 11 or below.
            </p>
            <hr />
            <h3>
                I just activated my ReportStream account, why can't I log in?
            </h3>
            <p>
                ReportStream, as part of the{" "}
                <a
                    className="usa-link"
                    href="https://www.cdc.gov/surveillance/projects/pandemic-ready-it-systems.html"
                    target="_blank"
                    rel="noreferrer noopener"
                >
                    Pandemic-Ready Interoperability Modernization Effort (PRIME)
                </a>
                , shares some resources with other projects operated by the CDC.
            </p>
            <p>
                Some aspects of the registration process may contain references
                to{" "}
                <a className="usa-link" href="https://simplereport.gov">
                    SimpleReport
                </a>
                , a PRIME project that ReportStream closely collaborates with.
                To access your user account, be sure to log in at{" "}
                <Link to="/login" key="login" className="usa-link">
                    reportstream.cdc.gov/login
                </Link>
                .
            </p>
            <p>
                For any other issues logging in,{" "}
                <a className="usa-link" href="/support/contact">
                    contact us
                </a>
                .
            </p>
            <hr />
            <h3>How do I reset my password?</h3>
            <p>
                If you forgot your password, follow the instructions under the
                "Need help signing in?" link on the login page at{" "}
                <Link to="/login" key="login" className="usa-link">
                    reportstream.cdc.gov/login
                </Link>
                .
            </p>
            <p>
                If you want to update your password, log out of the application
                and follow the instructions under the "Need help signing in?"
                link on the login page at{" "}
                <Link to="/login" key="login" className="usa-link">
                    reportstream.cdc.gov/login
                </Link>
                .
            </p>
            resourcesetting errors from ReportStream?
            <p>
                We've put together{" "}
                <a
                    className="usa-link"
                    href="/getting-started/testing-facilities/csv-upload-guide"
                >
                    detailed instructions
                </a>{" "}
                outlining how to prepare a CSV file for submission. We use a{" "}
                <a
                    className="usa-link"
                    href="/getting-started/testing-facilities/csv-schema"
                >
                    standard schema
                </a>{" "}
                that will be accepted by state, tribal, local, or territorial
                (STLT) health departments partnered with ReportStream. The most
                common errors are outlined below:
            </p>
            <ul>
                <li>
                    <strong>Incorrect values</strong>
                    <p>
                        Elements of our standard CSV schema may require specific
                        values or formatting. Common values you may be
                        experiencing errors with include:{" "}
                        <a
                            className="usa-link"
                            href="/getting-started/testing-facilities/csv-schema#doc-equipment_model_name"
                        >
                            equipment model name
                        </a>
                        ,{" "}
                        <a
                            className="usa-link"
                            href="/getting-started/testing-facilities/csv-schema#doc-test_performed_code"
                        >
                            test performed code
                        </a>
                        ,{" "}
                        <a
                            className="usa-link"
                            href="/getting-started/testing-facilities/csv-schema#doc-test_result"
                        >
                            test result
                        </a>
                        ,{" "}
                        <a
                            className="usa-link"
                            href="/getting-started/testing-facilities/csv-schema#doc-patient_race"
                        >
                            patient race
                        </a>
                        , and{" "}
                        <a
                            className="usa-link"
                            href="/getting-started/testing-facilities/csv-schema#doc-patient_ethnicity"
                        >
                            patient ethnicity
                        </a>
                        .
                    </p>
                </li>
                <li>
                    <strong>Blank values</strong>
                    <p>
                        Our{" "}
                        <a className="usa-link" href="/resources/csv-schema">
                            standard schema
                        </a>{" "}
                        requires certain fields to be filled out. Required
                        fields containing blank values can prevent your file
                        from being uploaded. For a list of required fields, read
                        our{" "}
                        <a className="usa-link" href="/resources/csv-schema">
                            CSV schema documentation
                        </a>
                        .
                    </p>
                </li>
            </ul>
            <hr />
            <h3>
                Can I sort my CSV columns in any order when I submit the file to
                ReportStream?
            </h3>
            <p>
                Yes, you can organize your CSV columns in any order within your
                file. ReportStream looks for data values beneath the column
                header regardless of where it's located in the file.
            </p>
            <hr />
            <h3>
                What's the difference between ReportStream and SimpleReport?
            </h3>
            <p>
                ReportStream and{" "}
                <a
                    className="usa-link"
                    href="https://simplereport.gov"
                    target="_blank"
                    rel="noreferrer"
                >
                    SimpleReport
                </a>{" "}
                are sister projects under the{" "}
                <a
                    className="usa-link"
                    href="https://www.cdc.gov/surveillance/projects/pandemic-ready-it-systems.html"
                >
                    Pandemic-Ready Interoperability Modernization Effort (PRIME)
                </a>
                , operated by the CDC.
            </p>
            <p>
                ReportStream is an open source, cloud based platform that
                aggregates COVID-19 test results from healthcare clinics,
                hospitals, laboratories, and other organizations, and delivers
                them to public health departments.
            </p>
            <p>
                SimpleReport is a cloud-based application that helps
                organizations to collect and submit COVID-19 test results to
                public health. Data submitted with SimpleReport is routed
                through ReportStream and delivered to public health departments.
            </p>
        </>
    );
};
