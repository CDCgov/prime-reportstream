import { HTMLAttributes, PropsWithChildren } from "react";

import styles from "./LiveMap.module.scss";
import { USLink } from "../../components/USLink";
import usamapsvg from "../../content/usa_w_territories.svg";

export type LiveMapProps = PropsWithChildren<
    HTMLAttributes<HTMLElement> & ContentItem
>;

const LiveMap = ({
    title,
    summary,
    subTitle,
    description,
    ...props
}: LiveMapProps) => {
    return (
        <section {...props}>
            <h2 data-testid="heading" className="font-sans-xl margin-0">
                {title}
            </h2>
            <p data-testid="summary" className="font-sans-lg margin-top-4">
                {summary}
            </p>
            <h3 data-testid="subTitle" className="font-sans-lg margin-top-4">
                {subTitle}
            </h3>
            <p data-testid="body" className="font-sans-md">
                {description}
            </p>
            <div
                data-testid="map"
                className="desktop:grid-col-8 desktop:grid-offset-2 margin-y-8"
            >
                <USLink href="/about/our-network">
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
        </section>
    );
};

export default LiveMap;
