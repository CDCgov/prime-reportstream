import { Helmet } from "react-helmet";

/* eslint-disable jsx-a11y/anchor-has-content */
export const CsvUploadGuide = () => {
    return (
        <>
            <Helmet>
                <title>
                    CSV upload guide | Organizations and testing facilities | Getting started |{" "}
                    {process.env.REACT_APP_TITLE}
                </title>
            </Helmet>
            <section id="anchor-top">
                <span className="text-base text-italic">Updated: January 2022</span>
                <h2 className=" margin-top-0">CSV upload guide <span className="text-secondary bg-white border-1px border-secondary font-body-3xs padding-x-1 padding-y-05 text-secondary margin-left-2 text-ttbottom">Pilot program </span></h2>
                <p className="usa-intro text-base margin-bottom-4">
                    Step-by-step instructions and guidance for preparing and uploading COVID-19 test results via a comma-separated values (CSV) file. 
                </p>  
                <h3>How Do I Use this Guide? </h3>
                <p>
                    The ReportStream CSV Uploader Guide is designed for the technical user at the testing facility or sending location who’ll submit COVID-19 data reporting results in small volumes to local, state, and federal jurisdictions using CSV file format. This guide provides step-by-step instructions for preparing and uploading CSV files successfully to ReportStream.
                </p>  
                <h3>What Topics Will I Learn?</h3> 
                <ul>
                    <li>How to prepare a CSV file for ReportStream</li>
                    <li>How to upload a CSV file to ReportStream</li>
                    <li>How to troubleshoot common file submission errors returned by ReportStream</li>
                </ul>           
            </section>
            <section>
                <h3 className="font-body-lg border-top-1px border-ink margin-top-8 margin-bottom-6 padding-top-1">Preparing a CSV file</h3>
                <ol className="usa-process-list">
                    <li className="usa-process-list__item">
                        <h4 className="usa-process-list__heading">
                            Download the ReportStream reference CSV and review the documentation
                        </h4>
                        <p className="margin-top-05">
                            [If your jurisdiction already has a CSV, compare it to our reference file/documentation. If you're starting from scratch, use the reference CSV as a template].
                        </p>
                        <p>
                            [Download the ReportStream reference CSV schema file LINK].
                        </p>
                        <p>
                            [Review the CSV documentation].
                        </p>
                    </li>
                    <li className="usa-process-list__item">
                        <h4 className="usa-process-list__heading">Format your CSV to match the ReportStream reference file</h4>
                        <p>
                            [Whether you're modifying an existing file or creating a new one from scratch, Include all column headers underneath “ReportStream CSV Field Names” (from the documentation)) in your CSV file, even if you don’t have the data. Make sure values are written exactly as displayed.]
                        </p>
                    </li>
                    <li className="usa-process-list__item">
                        <h4 className="usa-process-list__heading">Enter values into your CSV file</h4>
                        <p>
                            [Follow instructions (from the documentation) on how to modify your existing CSV template headers with ReportStream field names, and how to enter correct values underneath each ReportStream Field ]
                        </p>
                        <p>[CSV Column Names” = Displays the column headers used by ReportStream (make sure values are written exactly as displayed below)] </p>
                        <p>[Required fields:Displays whether a value is required (Yes) , requested (Requested) or optional (No) for the ReportStream CSV field ]</p>
                        <p>[Data Requirements for User” = Displays a required or recommended value a user must enter to be accepted by ReportStream (our guidance considers HHS data requirements and standard data accepted by the majority of state, tribal, local, or territorial public health agencies we’ve worked with).]</p>
                    </li>
                    <li className="usa-process-list__item">
                        <h4 className="usa-process-list__heading">Export your CSV</h4>
                        <p>
                            [Export your properly formatted CSV with filled-in data. Export as CSV, NOT XLS, XLXS, or other formats.]
                        </p>
                    </li>
                </ol>            
            </section>            
            <section>
                <h3 className="font-body-lg border-top-1px border-ink margin-top-8 margin-bottom-6 padding-top-1">Uploading a CSV file</h3>
                <p>[description of what uploading is in more detail].</p>
                <ol className="usa-process-list">
                    <li className="usa-process-list__item">
                        <h4 className="usa-process-list__heading">
                            Log in to ReportStream
                        </h4>
                        <p className="margin-top-05">
                        Go to https://reportstream.cdc.gov/login
                           
                        </p>
                        <ul>
                            <li>Go to https://reportstream.cdc.gov/login</li>
                            <li>Enter username</li>
                            <li>Enter password</li>
                            <li>Click Sign In</li>
                        </ul>
                        <p>
                            [image].
                        </p>
                        
                    </li>
                    <li className="usa-process-list__item">
                        <h4 className="usa-process-list__heading">Navigate to "Upload"</h4>
                        <p>
                            [Foo] [image]
                        </p>
                    </li>
                    <li className="usa-process-list__item">
                        <h4 className="usa-process-list__heading">Select file to upload</h4>
                        <p>
                            [Option A: Drag your CSV file from your folder to the upload area. Option B: Click “choose from folder” to browse your computer, select CSV file and click Open. ] [Image]
                        </p>
                    </li>
                    <li className="usa-process-list__item">
                        <h4 className="usa-process-list__heading">Upload the file</h4>
                        <p>
                            [Click uplaod] [Image]
                        </p>
                    </li>
                </ol> 
            </section>
        </>
    );
};
