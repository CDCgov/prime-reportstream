import content from "../../content/content.json";
import { library } from "@fortawesome/fontawesome-svg-core";
import { fas } from "@fortawesome/free-solid-svg-icons";
import Hero from './Hero'
import Section from "./Sections/Section";
import Feature from "./Features/Feature";

/* INFO
   to change any of the content rendered by Home.tsx, Ctrl+click (shortcut for VScode) on the content import above 
   to be taken to content.json. There you may make changes within each object held in the section and freeSecure arrays. No
   content is hard-coded in this file. */
export const Home = () => {
    library.add(fas);
    return (
        <>
            <Hero />

            {/* INFO
                this block of code maps through the section array in content.json to render all section
                and the features held in the feature array of each section. */}
            <div className="grid-container">
                {content.sections.map((section) => {
                    return (
                        <section className="usa-section margin-y-0 tablet:padding-top-2 tablet:padding-bottom-2">
                            <div className="grid-row grid-gap">
                                <Section section={section} />
                            </div>
                            <div className="grid-row grid-gap margin-bottom-2 ">
                                {section.features?.map((feature) => {
                                    return (
                                        <Feature
                                            section={section}
                                            feature={feature}
                                        />
                                    );
                                })}
                            </div>
                        </section>
                    );
                })}

                {/* INFO
                    this block of code, similar to the one above, maps through the freeSecure array in content.json to
                    render out all its contents. */}
                <section className="usa-section">
                    <div className="grid-row grid-gap  margin-bottom-4 padding-top-0">
                        {content.freeSecure.map((item) => {
                            return (
                                <>
                                    <div className="tablet:grid-col-6">
                                        <h3 className="font-sans-lg padding-top-3 border-top-05 border-base-lighter">
                                            <img
                                                src={"/assets/" + item.icon}
                                                alt="cdc logo"
                                                height="36"
                                                className="margin-right-2"
                                            />
                                            {item.title}
                                        </h3>
                                        <p className="usa-prose">
                                            {item.summary}
                                        </p>
                                    </div>
                                </>
                            );
                        })}
                    </div>
                </section>
            </div>
        </>
    );
};

export default Home;
