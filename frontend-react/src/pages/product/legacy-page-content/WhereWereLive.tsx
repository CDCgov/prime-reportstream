// NOTE: update live.json and open usa_w_territories.svg with TEXT EDITOR and uncomment state styles
import live from "../../../content/live.json";
import usamapsvg from "../../../content/usa_w_territories.svg"; // in /content dir to get unique filename per build
import { BasicHelmet } from "../../../components/header/BasicHelmet";

export const WhereWereLive = () => {
    return (
        <>
            <BasicHelmet pageTitle="Where we're live | Product" />
            <h1 aria-describedby="product-heading-description" id="anchor-top">
                Where we're live
            </h1>
            <h2>
                ReportStream is currently live or getting set up in
                jurisdictions across the United States.{" "}
            </h2>
            <h2>
                Don't see your state or territory?{" "}
                <a href="/support/contact" className="margin-left-1 usa-link">
                    Get in touch
                </a>
                .
            </h2>
            <img
                className="margin-bottom-6"
                src={usamapsvg}
                alt="Map of states using ReportStream"
            />
            <p className="font-body-2xs text-gray-50">
                <a
                    href="https://commons.wikimedia.org/wiki/File:Blank_USA,_w_territories.svg"
                    rel="noreferrer noopener"
                    className="usa-link--external"
                >
                    Heitordp
                </a>
                , CC0, via Wikimedia Commons
            </p>
            <p className="margin-top-10">
                ReportStream has established connections to send and report
                public health data for each of the states and territories listed
                here.
            </p>
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
