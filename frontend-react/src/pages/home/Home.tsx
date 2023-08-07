import content from "../../content/content.json";
import { USSmartLink } from "../../components/USLink";

import Hero from "./Hero";
import Section from "./Sections/Section";
import Feature from "./Features/Feature";

/* INFO
   to change any of the content rendered by Home.tsx, Ctrl+click (shortcut for VScode) on the content import above
   to be taken to content.json. There you may make changes within each object held in the section and freeSecure arrays. No
   content is hard-coded in this file. */
export const Home = () => {
    return (
        <>
            <Hero />

            {/* INFO
                this block of code maps through the section array in content.json to render all section
                and the features held in the feature array of each section. */}
            <div data-testid="container" className="grid-container">
                {content.sections.map((section, sectionIndex) => {
                    return (
                        <section
                            data-testid="section"
                            key={`home-${sectionIndex}`}
                            className="usa-section margin-y-0 tablet:padding-top-6 tablet:padding-bottom-3"
                        >
                            <div className="grid-row grid-gap">
                                <Section section={section} />
                            </div>
                            <div className="grid-row grid-gap margin-bottom-4 ">
                                {section.features?.map(
                                    (feature, featureIndex) => {
                                        return (
                                            <Feature
                                                data-testid="feature"
                                                key={`feature-${sectionIndex}.${featureIndex}`}
                                                section={section}
                                                feature={feature}
                                            />
                                        );
                                    },
                                )}
                            </div>
                        </section>
                    );
                })}

                <div className="tablet:margin-bottom-8 usa-prose">
                    <h2 className="font-sans-lg tablet:font-sans-xl margin-top-0 tablet:margin-bottom-0">
                        How it works
                    </h2>
                    <p className="usa-intro margin-top-1 text-base">
                        Our open-source platform aggregates and securely
                        delivers health care data from organizations and testing
                        facilities directly to public health entities.
                    </p>
                    <img
                        src="/assets/img/ReportStreamDiagram_2022.png"
                        alt="Organizations and testing facilities submit data, which ReportStream validates, augments and standardizes. Then, the data is routed as ReportStream determines the destination by subscription management. After this, it is transformed and transported to public health agencies in the desired data model and batch size."
                    />
                    <p>
                        While ReportStream currently works with COVID-19 and
                        mpox data, we are adding more reportable conditions
                        soon.{" "}
                        <USSmartLink href="mailto:reportstream@cdc.gov">
                            Contact us
                        </USSmartLink>{" "}
                        if you would like updates on additional conditions as
                        they become available.
                    </p>
                    <p>
                        <USSmartLink href="/resources/api">
                            Learn how to send data through our API.
                        </USSmartLink>
                    </p>
                </div>

                {/* INFO
                    this block of code, similar to the one above, maps through the liveMapContact array in content.json to
                    render out all its contents. */}
                {content.liveMapContact.map((section, sectionIndex) => {
                    return (
                        <section
                            data-testid="section"
                            key={`livemap-${sectionIndex}`}
                            className="usa-section margin-y-0 tablet:padding-top-2 tablet:padding-bottom-2"
                        >
                            <div className="grid-row grid-gap">
                                <div className="tablet:grid-col-8 tablet:grid-offset-2">
                                    <Section section={section} />
                                </div>
                            </div>
                        </section>
                    );
                })}

                <section className="usa-section margin-y-0 tablet:padding-top-2 tablet:padding-bottom-2">
                    <div className="grid-row grid-gap">
                        <div className="tablet:grid-col-8 tablet:grid-offset-2">
                            <h2 className="font-sans-lg tablet:font-sans-xl margin-top-0 tablet:margin-bottom-0">
                                Let’s work together
                            </h2>
                            <p className="usa-prose">
                                We believe in making the complex simple and that
                                limited resources shouldn’t limit public health.
                                That’s why we work with you and your
                                requirements and why we are committed to keeping
                                this service free. Contact us to explore how we
                                can help you.
                            </p>
                            <USSmartLink
                                href="mailto:reportstream@cdc.gov"
                                className="usa-button"
                            >
                                Connect with us
                            </USSmartLink>
                        </div>
                    </div>
                </section>
            </div>
        </>
    );
};

export default Home;
