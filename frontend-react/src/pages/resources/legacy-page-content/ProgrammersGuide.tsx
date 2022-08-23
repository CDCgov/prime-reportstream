import { Helmet } from "react-helmet";

import site from "../../../content/site.json";

export const ProgrammersGuide = () => {
    return (
        <>
            <Helmet>
                <title>
                    API Programmer's Guide | Resources |{" "}
                    {process.env.REACT_APP_TITLE}
                </title>
            </Helmet>
            <h1 id="anchor-top">API Programmer's guide</h1>
            <h2>
                Full documentation for interacting with the ReportStream API
            </h2>
            <section>
                <h3>Download the guide</h3>
                <p>
                    The ReportStream programmer's guide is available as a PDF.
                </p>
                <p>
                    <a
                        className={"usa-button usa-button--outline"}
                        href={site.assets.programmersGuidePdf.path}
                        target="_blank"
                        rel="noreferrer noopener"
                    >
                        API programmer's guide (pdf)
                    </a>
                </p>
            </section>
        </>
    );
};
