// @ts-nocheck // TODO: fix types in this file
import DOMPurify from "dompurify";

import { SectionProp } from "../HomeProps";

export default function LiveMapSection({ section }: { section: SectionProp }) {
    let cleanDescriptionHtml = DOMPurify.sanitize(section!.description!);
    return (
        <div>
            <h2
                data-testid="heading"
                className="font-sans-lg tablet:font-sans-xl margin-top-0 tablet:margin-bottom-0"
            >
                {section.title}
            </h2>
            <p
                data-testid="summary"
                className="usa-intro margin-top-1 text-base"
            >
                {section.summary}
            </p>
            <div data-testid="map" className="tablet:grid-col-10">
                <img src="/assets/usa_w_territories.svg" alt="Map of states using ReportStream" />
            </div>
            <p
                data-testid="description"
                className="usa-prose margin-top-2"
                dangerouslySetInnerHTML={{ __html: cleanDescriptionHtml }}
            ></p>
        </div>
    );
}
