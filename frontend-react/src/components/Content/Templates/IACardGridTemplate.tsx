import { IACardList } from "../../IACard";
import { ContentDirectory } from "../MarkdownDirectory";

export type ContentMap = Map<string, ContentDirectory[]>; // Key should be section title
export interface IACardGridProps {
    title: string;
    subtitle: string;
    directoriesToRender: ContentDirectory[] | ContentMap;
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

export const IACardGridTemplate = ({
    title,
    subtitle,
    directoriesToRender,
}: IACardGridProps) => {
    const contentIsMapped = directoriesToRender instanceof Map;
    return (
        <>
            <div className="rs-hero__index">
                <div className="grid-container">
                    <h1>{title}</h1>
                    <h2>{subtitle}</h2>
                </div>
            </div>
            <div className="grid-container usa-prose margin-top-6">
                <div className="grid-row grid-gap">
                    {!contentIsMapped ? (
                        <ArraySection dirs={directoriesToRender} />
                    ) : (
                        <MapSections cMap={directoriesToRender} />
                    )}
                </div>
            </div>
        </>
    );
};
