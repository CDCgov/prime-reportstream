import { ItemProp, Tile } from "../Tile/Tile";
import { CitationProp } from "../Citation/Citation";

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

export const Section = ({ section }: { section: SectionProp }) => {
    return (
        <>
            <div className="grid-row grid-gap">
                {section.title && (
                    <h2
                        data-testid="heading"
                        className="font-sans-xl margin-top-0"
                    >
                        {section.title}
                    </h2>
                )}
                {section.summary && (
                    <p
                        data-testid="paragraph"
                        className="usa-intro margin-top-4"
                    >
                        {section.summary}
                    </p>
                )}
            </div>
            {section.items && (
                <div className="grid-row grid-gap margin-bottom-8">
                    {section.items?.map((item, itemIndex) => (
                        <Tile
                            data-testid="item"
                            key={`item-${itemIndex}`}
                            section={section}
                            item={item}
                        />
                    ))}
                </div>
            )}
        </>
    );
};
