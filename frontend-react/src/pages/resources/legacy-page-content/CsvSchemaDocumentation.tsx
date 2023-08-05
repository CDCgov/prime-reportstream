import DOMPurify from "dompurify";
import { Helmet } from "react-helmet-async";

import site from "../../../content/site.json";
import schema from "../../../content/getting_started_csv_upload.json";
import { USExtLink, USLink } from "../../../components/USLink";

export type CsvSchemaItem = {
    name: string;
    colHeader: string;
    required: boolean;
    requested: boolean;
    acceptedFormat: boolean;
    acceptedValues: boolean;
    acceptedExample: boolean;
    valueType: string;
    values: string[];
    notes: string[];
};

type CsvSchemaItemProps = {
    item: CsvSchemaItem;
    className?: string;
};

export const CsvSchemaDocumentationItem: React.FC<CsvSchemaItemProps> = ({
    item,
    className,
}) => {
    return (
        <div className={className}>
            <h4
                id={`doc-${item.colHeader}`}
                className="font-body-md margin-bottom-2"
                data-testid="header"
            >
                {item.name}
                {item.required ? (
                    <span className="text-normal bg-white border-1px border-secondary font-body-3xs padding-x-1 padding-y-05 text-secondary margin-left-2 text-ttbottom">
                        Required
                    </span>
                ) : (
                    <span className="text-normal bg-white border-1px border-base font-body-3xs padding-x-1 padding-y-05 text-base margin-left-2 text-ttbottom">
                        Optional
                    </span>
                )}
                {!item.required && item.requested && (
                    <span className="text-normal bg-white border-1px border-base font-body-3xs padding-x-1 padding-y-05 text-base margin-left-2 text-ttbottom">
                        Requested
                    </span>
                )}
            </h4>
            <div data-testid="notes" className="margin-bottom-3">
                {item.notes?.map((note, noteIndex) => (
                    <p
                        key={`${item.colHeader}-note-${noteIndex}`}
                        dangerouslySetInnerHTML={{
                            __html: DOMPurify.sanitize(note),
                        }}
                    />
                ))}
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
                    {item.acceptedFormat && "Accepted format"}
                    {item.acceptedValues && "Accepted value(s)"}
                    {item.acceptedExample && "Example(s)"}
                </div>
                <ul className="grid-col-8 value-list">
                    {item.values?.map((value, valueIndex) => (
                        <li
                            key={`${item.colHeader}-value-${valueIndex}`}
                            dangerouslySetInnerHTML={{
                                __html: DOMPurify.sanitize(value),
                            }}
                        />
                    ))}
                </ul>
            </div>
        </div>
    );
};

/* eslint-disable jsx-a11y/anchor-has-content */
export const CsvSchemaDocumentation = () => {
    return (
        <>
            <Helmet>
                <title>CSV schema documentation | Resources</title>
            </Helmet>
            <h1 id="anchor-top" className="margin-top-0 ">
                CSV schema documentation{" "}
            </h1>
            <h2 className="usa-intro text-base">
                How to format data for submission to ReportStream via CSV
                upload.
            </h2>
            <p>
                The ReportStream standard CSV schema is a blend of the
                Department of Health and Human Science's (HHS){" "}
                <USExtLink href="https://www.hhs.gov/coronavirus/testing/covid-19-diagnostic-data-reporting/index.html">
                    requirements for COVID-19 test data
                </USExtLink>{" "}
                as well as those of numerous jurisdictions. Data reported using
                this standard schema will be accepted by state, tribal, local,
                or territorial (STLT) health departments{" "}
                <USLink href="/product/where-were-live">partnered</USLink> with
                ReportStream.{" "}
            </p>
            <p className="text-base text-italic">Last updated: May 9, 2022</p>
            <section>
                <div className="usa-alert usa-alert--info margin-top-2 margin-bottom-6">
                    <div className="usa-alert__body">
                        <h3 className="usa-alert__heading font-body-md margin-top-05">
                            About CSV upload
                        </h3>
                        This documentation will help you prepare a file for{" "}
                        <USLink href="/resources/csv-upload-guide">
                            CSV upload
                        </USLink>
                        . This feature is currently being piloted in select
                        jurisdictions with organizations or facilities that have
                        existing Electronic Medical Record (EMR) systems. Pilot
                        partners are selected by recommendation from
                        jurisdictions. Find out if your jurisdiction is{" "}
                        <USLink href="/product/where-were-live">
                            partnered
                        </USLink>{" "}
                        with ReportStream and{" "}
                        <USLink href="/support/contact">contact us</USLink> to
                        learn more.
                    </div>
                </div>

                <p>
                    <strong>In this guide</strong>
                </p>
                <ul>
                    <li>
                        <USLink href="#formatting-guidelines">
                            General formatting guidelines
                        </USLink>
                    </li>
                    {schema.fields.map((field, fieldIndex) => {
                        return (
                            <div key={`toc-${fieldIndex}`} className="">
                                {field.sections?.map(
                                    (section, sectionIndex) => {
                                        return (
                                            <li
                                                key={`toc-${fieldIndex}-${sectionIndex}`}
                                            >
                                                <USLink
                                                    href={`#${section.slug}`}
                                                >
                                                    {section.title}
                                                </USLink>
                                            </li>
                                        );
                                    },
                                )}
                            </div>
                        );
                    })}
                </ul>
                <p>
                    <strong>Resources</strong>
                </p>
                <ul>
                    <li>
                        <USLink
                            id="standard-csv"
                            href={site.assets.standardCsv.path}
                        >
                            ReportStream standard CSV with example data
                        </USLink>
                    </li>
                </ul>
            </section>

            <section className="border-top-1px border-ink margin-top-9">
                <h3
                    id="formatting-guidelines"
                    className="font-body-lg margin-y-1"
                >
                    General formatting guidelines
                </h3>

                <h4 className="margin-top-4">Column headers and order</h4>
                <ul>
                    <li>Column headers can be placed in any order.</li>
                    <li>
                        Column headers must be included as specified in this
                        docgenerators.{" "}
                    </li>
                </ul>

                <h4>Required, requested, and optional fields</h4>

                <p>
                    <span className="text-normal bg-white border-1px border-secondary font-body-3xs padding-x-1 padding-y-05 text-secondary margin-right-1 text-middle">
                        Required
                    </span>
                </p>
                <p>
                    Files <em>must</em> contain these column headers and values.
                    If headers or fields are blank or contain incorrect values
                    you will not be able to submit your file. Accepted values
                    are outlined for each header and field below
                </p>
                <p>
                    <span className="text-normal bg-white border-1px border-base font-body-3xs padding-x-1 padding-y-05 text-base margin-right-1 text-middle">
                        Requested
                    </span>
                </p>
                <p>
                    Fields are not required by HHS, but the data is incredibly
                    helpful to and, in some cases, required by jurisdictions.
                </p>
                <p>
                    <span className="text-normal bg-white border-1px border-base font-body-3xs padding-x-1 padding-y-05 text-base margin-right-1 text-middle">
                        Optional
                    </span>
                </p>
                <p>Fields are not required.</p>
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

                                    {section.items?.map((item, index) => {
                                        return (
                                            <CsvSchemaDocumentationItem
                                                item={item}
                                                key={`CsvSchemaDocumentationItem-${index}`}
                                                className="margin-top-8 rs-documentation__values"
                                            />
                                        );
                                    })}
                                </div>
                            );
                        })}
                        <p className="margin-top-8">
                            <USLink href="#anchor-top" className="usa-link">
                                Return to top
                            </USLink>
                        </p>
                    </div>
                );
            })}
        </>
    );
};
