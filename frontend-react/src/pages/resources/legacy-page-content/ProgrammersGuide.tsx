import { BasicHelmet } from "../../../components/header/BasicHelmet";
import site from "../../../content/site.json";
import { ResourcesDirectories } from "../../../content/resources";
import { USExtLink } from "../../../components/USLink";

export const ProgrammersGuide = () => {
    return (
        <>
            <BasicHelmet
                pageTitle={`${ResourcesDirectories.PROGRAMMERS_GUIDE} | Resources`}
            />
            <h1 id="anchor-top">{ResourcesDirectories.PROGRAMMERS_GUIDE}</h1>
            <h2>
                Full documentation for interacting with the ReportStream API
            </h2>
            <section>
                <h3>Download the guide</h3>
                <p>
                    The ReportStream programmer's guide is available as a PDF
                    (Updated: August 2022)
                </p>
                <p>
                    <button className="usa-button usa-button--outline">
                        {/* External link might be misleading, this is a _download_ link
                        we should consider communicating that better visually */}
                        <USExtLink href={site.assets.programmersGuidePdf.path}>
                            API programmer's guide (pdf)
                        </USExtLink>
                    </button>
                </p>
            </section>
        </>
    );
};
