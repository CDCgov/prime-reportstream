import classNames from "classnames";
import DOMPurify from "dompurify";
import { ReactNode } from "react";

import styles from "./Tile.module.scss";

export interface TileProps {
    className?: string;
    children?: ReactNode;
    img?: string;
    imgAlt?: string;
    imgClassName?: string;
    title?: string;
    summary?: string;
}

const Tile = ({ children, className, img, imgAlt, imgClassName, title, summary }: TileProps) => {
    const cleanSummaryHtml = DOMPurify.sanitize(summary ?? "");
    const classnames = classNames(styles["rs-tile"], className);
    return (
        <div className={classnames}>
            {img && <img data-testid="img" src={img} alt={imgAlt} className={imgClassName} />}
            {title && (
                <p data-testid="heading" className="usa-prose font-sans-lg text-bold">
                    {title}
                </p>
            )}
            {cleanSummaryHtml && (
                <p
                    data-testid="summary"
                    className="usa-prose font-sans-md"
                    dangerouslySetInnerHTML={{ __html: cleanSummaryHtml }}
                ></p>
            )}
            {children}
        </div>
    );
};

export default Tile;
