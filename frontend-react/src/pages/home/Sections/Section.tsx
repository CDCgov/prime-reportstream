import { ItemProp } from "../../../shared/Tile/Tile";
import { CitationProp } from "../../../shared/Citation/Citation";

export interface SectionProp {
    title?: string;
    type?: string;
    summary?: string;
    subTitle?: string;
    bullets?: { content?: string }[];
    items?: ItemProp[];
    description?: string;
    buttonText?: string;
    buttonUrlSubject?: string;
    citation?: CitationProp[];
}

export default function Section({ section }: { section: SectionProp }) {
    return (
        <>
            {section.title && (
                <h2 data-testid="heading" className="font-sans-xl margin-top-0">
                    {section.title}
                </h2>
            )}
            {section.summary && (
                <p data-testid="paragraph" className="usa-intro margin-top-4">
                    {section.summary}
                </p>
            )}
        </>
    );
}
