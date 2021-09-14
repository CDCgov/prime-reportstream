import CtaSection from "./CtaSection";
import { SectionProp } from "../HomeProps";
import LiveMapSection from "./LiveMapSection";


export default function Section({ section }: { section: SectionProp }) {
    if (section.type === "cta") return <CtaSection section={section} />;
    else if (section.type === "liveMap")
        return <LiveMapSection section={section} />;
    else
        return (
            <div className="tablet:grid-col-10 ">
                <h2 className="font-sans-lg tablet:font-sans-xl margin-top-0 tablet:margin-bottom-0">
                    {section.title}
                </h2>
                <p className="usa-intro margin-top-1 text-base">
                    {section.summary}
                </p>
            </div>
        );
}