import content from "../../content/content.json";

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
                            key={`section=${sectionIndex}`}
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
                                    }
                                )}
                            </div>
                        </section>
                    );
                })}

                {/* INFO
                    this block of code, similar to the one above, maps through the liveMapContact array in content.json to
                    render out all its contents. */}                
                {content.liveMapContact.map((section, sectionIndex) => {
                    return (
                        <section
                            data-testid="section"
                            key={`section=${sectionIndex}`}
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
            </div>
        </>
    );
};

export default Home;
