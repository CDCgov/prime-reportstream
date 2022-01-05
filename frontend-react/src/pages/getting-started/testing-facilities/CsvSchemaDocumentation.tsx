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
                <h2 className="margin-top-0">CSV schema documentation</h2>
                <p className="usa-intro">
                   
                    {schema.summary}

                    <a
                        href={
                            "mailto:" +
                            DOMPurify.sanitize(site.orgs.RS.email) +
                            "?subject=Getting started with ReportStream"
                        }
                        className="margin-left-1"
                    >
                        Get in touch
                    </a>
                    .
                </p>
                
            </section>

            {schema.fields.map((field, fieldIndex) => {
                return (
                    <div
                        data-testid="fieldDiv"
                        key={`field-${fieldIndex}`}
                        className="margin-bottom-5 border-top-1px border-base-lighter"
                    >
                        {field.sections?.map((section, sectionIndex) => {
                            return (
                                <div 
                                    key={`section-${sectionIndex}`}
                                    className="border-top-1px border-base-lighter margin-top-5 padding-top-1"
                                >
                                    <h3>{section.title}</h3>

                                    {section.items?.map((item, itemIndex) => {
                                        return (
                                            <div
                                                key={`item-${itemIndex}`}
                                                className="margin-top-6"
                                            >
                                                
                                                <h4 id={`doc-${item.colHeader}`}>
                                                    {item.name}
                                                    {item.required ? <span className="text-normal bg-white border-1px border-secondary font-body-3xs padding-x-1 padding-y-05 text-secondary margin-left-2 text-ttbottom">Required</span> : <span className="text-normal bg-white border-1px border-base font-body-3xs padding-x-1 padding-y-05 text-base margin-left-2">Optional</span>}
                                                </h4>
                                                <div className="margin-bottom-3">
                                                    {item.notes?.map(
                                                        (note, noteIndex) => {
                                                            return (
                                                                <p key={`value-${noteIndex}`}>{note}</p>
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
                                                        {item.acceptedFormat && <>Accepted format</>}
                                                        {item.acceptedValues && <>Accepted value(s)</>}
                                                        {item.acceptedExample && <>Example(s)</>}
                                                    </div>
                                                    <div className="grid-col-8">
                                                        {item.values?.map(
                                                            (value, valueIndex) => {
                                                                return (
                                                                    <><span key={`value-${valueIndex}`}>{value}</span><br /></>
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
