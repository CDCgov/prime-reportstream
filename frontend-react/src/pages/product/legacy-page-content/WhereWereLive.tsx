// NOTE: update live.json and open usa_w_territories.svg with TEXT EDITOR and uncomment state styles
import { Helmet } from "react-helmet-async";

import live from "../../../content/live.json";
import usamapsvg from "../../../content/usa_w_territories.svg"; // in /content dir to get unique filename per build
import { USExtLink, USLink } from "../../../components/USLink";

export const WhereWereLive = () => {
    return (
        <>
            <Helmet>
                <title>Where we're live | Product</title>
            </Helmet>
            <h1 aria-describedby="product-heading-description" id="anchor-top">
                Where we're live
            </h1>
            <h2>
                ReportStream is currently live or getting set up in
                jurisdictions across the United States.{" "}
            </h2>
            <h2>
                Don't see your state or territory?{" "}
                <USLink href="/support/contact" className="margin-left-1">
                    Get in touch
                </USLink>
                .
            </h2>
            <img
                className="margin-bottom-6"
                src={usamapsvg}
                alt="Map of states using ReportStream"
            />
            <p className="font-body-2xs text-gray-50">
                <USExtLink href="https://commons.wikimedia.org/wiki/File:Blank_USA,_w_territories.svg">
                    Heitordp
                </USExtLink>
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
