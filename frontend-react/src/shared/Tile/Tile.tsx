import React, { ReactNode } from "react";
import DOMPurify from "dompurify";

export interface TileProps {
    className?: string;
    children?: ReactNode;
    img?: string;
    imgAlt?: string;
    imgClassName?: string;
    title?: string;
    summary?: string;
}

export const Tile = ({
    children,
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
                    className="usa-prose font-sans-lg text-bold padding-top-3 border-top-05 border-primary"
                >
                    {title}
                </p>
            )}
            {summary && (
                <p
                    data-testid="summary"
                    className="usa-prose"
                    dangerouslySetInnerHTML={{ __html: cleanSummaryHtml }}
                ></p>
            )}
            {children}
        </div>
    );
};
