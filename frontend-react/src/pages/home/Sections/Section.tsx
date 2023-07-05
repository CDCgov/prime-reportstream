import { SectionProp } from "../HomeProps";

import LiveMapSection from "./LiveMapSection";

export default function Section({ section }: { section: SectionProp }) {
    if (section.type === "liveMap") return <LiveMapSection section={section} />;
    else
        return (
            <div className="tablet:grid-col-8">
                <h2
                    data-testid="heading"
                    className="font-sans-lg tablet:font-sans-xl margin-top-0 tablet:margin-bottom-0"
                >
                    {section.title}
                </h2>
                <p
                    data-testid="paragraph"
                    className="usa-intro margin-top-1 text-base"
                >
                    {section.summary}
                </p>
            </div>
        );
}
