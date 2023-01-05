import { SectionProp } from "../HomeProps";
import { USLink } from "../../../components/USLink";

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
            <USLink data-testid="email-link" href="/support/contact">
                <button className="usa-button">Get in touch</button>
            </USLink>
        </div>
    );
}
