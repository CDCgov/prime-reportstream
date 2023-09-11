import { Link } from "@trussworks/react-uswds";

import usamapsvg from "../../content/usa_w_territories.svg";
import { USLink } from "../../components/USLink";

import styles from "./LiveMap.module.scss";

export interface LiveMapProps
    extends React.PropsWithChildren<
        React.HTMLAttributes<HTMLElement> & ContentItem
    > {}

export const LiveMap = ({
    title,
    summary,
    subTitle,
    ...props
}: LiveMapProps) => {
    return (
        <section {...props}>
            <h2
                data-testid="heading"
                className="font-sans-xl margin-top-0 tablet:margin-bottom-0"
            >
                {title}
            </h2>
            <p data-testid="summary" className="usa-intro margin-top-4">
                {summary}
            </p>
            <h3 data-testid="subTitle" className="font-sans-lg margin-top-4">
                {subTitle}
            </h3>
            <div
                data-testid="map"
                className="desktop:grid-col-8 desktop:grid-offset-2 margin-y-8"
            >
                <USLink href="/product/where-were-live">
                    <img
                        src={usamapsvg}
                        title="USA with Territories (Heitordp, CC0, via Wikimedia Commons)"
                        alt="Map of states using ReportStream"
                    />
                </USLink>
                <div className="grid-row flex-justify-center">
                    <ul className={styles.legend}>
                        <li>
                            <span className="bg-primary"></span>
                            Connected
                        </li>
                        <li>
                            <span className="bg-gray-10"></span>
                            Not Connected
                        </li>
                    </ul>
                </div>
            </div>
            <div className="grid-row flex-justify-center">
                <Link
                    href="/product/where-were-live"
                    className="usa-button usa-button--outline"
                >
                    See all partners
                </Link>
            </div>
        </section>
    );
};
