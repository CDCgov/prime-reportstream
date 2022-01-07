import { Helmet } from "react-helmet";

import schema from "../../../content/getting_started_csv_upload.json";

/* eslint-disable jsx-a11y/anchor-has-content */
export const CsvSchemaDocumentation = () => {
    return (
        <>
            <Helmet>
                <title>
                    CSV schema documentation | Organizations and testing facilities | Getting started |{" "}
                    {process.env.REACT_APP_TITLE}
                </title>
            </Helmet>
            <section id="anchor-top">
                <span className="text-base text-italic">Updated: January 2022</span>
                <h2 className="margin-top-0 ">CSV schema documentation <span className="text-secondary bg-white border-1px border-secondary font-body-3xs padding-x-1 padding-y-05 text-secondary margin-left-2 text-ttbottom">Pilot program </span></h2>
                
                <p><strong>In this guide</strong></p> 
                <ul>
                    {schema.fields.map((field, fieldIndex) => {
                        return (
                            <div
                                key={`toc-${fieldIndex}`}
                                className=""
                            >
                                {field.sections?.map((section, sectionIndex) => {
                                    return (
                                        <li key={`toc-${fieldIndex}-${sectionIndex}`}>
                                            <a href={`#${section.slug}`} className="usa-link">
                                                {section.title}
                                            </a>
                                        </li>
                                    );
                                })}
                            </div>
                        );
                    })}
                </ul>
                <p><strong>Resources</strong></p> 
                <ul>
                    <li><a href="#preparing-a-csv" className="usa-link">ReportStream standard schema CSV</a></li>
                </ul> 
                <p><strong>About required, requested, and optional fields</strong></p> 
                <ul>
                    <li>Required: Files will fail to be processed if required fields are left blank or contain incorrect values.</li>
                    <li>Requested: explanation of requested</li>
                    <li>Optional: explanation of optinoal</li>
                </ul>
                
            </section>
            
            {schema.fields.map((field, fieldIndex) => {
                return (
                    <div
                        data-testid="fieldDiv"
                        key={`field-${fieldIndex}`}
                        
                        className="margin-bottom-5"
                    >
                        {field.sections?.map((section, sectionIndex) => {
                            return (
                                <div 
                                    key={`section-${fieldIndex}-${sectionIndex}`}
                                    className="border-top-1px border-ink margin-top-9"
                                >
                                    <h3
                                        id={`${section.slug}`}
                                        className="font-body-lg margin-y-1"
                                    >
                                        {section.title}
                                    </h3>

                                    {section.items?.map((item, itemIndex) => {
                                        return (
                                            <div
                                                key={`item-${fieldIndex}-${sectionIndex}-${itemIndex}`}
                                                className="margin-top-8"
                                            >
                                                
                                                <h4 
                                                    id={`doc-${item.colHeader}`}
                                                    className="font-body-md margin-bottom-2"
                                                >
                                                    {item.name}
                                                    {item.required ? <span className="text-normal bg-white border-1px border-secondary font-body-3xs padding-x-1 padding-y-05 text-secondary margin-left-2 text-ttbottom">Required</span> : <span className="text-normal bg-white border-1px border-base font-body-3xs padding-x-1 padding-y-05 text-base margin-left-2 text-ttbottom">Optional</span>}
                                                    {section.title === 'Ask on entry (AOE) data elements'? <span className="text-normal bg-white border-1px border-base font-body-3xs padding-x-1 padding-y-05 text-base margin-left-2 text-ttbottom">Requested</span> : null}
                                                </h4>
                                                <div className="margin-bottom-3">
                                                    {item.notes?.map(
                                                        (note, noteIndex) => {
                                                            return (
                                                                <p key={`value-${fieldIndex}-${sectionIndex}-${itemIndex}-${noteIndex}`}>{note}</p>
                                                            );
                                                        }
                                                    )}
                                                </div>                        
                                                <div className="grid-row margin-bottom-05">
                                                    <div className="grid-col-4 text-base">Column header</div>
                                                    <div className="grid-col-auto">{item.colHeader}</div>
                                                </div>
                                                <div className="grid-row margin-bottom-05 border-base-lighter border-top-1px padding-top-1">
                                                    <div className="grid-col-4 text-base">Value type</div>
                                                    <div className="grid-col-auto">{item.valueType}</div>
                                                </div>
                                                <div className="grid-row margin-bottom-05 border-base-lighter border-top-1px padding-top-1">
                                                    <div className="grid-col-4 text-base">
                                                        {item.acceptedFormat ? <>Accepted format</> : null }
                                                        {item.acceptedValues ? <>Accepted value(s)</> : null}
                                                        {item.acceptedExample ? <>Example(s)</> : null}
                                                    </div>
                                                    <div className="grid-col-8">
                                                        {item.values?.map(
                                                            (value, valueIndex) => {
                                                                return (
                                                                    <div 
                                                                        key={`value-${fieldIndex}-${sectionIndex}-${itemIndex}-${valueIndex}`}
                                                                        dangerouslySetInnerHTML={{ __html: `${value}` }}
                                                                    ></div>
                                                                );
                                                            }
                                                        )}
                                                    </div>
                                                </div>
                                                
                                            </div>
                                        )
                                    })}
                                </div>
                            );
                        })}
                        <p><a href="#anchor-top" className="usa-link">Return to top</a></p>
                    </div>
                );
            })}
                
        </>
    );
};
