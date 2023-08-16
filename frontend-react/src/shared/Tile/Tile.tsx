import React from "react";
import DOMPurify from "dompurify";

import { SectionProp } from "../Section/Section";

export interface ItemProp {
    method?: number;
    title?: string;
    icon?: string;
    img?: string;
    imgAlt?: string;
    imgClassName?: string;
    summary?: string;
    items?: { title?: string; summary?: string }[];
}

export const Tile = ({
    section,
    item,
}: {
    section: SectionProp;
    item: ItemProp;
}) => {
    let cleanSummaryHtml = DOMPurify.sanitize(item!.summary!);
    const totalItems = section.items?.length || 0;
    let gridColValue = 12 / totalItems;
    const styleItems = `tablet:grid-col-${gridColValue} margin-bottom-0`;

    return (
        <div className={styleItems}>
            {item.img && (
                <img
                    data-testid="img"
                    src={item.img}
                    alt={item.imgAlt}
                    className={item.imgClassName}
                />
            )}
            {item.title && (
                <h3
                    data-testid="heading"
                    className="usa-prose maxw-mobile-lg font-sans-lg padding-top-3 border-top-05 border-primary"
                >
                    {item.title}
                </h3>
            )}
            <p
                data-testid="summary"
                className="usa-prose maxw-mobile-lg"
                dangerouslySetInnerHTML={{ __html: cleanSummaryHtml }}
            ></p>
        </div>
    );
};
