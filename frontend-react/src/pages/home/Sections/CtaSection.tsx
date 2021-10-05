import { SectionProp } from "../HomeProps";
import site from "../../../content/site.json";

export default function CtaSection({ section }: { section: SectionProp }) {
    return (
        <div className="tablet:grid-col-8">
            <h2 className="font-sans-lg tablet:font-sans-xl margin-top-0 tablet:margin-bottom-0">
                {section.title}
            </h2>
            <p className="usa-prose">{section.description}</p>
            <p className="usa-prose">{section.summary}</p>
            <a
                href={
                    "mailto:" +
                    site.orgs.RS.email +
                    "?subject=Getting started with ReportStream"
                }
                className="usa-button"
            >
                Get in touch
            </a>
        </div>
    );
}
