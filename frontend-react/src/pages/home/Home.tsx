import { Link } from "@trussworks/react-uswds";
import React from "react";

import { USLink, USSmartLink } from "../../components/USLink";

import content from "./content.json";
import Hero from "./Hero";
import Section from "./Sections/Section";
import Feature from "./Features/Feature";
import LiveMapSection from "./Sections/LiveMapSection";
import OtherPartnersSection from "./Sections/OtherPartnersSection";

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
            <div className="bg-primary-lighter">
                <div
                    data-testid="container-get-started"
                    className="grid-container"
                >
                    {content.sections.map((section, sectionIndex) => {
                        return (
                            <section
                                data-testid="section"
                                key={`home-${sectionIndex}`}
                                className="usa-section margin-y-0 tablet:padding-top-9 tablet:padding-bottom-9"
                            >
                                <div className="grid-row grid-gap">
                                    <Section section={section} />
                                </div>
                                <div className="grid-row grid-gap margin-y-8">
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
                                <div className="grid-row">
                                    <div className="grid-col-5"></div>
                                    <Link href="" className="usa-button">
                                        Get Started
                                    </Link>
                                </div>
                            </section>
                        );
                    })}
                </div>
            </div>
            <div
                data-testid="container-how-it-works"
                className="grid-container"
            >
                <section className="usa-section padding-y-9">
                    <h2 className="font-sans-xl margin-top-0 tablet:margin-bottom-0">
                        How it works
                    </h2>
                    <p className="usa-intro margin-top-4">
                        Our open-source platform aggregates and{" "}
                        <USSmartLink href="/resources/security-practices">
                            securely delivers
                        </USSmartLink>{" "}
                        healthcare data test results from organizations and
                        testing facilities directly to public health entities.
                        Born in response to the COVID-19 pandemic, ReportStream
                        is built to thrive and evolve within the complex
                        environment of public health.
                    </p>
                    <img
                        className="margin-y-8"
                        src="/assets/img/ReportStreamDiagram_2022.png"
                        alt="Organizations and testing facilities submit data, which ReportStream validates, augments and standardizes. Then, the data is routed as ReportStream determines the destination by subscription management. After this, it is transformed and transported to public health agencies in the desired data model and batch size."
                    />
                    <p>
                        Learn what’s next for ReportStream and how we are
                        working to improve your experience and advance public
                        health.
                    </p>
                    <h3 className="font-sans-lg margin-y-4">
                        Accepted data types and conditions
                    </h3>
                    <p>
                        Check out the current data types we accept and the
                        growing list of conditions that ReportStream can help
                        you send or receive.
                    </p>
                </section>
            </div>
            <div className="bg-primary-lighter">
                {/* INFO
                    this block of code, similar to the one above, maps through the otherProducts array in content.json to
                    render out all its contents. */}
                <div
                    data-testid="container-other-products"
                    className="grid-container"
                >
                    {content.otherProducts.map((section, sectionIndex) => {
                        return (
                            <section
                                data-testid="section"
                                key={`home-${sectionIndex}`}
                                className="usa-section margin-y-0 tablet:padding-top-9 tablet:padding-bottom-9"
                            >
                                <div className="grid-row grid-gap">
                                    <Section section={section} />
                                </div>
                                <div className="grid-row grid-gap margin-y-8">
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
                                <p>
                                    Questions about other products? Our team
                                    would be{" "}
                                    <USLink href="/support/contact">
                                        happy to talk more
                                    </USLink>{" "}
                                    and explore how we can help you.
                                </p>
                            </section>
                        );
                    })}
                </div>
            </div>
            <div data-testid="container-map" className="grid-container">
                {/* INFO
                    this block of code, similar to the one above, maps through the liveMapContact array in content.json to
                    render out all its contents. */}
                {content.liveMapContact.map((section, sectionIndex) => {
                    return (
                        <section
                            data-testid="section"
                            key={`livemap-${sectionIndex}`}
                            className="usa-section margin-y-0 tablet:padding-top-9 tablet:padding-bottom-9"
                        >
                            <LiveMapSection section={section} />
                        </section>
                    );
                })}
            </div>
            <div
                data-testid="container-other-partners"
                className="grid-container"
            >
                {/* INFO
                    this block of code, similar to the one above, maps through the otherPartners array in content.json to
                    render out all its contents. */}
                {content.otherPartners.map((section, sectionIndex) => {
                    return (
                        <section
                            data-testid="section"
                            key={`partner-${sectionIndex}`}
                            className="usa-section margin-y-0 padding-top-9"
                        >
                            <OtherPartnersSection section={section} />
                        </section>
                    );
                })}
            </div>
            <div className="bg-primary-lighter margin-bottom-neg-8">
                {/* INFO
                    this block of code, renders out Connect with us content. */}
                <div data-testid="container-connect" className="grid-container">
                    <section className="usa-section margin-y-0 tablet:padding-top-9 tablet:padding-bottom-9">
                        <div className="grid-row">
                            <div className="grid-col-2"></div>
                            <h2
                                data-testid="heading"
                                className="font-sans-xl margin-top-0 tablet:margin-bottom-0"
                            >
                                Still exploring if ReportStream is right for
                                you?
                            </h2>
                            <div className="grid-col-1"></div>
                            <p className="usa-intro margin-top-4 margin-bottom-4">
                                Our team will respond to your questions or set
                                up a time to learn more about how we can help
                                you.
                            </p>
                        </div>
                        <div className="grid-row">
                            <Link
                                href="mailto:reportstream@cdc.gov"
                                className="usa-button grid-offset-5"
                            >
                                Connect with us
                            </Link>
                        </div>
                    </section>
                </div>
            </div>
        </>
    );
};

export default Home;
