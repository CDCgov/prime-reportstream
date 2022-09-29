import { ContentDirectory } from "../MarkdownDirectory";

import { IACardList } from "./IACard";

export type ContentMap = Map<string, ContentDirectory[]>; // Key should be section title
export interface IACardGridProps {
    directories: ContentDirectory[] | ContentMap;
}

const ArraySection = ({
    dirs,
    title,
}: {
    dirs: ContentDirectory[];
    title?: string;
}) => (
    <section>
        {title ? <h3>{title}</h3> : null}
        <IACardList dirs={dirs} />
    </section>
);

const MapSections = ({ cMap }: { cMap: ContentMap }) => {
    const arraySections: JSX.Element[] = [];
    cMap.forEach((value, key) =>
        arraySections.push(
            <ArraySection dirs={value!!} title={key} key={key} />
        )
    );
    return <>{arraySections}</>;
};

export const IACardGridTemplate = ({ directories }: IACardGridProps) => {
    const contentIsMapped = directories instanceof Map;
    return (
        <>
            <div className="grid-container usa-prose margin-top-6">
                <div className="grid-row grid-gap">
                    {!contentIsMapped ? (
                        <ArraySection dirs={directories} />
                    ) : (
                        <MapSections cMap={directories} />
                    )}
                </div>
            </div>
        </>
    );
};
