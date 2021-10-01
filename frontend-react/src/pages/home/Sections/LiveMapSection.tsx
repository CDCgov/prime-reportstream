// @ts-nocheck // TODO: fix types in this file
import { SectionProp } from "../HomeProps";
import CdcMap from "@cdc/map";
import live from "../../../content/live.json";

export default function LiveMapSection({ section }: { section: SectionProp }) {
    return (
        <div>
            <h2 className="font-sans-lg tablet:font-sans-xl margin-top-0 tablet:margin-bottom-0">
                {section.title}
            </h2>
            <p className="usa-intro margin-top-1 text-base">
                {section.summary}
            </p>
            <div className="tablet:grid-col-10">
                <CdcMap config={live} />
            </div>
            <p
                className="usa-prose margin-top-2"
                dangerouslySetInnerHTML={{ __html: section!.description! }}
            ></p>
        </div>
    );
};