import type { TocEntry } from "remark-mdx-toc";
import Slugger from "github-slugger";

import { USSmartLink } from "../../components/USLink";

const slugger = new Slugger();

function sluggifyToc(items: TocEntry[]) {
    const sluggedItems = items.map((i) => {
        return {
            ...i,
            children: i.children ? sluggifyToc(i.children) : i.children,
            attributes: i.attributes.id
                ? i.attributes
                : {
                      ...i.attributes,
                      id: slugger.slug(i.value),
                  },
        };
    }) as SluggedTocEntry[];

    slugger.reset();

    return sluggedItems;
}

export function TableOfContents({
    items,
    depth = 6,
    isInPage = false,
}: {
    depth?: number;
    items: TocEntry[];
    isInPage?: boolean;
}) {
    const sluggedItems = sluggifyToc(items);
    const listItems = sluggedItems
        .filter((i) => i.depth <= depth)
        .map((i) => (
            <TableOfContentsEntry
                key={i.attributes.id}
                {...i}
                maxDepth={depth}
                isInPage={isInPage}
            />
        ));

    return <ul>{listItems}</ul>;
}

export interface SluggedTocEntry extends TocEntry {
    children: SluggedTocEntry[];
    attributes: {
        id: string;
    };
}

export function TableOfContentsEntry({
    children,
    depth,
    value,
    attributes,
    maxDepth = 6,
    isInPage = false,
}: SluggedTocEntry & { maxDepth?: number; isInPage?: boolean }) {
    return (
        <li>
            <USSmartLink href={`#${attributes.id}`}>{value}</USSmartLink>
            {children.length && depth + 1 <= maxDepth ? (
                <TableOfContents
                    items={children}
                    depth={maxDepth}
                    isInPage={isInPage}
                />
            ) : null}
        </li>
    );
}
