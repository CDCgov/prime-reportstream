import { useNavigate } from "react-router-dom";
import { Button } from "@trussworks/react-uswds";

import { SectionProp } from "../HomeProps";

export default function CtaSection({ section }: { section: SectionProp }) {
    const navigate = useNavigate();
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
            <Button type="button" onClick={() => navigate("/support/contact")}>
                Get in touch
            </Button>
        </div>
    );
}
