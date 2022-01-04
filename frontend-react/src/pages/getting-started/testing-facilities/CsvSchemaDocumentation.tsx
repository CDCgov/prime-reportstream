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
                        className="margin-bottom-6"
                    >
                        <h3>
                            {field.name}
                            {field.required && <span className="text-normal text-xs text-secondary margin-left-2">Required</span>}
                        </h3>
                        <div className="margin-top-neg-1 margin-bottom-3">
                            {field.notes?.map(
                                (note, noteIndex) => {
                                    return (
                                        <p key={`value-${noteIndex}`}>{note}</p>
                                    );
                                }
                            )}
                        </div>
                        
                        <div className="grid-row margin-bottom-05">
                            <div className="grid-col-4 text-base">Column header</div>
                            <div className="grid-col-auto">{field.colHeader}</div>
                        </div>
                        <div className="grid-row margin-bottom-05">
                            <div className="grid-col-4 text-base">Value type</div>
                            <div className="grid-col-auto">{field.valueType}</div>
                        </div>
                        <div className="grid-row margin-bottom-05">
                            <div className="grid-col-4 text-base">Accepted value(s)</div>
                            <div className="grid-col-8 font-family-mono">
                                {field.values?.map(
                                    (value, valueIndex) => {
                                        return (
                                            <><span key={`value-${valueIndex}`}>{value}</span><br /></>
                                        );
                                    }
                                )}
                            </div>
                        </div>
                    </div>
                );
            })}
                
        </>
    );
};
