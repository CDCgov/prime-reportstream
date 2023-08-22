import React from "react";
import DOMPurify from "dompurify";
import classNames from "classnames";

export interface TileProps
    extends React.PropsWithChildren<
        React.HTMLAttributes<HTMLElement> & ContentSubitem
    > {}

export const Tile = ({
    className,
    img,
    imgAlt,
    imgClassName,
    title,
    summary,
    children,
}: TileProps) => {
    const cleanSummaryHtml = DOMPurify.sanitize(summary ?? "");
    const classnames = classNames("usa-prose", "padding-bottom-3", className);
    return (
        <div className={classnames}>
            <hr className="border-y-2px border-primary margin-0 margin-bottom-205" />
            {img && (
                <img
                    data-testid="img"
                    src={img}
                    alt={imgAlt}
                    className={imgClassName}
                />
            )}
            {title && (
                <p
                    data-testid="heading"
                    className="usa-prose maxw-mobile-lg font-sans-lg text-bold"
                >
                    {title}
                </p>
            )}
            {cleanSummaryHtml && (
                <p
                    data-testid="summary"
                    className="usa-prose maxw-mobile-lg"
                    dangerouslySetInnerHTML={{ __html: cleanSummaryHtml }}
                ></p>
            )}
            {children}
        </div>
    );
};
