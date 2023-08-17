export interface CitationProp {
    title?: string;
    quote?: string;
    author?: string;
    authorTitle?: string;
}

export const Citation = ({ citation }: { citation: CitationProp }) => {
    return (
        <div className="padding-top-9">
            <p data-testid="title" className="font-sans-lg text-bold">
                {citation.title}
            </p>
            <p data-testid="quote" className="usa-intro">
                "{citation.quote}"
            </p>
            <p data-testid="author" className="font-sans-sm text-bold">
                {citation.author}
            </p>
            <p data-testid="authorTitle" className="margin-top-0">
                {citation.authorTitle}
            </p>
        </div>
    );
};
