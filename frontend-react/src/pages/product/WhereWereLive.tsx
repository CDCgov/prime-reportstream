import { Helmet } from "react-helmet";

// NOTE: update live.json and open usa_w_territories.svg with TEXT EDITOR and uncomment state styles
import live from "../../content/live.json";
import usamapsvg from "../../content/usa_w_territories.svg"; // in /content dir to get unique filename per build

export const WhereWereLiveIa = () => {
    return (
        <>
            <Helmet>
                <title>
                    Where we're live | Product | {process.env.REACT_APP_TITLE}
                </title>
            </Helmet>
            <h1 aria-describedby="product-heading-description" id="anchor-top">
                Where we're live
            </h1>
            <p className="usa-intro text-base">
                ReportStream is currently live or getting set up in
                jurisdictions across the United States.{" "}
            </p>
            <p className="usa-intro text-base">
                Don't see your state or territory?{" "}
                <a href="/support/contact" className="margin-left-1 usa-link">
                    Get in touch
                </a>
                .
            </p>
            <img
                className="margin-bottom-6"
                src={usamapsvg}
                alt="Map of states using ReportStream"
            />
            ReportStream has established connections to send and report public
            health data for each of the states and territories listed here.
            <ul className={"rs-livestate-two-column"}>
                {live.data
                    .sort((a, b) => a.state.localeCompare(b.state))
                    .map((data) => (
                        <li key={`key_${data.state}`}>{data.state}</li>
                    ))}
            </ul>
            Companies or testing facilities sending test results may still need
            to register directly with the state before sending data to their
            public health department.
        </>
    );
};
