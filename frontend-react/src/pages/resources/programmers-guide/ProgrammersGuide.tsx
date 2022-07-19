import { Helmet } from "react-helmet";
import { Link } from "react-router-dom";

import site from "../../../content/site.json";

export const ProgrammersGuide = () => {
    return (
        <>
            <Helmet>
                <title>
                    Programmer's Guide | Resources |{" "}
                    {process.env.REACT_APP_TITLE}
                </title>
            </Helmet>
            <h1 id="anchor-top">Programmer's guide</h1>
            <h2>
                Full documentation for interacting with the ReportStream API
            </h2>
            <section>
                <h3>Download the guide</h3>
                <p>
                    The ReportStream programmer's guide is available as a PDF.
                </p>
                <p>
                    <Link
                        className={"usa-link"}
                        to={site.assets.programmersGuidePdf.path}
                    >
                        API programmer's guide (pdf)
                    </Link>
                </p>
            </section>
        </>
    );
};
export default ProgrammersGuide;
