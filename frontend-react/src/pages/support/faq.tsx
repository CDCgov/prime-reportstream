import { Helmet } from "react-helmet";

export const SupportFaq = () => {

    return (
        <>
            <Helmet>
                <title>FAQ | {process.env.REACT_APP_TITLE}</title>
            </Helmet>
            <section className="grid-container tablet:margin-top-6 margin-bottom-5">
                <div className="grid-row grid-gap">
                    <div className="tablet:grid-col-8 usa-prose">
                        <section>
                            <h1 className="margin-top-0">Frequently asked questions (FAQ) </h1>
                            
                            <div className="margin-top-6 border-top border-base-lighter usa-prose">&nbsp;</div>

                            <h2>I just activated my ReportStream account, why can't I log in? </h2>
                            <p>We use Okta to manage and authenticate access to our application. However, you'll log in to your user account at reportstream.cdc.gov/login.  </p>
                            
                            <h2 className="margin-top-4">Having problems logging in? </h2>
                            <p>Trouble logging in to ReportStream or need to reset your password? Go to reportstream.cdc.gov/login or click the button below. </p>

                            <h2 className="margin-top-4">Why does my CSV file keep getting errors from ReportStream? </h2>
                            <p><strong>Incorrect values:</strong> The most common submission errors we get from CSV upload users are incorrectly entered values under the following required data elements: Equipment Mode Name, Test Performed Code, Test Result, Patient Race, Patient Ethnicity. </p>

                            <p><strong>Blank values:</strong> Another common submission error is leaving a field blank underneath a data element that's required. Required fields with blank values can fail the file.</p>

                            <p>We've put together detailed instructions outlining how to prepare a CSV file using the standard schema that'll be accepted by state, tribal, local, or territorial (STLT) health departments partnered with ReportStream.</p> 

                            <h2 className="margin-top-4">Can I sort my CSV columns in any order when I submit the file to ReportStream? </h2>
                            <p>Yes, you can organize your CSV columns in any order within your file. ReportStream looks for data values beneath the column header regardless of where it's located in the file.</p>

                            <h2 className="margin-top-4">Which browser is recommended for using ReportStream? </h2>
                            <p>Our application works best on a modern desktop browser (Chrome, FireFox, Safari, Edge). ReportStream doesn't support Internet Explorer 11 or below.</p>

                            <h2 className="margin-top-4">Where does ReportStream work?</h2>
                            <p>ReportStream is currently live or getting set up in jurisdictions across the United States. Here's a map and list of where we're live.</p> 

                            <h2 className="margin-top-4">What's the difference between ReportStream and SimpleReport? </h2>
                            <p>ReportStream and SimpleReport are sister projects that share similar resources under the Pandemic-Ready Interoperability Modernization Effort (PRIME) operated by the CDC.</p>
                            <p>ReportStream is a cloud-based data router that aggregates COVID-19 test data reporting from healthcare clinics, hospitals, laboratories, and organizations, and delivers them to public health departments.</p>
                            <p>SimpleReport is a cloud-based application that allows organizations who have not reported to public health in the past (e.g. school systems, jails, assisted living facilities, etc.) to submit COVID-19 rapid test results to public health. The data submitted through SimpleReport gets aggregated and routed through ReportStream to public health departments.</p>                       

                            <h2 className="margin-top-4">How much does ReportStream cost?</h2>
                            <p>ReportStream was developed by the CDC for COVID-19 test data reporting and is 100% free.</p>

                        </section>
                    </div>
                </div>
            </section>
        </>
    );
};
