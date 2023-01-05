import DOMPurify from "dompurify";

import { SectionProp } from "../HomeProps";
import usamapsvg from "../../../content/usa_w_territories.svg";
import { USLink } from "../../../components/USLink"; // in /content dir to get unique filename per build

export default function LiveMapSection({ section }: { section: SectionProp }) {
    let cleanDescriptionHtml = DOMPurify.sanitize(section!.description!);
    return (
        <div className="tablet:margin-bottom-8">
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
            <div data-testid="map">
                <USLink href="/product/where-were-live">
                    <img
                        src={usamapsvg}
                        title="USA with Territories (Heitordp, CC0, via Wikimedia Commons)"
                        alt="Map of states using ReportStream"
                    />
                </USLink>
            </div>
            <p
                data-testid="description"
                className="usa-prose margin-top-6"
                dangerouslySetInnerHTML={{ __html: cleanDescriptionHtml }}
            ></p>
        </div>
    );
}
