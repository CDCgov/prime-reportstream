import { Button } from "@trussworks/react-uswds";
import DOMPurify from "dompurify";
import { Helmet } from "react-helmet-async";

import site from "../../../content/site.json";
import { ResourcesDirectories } from "../../../content/resources";

export const ProgrammersGuide = () => {
    return (
        <>
            <Helmet>
                <title>{`${ResourcesDirectories.PROGRAMMERS_GUIDE} | Resources`}</title>
            </Helmet>
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
                    <Button
                        type="button"
                        outline
                        onClick={() =>
                            window.open(
                                DOMPurify.sanitize(
                                    site.assets.programmersGuidePdf.path
                                )
                            )
                        }
                    >
                        API programmer's guide (pdf)
                    </Button>
                </p>
            </section>
        </>
    );
};
