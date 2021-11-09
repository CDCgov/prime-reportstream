// @ts-nocheck // TODO: fix types in this file
import DOMPurify from "dompurify";
import { Helmet } from "react-helmet";

import site from "../../content/site.json";
// NOTE: update live.json and open usa_w_territories.svg with TEXT EDITOR and uncomment state styles
// @ts-ignore
import live from "../../content/live.json";
// @ts-ignore
import usamapsvg from "../../content/usa_w_territories.svg"; // in /content dir to get unique filename per build

export const WhereWereLive = () => {
    return (
        <>
            <Helmet>
                <title>
                    Where we're live | How it works |{" "}
                    {process.env.REACT_APP_TITLE}
                </title>
            </Helmet>
            <section id="anchor-top">
                <h1 className="margin-top-0">Where we're live</h1>

                <p className="usa-intro">
                    ReportStream is currently live or getting set up in
                    jurisdictions across the United States.{" "}
                </p>
                <p className="usa-intro">
                    Don’t see your state or territory?{" "}
                    <a
                        href={
                            "mailto:" +
                            DOMPurify.sanitize(site.orgs.RS.email) +
                            "?subject=Getting started with ReportStream"
                        }
                        className="margin-left-1"
                    >
                        Get in touch
                    </a>
                    .
                </p>
            </section>
            <section>
                <img src={usamapsvg} alt="Map of states using ReportStream" />
                ReportStream has established connections to send and report
                public health data for each of the states and territories listed
                here.
                <ul>
                    {live.data
                        .sort((a, b) => a.state.localeCompare(b.state))
                        .map((data) => (
                            <li key={`key_${data.state}`}>{data.state}</li>
                        ))}
                </ul>
                Companies or testing facilities sending test results may still
                need to register directly with the state before sending data to
                their public health department.
            </section>
        </>
    );
};
