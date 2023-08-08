import { Link } from "@trussworks/react-uswds";

import { SectionProp } from "../HomeProps";
import usamapsvg from "../../../content/usa_w_territories.svg";
import { USLink } from "../../../components/USLink";

import CitationSection from "./Citation";
import styles from "./LiveMapSection.module.scss";

export default function LiveMapSection({ section }: { section: SectionProp }) {
    return (
        <div>
            <h2
                data-testid="heading"
                className="font-sans-xl margin-top-0 tablet:margin-bottom-0"
            >
                {section.title}
            </h2>
            <p data-testid="summary" className="usa-intro margin-top-4">
                {section.summary}
            </p>
            <h3 data-testid="subTitle" className="font-sans-lg margin-top-4">
                {section.subTitle}
            </h3>
            <div
                data-testid="map"
                className="grid-col-8 grid-offset-2 margin-y-8"
            >
                <USLink href="/product/where-were-live">
                    <img
                        src={usamapsvg}
                        title="USA with Territories (Heitordp, CC0, via Wikimedia Commons)"
                        alt="Map of states using ReportStream"
                    />
                </USLink>
                <div className="grid-offset-3">
                    <ul className={styles.legend}>
                        <li>
                            <span className="bg-primary"></span> Connected
                        </li>
                        <li>
                            <span className="bg-gray-10"></span> Not Connected
                        </li>
                    </ul>
                </div>
            </div>
            <div className="grid-row padding-top-4">
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
