import { IACardList } from "../../IACard";
import { ContentDirectory } from "../MarkdownDirectory";

export interface IACardGridProps {
    title: string;
    subtitle: string;
    directoriesToRender: ContentDirectory[];
}

export const IACardGridTemplate = ({
    title,
    subtitle,
    directoriesToRender,
}: IACardGridProps) => {
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
                    <section>
                        <IACardList dirs={directoriesToRender} />
                    </section>
                </div>
            </div>
        </>
    );
};
