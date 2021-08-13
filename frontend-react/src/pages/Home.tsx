import content from "../content/content.json";
import site from "../content/site.json";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { IconName } from "@fortawesome/fontawesome-svg-core";
import { library } from "@fortawesome/fontawesome-svg-core";
import { fas } from "@fortawesome/free-solid-svg-icons";
import CdcMap from "@cdc/map";
import live from "../content/live.json";
import { Link } from "react-router-dom";

const Hero = () => {
    return (
        <section className="rs-hero">
            <div className="grid-container usa-section margin-bottom-10">
                <div className="grid-row grid-gap margin-bottom-0 rs-hero__flex-center text-center">
                    <div className="tablet:grid-col-10 tablet:grid-offset-1">
                        <h1 className="tablet:font-sans-3xl">
                            {content.title}
                        </h1>
                    </div>
                    <div className="tablet:grid-col-8 tablet:grid-offset-2">
                        <p className="font-sans-lg line-height-sans-4">
                            {content.summary}
                        </p>
                    </div>
                </div>
            </div>
        </section>
    );
};

export const Home = () => {
    library.add(fas);
    return (
        <>
            <Hero />
            <div className="grid-container">
                {content.sections.map((section, idx) => (
                    <section className="usa-section margin-y-0 padding-y-0">
                        <div className="grid-row grid-gap margin-bottom-0">
                            <div className="tablet:grid-col-12">
                                <h2 className="font-sans-xl margin-top-0 tablet:margin-bottom-0">
                                    {section.title}
                                </h2>
                                <p
                                    className="font-sans-lg margin-top-1 text-base"
                                    dangerouslySetInnerHTML={{
                                        __html: section.summary,
                                    }}
                                ></p>
                            </div>
                        </div>
                        <div className="grid-row grid-gap margin-bottom-8">
                            {section.features.map((feature) => {
                                return (
                                    <div className="tablet:grid-col-4 margin-bottom-0">
                                        <h3>
                                            <FontAwesomeIcon
                                                color="#005EA2"
                                                icon={feature.icon as IconName}
                                                className="margin-right-1"
                                            />
                                            {feature.title}
                                        </h3>
                                        <p
                                            className="usa-prose"
                                            dangerouslySetInnerHTML={{
                                                __html: feature.summary,
                                            }}
                                        ></p>
                                    </div>
                                );
                            })}
                        </div>
                    </section>
                ))}
            </div>

            <div className="grid-container">
                <section className="usa-section margin-top-0 padding-top-0 padding-bottom-6 border-bottom-1px border-base-lighter">
                    <div className="grid-row grid-gap  margin-bottom-2 ">
                        <div className="tablet:grid-col-12">
                            <h2 className="font-sans-xl margin-top-0 tablet:margin-bottom-0">
                                Where is ReportStream live?
                            </h2>
                            <p className="font-sans-lg margin-top-1 margin-bottom-6">
                                ReportStream partners with health departments,
                                test manufacturers, data aggregators, and others
                                across the U.S.
                            </p>
                            <CdcMap config={live} />
                            <p className="usa-prose margin-top-2">
                                We’re growing quickly. Take a look at the{" "}
                                <Link to="/how-it-works/where-were-live">
                                    complete list of ReportStream partners
                                </Link>
                                .
                            </p>
                        </div>
                    </div>
                </section>
            </div>

            <div className="grid-container margin-bottom-15">
                <section className="usa-section margin-top-0 padding-top-0 padding-bottom-6 border-bottom-1px border-base-lighter">
                    <div className="grid-row grid-gap  margin-bottom-2 ">
                        <div className="tablet:grid-col-12">
                            <h2 className="font-sans-xl margin-top-0 tablet:margin-bottom-0">
                                {content.cta.title}
                            </h2>
                            <p
                                className="font-sans-lg margin-top-1 margin-bottom-6"
                                dangerouslySetInnerHTML={{
                                    __html: content.cta.summary,
                                }}
                            ></p>
                            <a
                                className="usa-button"
                                href={"mailto:" + site.orgs.RS.email}
                            >
                                {content.cta.buttonText}
                            </a>
                        </div>
                    </div>
                </section>
            </div>
        </>
    );
};

export default Home;
