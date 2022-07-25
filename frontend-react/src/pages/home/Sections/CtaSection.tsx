import { SectionProp } from "../HomeProps";

export default function CtaSection({ section }: { section: SectionProp }) {
    return (
        <div>
            <h2
                data-testid="heading"
                className="font-sans-lg tablet:font-sans-xl margin-top-0 tablet:margin-bottom-0"
            >
                {section.title}
            </h2>
            <p data-testid="description" className="usa-prose">
                {section.description}
            </p>
            <p data-testid="summary" className="usa-prose">
                {section.summary}
            </p>
            <a
                data-testid="email-link"
                href="/support/contact"
                className="usa-button"
            >
                Get in touch
            </a>
        </div>
    );
}
