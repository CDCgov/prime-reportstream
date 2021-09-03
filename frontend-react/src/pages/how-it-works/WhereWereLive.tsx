import live from "../../content/live.json";
import site from "../../content/site.json";
import CdcMap from "@cdc/map";

export const WhereWereLive = () => {
    return (
        <>
            <section id="anchor-top">
                <h1 className="margin-top-0">Where we're live</h1>

                <p className="usa-intro">
                    ReportStream is currently live or getting set up in
                    jurisdictions across the United States.{" "}
                </p>
                <p className="usa-intro">
                    Don’t see your state or territory?{" "}
                    <a href={
                        "mailto:" +
                        site.orgs.RS.email +
                        "?subject=Getting started with ReportStream"
                    } className="margin-left-1">Get in touch</a>.
                </p>
            </section>
            <section>
                <CdcMap config={live} />
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
