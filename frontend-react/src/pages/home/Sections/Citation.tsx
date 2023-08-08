import { CitationProp } from "../HomeProps";

export default function CitationSection({
    citation,
}: {
    citation: CitationProp;
}) {
    return (
        <div className="padding-top-9">
            <h3 className="font-sans-lg">{citation.title}</h3>
            <p className="usa-intro">"{citation.quote}"</p>
            <h4>{citation.author}</h4>
            <p className="margin-top-0">{citation.authorTitle}</p>
        </div>
    );
}
