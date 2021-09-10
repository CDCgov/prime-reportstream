import content from "../../content/content.json";
import { library } from "@fortawesome/fontawesome-svg-core";
import { fas } from "@fortawesome/free-solid-svg-icons";
import Hero from './Hero'
import Section from "./Sections/Section";
import Feature from "./Features/Feature";

export const Home = () => {
    library.add(fas);
    return (
        <>
            <Hero />
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
