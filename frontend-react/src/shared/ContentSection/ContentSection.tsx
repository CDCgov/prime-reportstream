import { Tile } from "../Tile/Tile";

export interface ContentSectionProps
    extends React.PropsWithChildren<
        React.HTMLAttributes<HTMLElement> & ContentItem
    > {}

export const ContentSection = ({
    title,
    summary,
    items,
    children,
    ...props
}: ContentSectionProps) => {
    const totalItems = items?.length || 0;
    let gridColValue = 12 / totalItems;
    const tileClassname = `tablet:grid-col-${gridColValue} margin-bottom-0`;
    return (
        <section className="usa-section" {...props}>
            <div className="grid-row grid-gap">
                {title && (
                    <h2
                        data-testid="heading"
                        className="font-sans-xl margin-top-0"
                    >
                        {title}
                    </h2>
                )}
                {summary && (
                    <p
                        data-testid="paragraph"
                        className="usa-intro margin-top-4"
                    >
                        {summary}
                    </p>
                )}
            </div>
            {items && (
                <div className="grid-row grid-gap margin-bottom-8">
                    {items?.map((item, itemIndex) => (
                        <Tile
                            data-testid="item"
                            key={`item-${itemIndex}`}
                            {...item}
                            className={tileClassname}
                        />
                    ))}
                </div>
            )}
            {children}
        </section>
    );
};
