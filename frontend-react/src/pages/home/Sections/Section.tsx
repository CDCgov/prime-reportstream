import { SectionProp } from "../HomeProps";

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
