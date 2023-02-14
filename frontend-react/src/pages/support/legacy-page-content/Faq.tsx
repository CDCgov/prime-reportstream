import { BasicHelmet } from "../../../components/header/BasicHelmet";
import { USExtLink, USLink } from "../../../components/USLink";

export const Faq = () => {
    return (
        <>
            <BasicHelmet pageTitle="FAQ | Support" />
            <h1>Frequently asked questions</h1>
            <h2>Answers to common questions about ReportStream</h2>
            <hr />
            <h3>How do I reset my password?</h3>
            <p>
                If you forgot your password, follow the instructions under the
                "Need help signing in?" link on the login page at{" "}
                <USLink href="/login" key="login">
                    reportstream.cdc.gov/login
                </USLink>
                .
            </p>
            <hr />
            <h3>How much does ReportStream cost?</h3>
            <p>
                ReportStream is 100% free. Development is supported by the CDC.
            </p>
            <hr />
            <h3>Where is ReportStream used?</h3>
            <p>
                ReportStream is currently live or getting set up in
                jurisdictions across the United States. Take a look at the
                complete{" "}
                <USLink href="/product/where-were-live">
                    list of ReportStream partners
                </USLink>
                .
            </p>
            <hr />
            <h3>Which browser is recommended for using ReportStream?</h3>
            <p>
                Our application works best on a modern desktop browser (ex:{" "}
                <USExtLink href="https://www.google.com/chrome/">
                    Chrome
                </USExtLink>
                ,{" "}
                <USExtLink href="https://www.mozilla.org/en-US/firefox/new/">
                    Firefox
                </USExtLink>
                ,{" "}
                <USExtLink href="https://www.apple.com/safari/">
                    Safari
                </USExtLink>
                ,{" "}
                <USExtLink href="https://www.microsoft.com/en-us/edge">
                    Edge
                </USExtLink>
                ). ReportStream doesn't support Internet Explorer 11 or below.
            </p>
            <hr />
            <h3>
                I just activated my ReportStream account, why can't I log in?{" "}
            </h3>

            <p>
                ReportStream shares some resources with other projects operated
                by the CDC (e.g.{" "}
                <USLink href="https://www.simplereport.gov/">
                    SimpleReport
                </USLink>
                ).
            </p>
            <p>
                Some aspects of the registration process may contain references
                to SimpleReport. To access your ReportStream user account, be
                sure to log in at{" "}
                <USLink href="/login">reportstream.cdc.gov/login</USLink>.{" "}
            </p>
            <p>
                For any other issues logging in,{" "}
                <USLink href="/support/contact">contact us</USLink>.
            </p>
            <hr />
            <h3>
                What's the difference between ReportStream and SimpleReport?
            </h3>
            <p>
                ReportStream and{" "}
                <USLink href="https://simplereport.gov">SimpleReport</USLink>{" "}
                are sister projects under the{" "}
                <USLink href="https://www.cdc.gov/surveillance/projects/pandemic-ready-it-systems.html">
                    Pandemic-Ready Interoperability Modernization Effort (PRIME)
                </USLink>
                , operated by the CDC.
            </p>
            <p>
                ReportStream is an open source, cloud- based platform that
                aggregates reportable disease test results from healthcare
                clinics, hospitals, laboratories, and other organizations, and
                delivers them to public health departments.
            </p>
            <p>
                SimpleReport is a cloud-based application that helps
                organizations to collect and submit reportable disease test
                results to public health. Data submitted with SimpleReport is
                routed through ReportStream and delivered to public health
                departments.
            </p>
        </>
    );
};
