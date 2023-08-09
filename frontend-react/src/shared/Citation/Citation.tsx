import { CitationProp } from "../../pages/home/HomeProps";

export const Citation = ({ citation }: { citation: CitationProp }) => {
    return (
        <div className="padding-top-9">
            <h3 data-testid="title" className="font-sans-lg">
                {citation.title}
            </h3>
            <p data-testid="quote" className="usa-intro">
                "{citation.quote}"
            </p>
            <h4 data-testid="author">{citation.author}</h4>
            <p data-testid="authorTitle" className="margin-top-0">
                {citation.authorTitle}
            </p>
        </div>
    );
};
