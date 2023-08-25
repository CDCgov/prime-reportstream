export interface CitationProps
    extends React.PropsWithChildren<
        React.HTMLAttributes<HTMLElement> & CitationItem
    > {}

export const Citation = ({
    title,
    quote,
    author,
    authorTitle,
    ...props
}: CitationProps) => {
    return (
        <div className="padding-top-9" {...props}>
            <p data-testid="title" className="font-sans-lg text-bold">
                {title}
            </p>
            <p data-testid="quote" className="usa-intro">
                "{quote}"
            </p>
            <p data-testid="author" className="font-sans-sm text-bold">
                {author}
            </p>
            <p data-testid="authorTitle" className="margin-top-0">
                {authorTitle}
            </p>
        </div>
    );
};
