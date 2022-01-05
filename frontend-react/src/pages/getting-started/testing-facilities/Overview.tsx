import { Helmet } from "react-helmet";

/* eslint-disable jsx-a11y/anchor-has-content */
export const FacilitiesOverview = () => {
    return (
        <>
            <Helmet>
                <title>
                    Organizations and testing facilities | Getting started |{" "}
                    {process.env.REACT_APP_TITLE}
                </title>
            </Helmet>
            <section id="anchor-top">
                <h2 className="margin-top-0">Overview</h2>
                <p className="usa-intro text-base margin-bottom-6">
                    ReportStream is a free, open-source data platform that makes it easy for public health data to be transferred from organizations and testing facilities to public health departments.
                </p>
                <h3>How do I submit data through ReportStream?</h3>
                <p>[VIA ELR or CSV UPLOAD]. Currently, ReportStream can accept either a comma-separated values (CSV) file or Health Level 7 (HL7) input data format.</p>  
                <h3>What’s the Difference Between CSV and HL7?</h3> 
                <p>HL7 is the data format underlying Electronic Lab Reporting (ELR). HL7 is a set of international standards for transfer of clinical and administrative data between software applications used by various healthcare providers.</p> 
                <p>CSV is a delimited text file that uses a comma to separate values. Each line of the file is a data record. Each record consists of one or more fields, separated by commas.</p>          
                
            </section>

            <section>
                
            </section>
        </>
    );
};
