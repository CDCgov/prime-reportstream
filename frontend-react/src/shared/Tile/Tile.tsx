import React from "react";
import DOMPurify from "dompurify";

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
}: TileProps) => {
    const cleanSummaryHtml = DOMPurify.sanitize(summary ?? "");

    return (
        <div className={className}>
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
                    className="usa-prose maxw-mobile-lg font-sans-lg text-bold padding-top-3 border-top-05 border-primary"
                >
                    {title}
                </p>
            )}
            <p
                data-testid="summary"
                className="usa-prose maxw-mobile-lg"
                dangerouslySetInnerHTML={{ __html: cleanSummaryHtml }}
            ></p>
        </div>
    );
};
