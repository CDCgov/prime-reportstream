import USAMap from "react-usa-map";

import states from "../../content/live.json";

export const WhereWereLive = () => {
    const statesCustomConfig = () => {
        return states;
    };

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
                    <a
                        href="mailto:{{ site.orgs.RS.email }}"
                        className="usa-link"
                    >
                        Get in touch
                    </a>
                    .
                </p>
            </section>
            <section>
                <USAMap customize={statesCustomConfig()} />
                ReportStream has established connections to send and report
                public health data for each of the states and territories listed
                here.
                <ul></ul>
                Companies or testing facilities sending test results may still
                need to register directly with the state before sending data to
                their public health department.
            </section>
        </>
    );
};
