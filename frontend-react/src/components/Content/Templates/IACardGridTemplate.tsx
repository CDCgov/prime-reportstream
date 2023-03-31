import { ContentDirectory } from "../MarkdownDirectory";

import { IACardList } from "./IACard";
import { ContentMap, IAComponentProps } from "./IAComponentProps";

export interface IACardGridProps extends IAComponentProps {
    pageName: string;
    subtitle: string;
    sectionOrder?: string[];
}

const ArraySection = ({
    dirs,
    title,
}: {
    dirs: ContentDirectory[];
    title?: string;
}) => {
    const dirsSorted = dirs.sort(function (a, b) {
        if (a.title < b.title) {
            return -1;
        }
        if (a.title > b.title) {
            return 1;
        }
        return 0;
    });
    const order = dirs.map((d) => dirsSorted.indexOf(d) + 1);

    return (
        <section>
            {title ? <h2>{title}</h2> : null}
            <IACardList dirs={dirs} order={order} />
        </section>
    );
};

const MapSections = ({
    cMap,
    order = Array.from(
        new Set(
            Array.from(cMap.keys()).map((d) =>
                typeof d === "string" ? d : d.key
            )
        )
    ),
}: {
    cMap: ContentMap;
    order?: string[];
}) => {
    const cMapEntries = Array.from(cMap.entries());
    const sections = order.map((k) => {
        const entry = cMapEntries.find(([eK, _]) =>
            typeof eK === "string" ? eK === k : eK.key === k
        );

        if (entry === undefined) {
            throw new Error("Could not find dirs for section");
        }

        const [section, dirs] = entry;
        let title, key;

        if (typeof section === "string") {
            title = section;
            key = section;
        } else {
            title = section.label;
            key = section.key;
        }

        return <ArraySection dirs={dirs!!} title={title} key={key} />;
    });

    return <>{sections}</>;
};

export const IACardGridTemplate = ({
    pageName,
    subtitle,
    directories,
    sectionOrder,
}: IACardGridProps) => {
    const contentIsMapped = directories instanceof Map;
    return (
        <>
            <div className="rs-hero__index">
                <div>
                    <h1>{pageName}</h1>
                    <h2>{subtitle}</h2>
                </div>
            </div>
            <div className="usa-prose margin-top-6">
                <div className="grid-row grid-gap">
                    {!contentIsMapped ? (
                        <ArraySection dirs={directories} />
                    ) : (
                        <MapSections cMap={directories} order={sectionOrder} />
                    )}
                </div>
            </div>
        </>
    );
};
