import { Helmet } from "react-helmet";

export const GettingStartedReportingFacilities = () => {
    return (
        <>
            <Helmet>
                <title>
                    Reporting facilities | Getting started |{" "}
                    {process.env.REACT_APP_TITLE}
                </title>
            </Helmet>
            <section id="anchor-top">
                <h1 className="margin-top-0">Reporting facilities</h1>

                <p className="usa-intro">
                    ReportStream is currently live or getting set up in
                    jurisdictions across the United States.{" "}
                </p>
                <p className="usa-intro">
                    Don’t see your state or territory?{" "}
                    
                        Get in touch
                   
                    .
                </p>
            </section>
            <section>
            
                ReportStream has established connections to send and report
                public health data for each of the states and territories listed
                here.
                <ul>
                    foo
                </ul>
                Companies or testing facilities sending test results may still
                need to register directly with the state before sending data to
                their public health department.
            </section>
        </>
    );
};
