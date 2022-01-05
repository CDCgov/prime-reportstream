import { Helmet } from "react-helmet";
import DOMPurify from "dompurify";

import site from "../../../content/site.json";
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
                <h2 className="margin-top-0 ">CSV schema documentation</h2>
                <p className="usa-intro text-base">
                   
                    {schema.summary}

                </p>
                <p>Required: explanation of required. Files will fail to be processed if required fields are left blank or contain incorrect values.</p>
                <p>Requested: explanation of requested</p>
                <p>Optional: explanation of optinoal</p>
                <p><a
                        href={
                            "mailto:" +
                            DOMPurify.sanitize(site.orgs.RS.email) +
                            "?subject=Getting started with ReportStream"
                        }
                        className="foo"
                    >
                        ReportStream schema CSV
                    </a>
                </p>
                
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
                                                    {item.required ? <span className="text-normal bg-white border-1px border-secondary font-body-3xs padding-x-1 padding-y-05 text-secondary margin-left-2 text-ttbottom">Required</span> : <span className="text-normal bg-white border-1px border-base font-body-3xs padding-x-1 padding-y-05 text-base margin-left-2">Optional</span>}
                                                    {section.title === 'Ask on entry (AOE) data elements'? <span className="text-normal bg-white border-1px border-base font-body-3xs padding-x-1 padding-y-05 text-base margin-left-2">Requested</span> : null}
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
 
                    </div>
                );
            })}
                
        </>
    );
};
