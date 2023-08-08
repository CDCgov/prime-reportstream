import { Link } from "@trussworks/react-uswds";

import { SectionProp } from "../HomeProps";

import CitationSection from "./Citation";

export default function OtherPartnersSection({
    section,
}: {
    section: SectionProp;
}) {
    return (
        <div>
            <h3 data-testid="subTitle" className="font-sans-lg margin-top-0">
                {section.subTitle}
            </h3>
            <div className="grid-row margin-top-8 margin-bottom-9">
                <Link
                    href="/product/where-were-live"
                    className="usa-button usa-button--outline grid-offset-5"
                >
                    See all partners
                </Link>
            </div>
            {section.citation?.map((citation, citationIndex) => {
                return (
                    <CitationSection
                        data-testid="citation"
                        key={citationIndex}
                        citation={citation}
                    />
                );
            })}
        </div>
    );
}
