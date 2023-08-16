import { Link } from "@trussworks/react-uswds";

import { SectionProp } from "../Section/Section";

export const OtherPartners = ({ section }: { section: SectionProp }) => {
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
        </div>
    );
};
