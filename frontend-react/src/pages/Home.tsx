import content from "../content/content.json";
import site from "../content/site.json";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { IconName } from "@fortawesome/fontawesome-svg-core";
import { library } from "@fortawesome/fontawesome-svg-core";
import { fas } from "@fortawesome/free-solid-svg-icons";
import CdcMap from "@cdc/map";
import live from "../content/live.json";

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

interface SectionProp {
    title?: string;
    type?: string;
    summary?: string;
    bullets?: { content?: string }[];
    features?: FeatureProp[];
    description?: string;
    buttonText?: string;
    buttonUrlSubject?: string;
}

interface FeatureProp {
    method?: number;
    title?: string;
    icon?: string;
    img?: string;
    imgAlt?: string;
    summary?: string;
    items?: { title?: string; summary?: string }[];
}

const Feature = ({
    section,
    feature,
}: {
    section: SectionProp;
    feature: FeatureProp;
}) => {
    if (section.type === "deliveryMethods") {
        return <DeliveryMethodsFeature section={section} feature={feature} />;
    } else if (section.type === "liveMap") {
        return <LiveMapFeature section={section} feature={feature} />;
    } else
        return (
            <div className="tablet:grid-col-4 margin-bottom-0">
                <h3 className="font-sans-md tablet:font-sans-lg padding-top-3 border-top-05 border-base-lighter">
                    <FontAwesomeIcon
                        icon={feature.icon as IconName}
                        color="#005EA2"
                        className="margin-right-1"
                    />
                    {feature.title}
                </h3>
                <p
                    className="usa-prose"
                    dangerouslySetInnerHTML={{ __html: feature!.summary! }}
                ></p>
            </div>
        );
};

const DeliveryMethodsFeature = ({
    section,
    feature,
}: {
    section: SectionProp;
    feature: FeatureProp;
}) => {
    return (
        <div className="grid-col-12 margin-bottom-3">
            <div className="grid-row grid-gap display-flex flex-row flex-align-top">
                <div className="tablet:grid-col-6">
                    <img
                        src={site.imgPath + feature.img}
                        alt="{ feature.imgAlt }"
                    />
                </div>
                <div className="tablet:grid-col-6 ">
                    <h3 className="font-sans-lg margin-top-0 padding-top-3 margin-bottom-1 tablet:border-top-05 tablet:border-base-lighter">
                        <FontAwesomeIcon
                            icon={feature.icon as IconName}
                            color="#005EA2"
                            className="margin-right-1"
                        />
                        {feature.title}
                    </h3>
                    <p className="usa-prose">{feature!.items![0]?.summary}</p>
                    <p className="usa-prose">{feature!.items![1]?.summary}</p>
                </div>
            </div>
        </div>
    );
};

const LiveMapFeature = ({
    section,
    feature,
}: {
    section: SectionProp;
    feature: FeatureProp;
}) => {
    return (
        <>
            <div className="tablet:grid-col-4 margin-bottom-0">
                <h3 className="font-sans-md tablet:font-sans-lg padding-top-3 border-top-05 border-base-lighter">
                    {feature.title}
                </h3>
                <p className="usa-prose">{feature.summary}</p>
            </div>
        </>
    );
};

const Section = ({ section }: { section: SectionProp }) => {
    if (section.type === "cta") return <CtaSection section={section} />;
    else if (section.type === "liveMap")
        return <LiveMapSection section={section} />;
    else
        return (
            <div className="tablet:grid-col-10 ">
                <h2 className="font-sans-lg tablet:font-sans-xl margin-top-0 tablet:margin-bottom-0">
                    {section.title}
                </h2>
                <p className="usa-intro margin-top-1 text-base">
                    {section.summary}
                </p>
            </div>
        );
};

const CtaSection = ({ section }: { section: SectionProp }) => {
    return (
        <>
            <div className="tablet:grid-col-8">
                <h2 className="font-sans-lg tablet:font-sans-xl margin-top-0 tablet:margin-bottom-0">
                    {section.title}
                </h2>
                <p className="usa-prose">{section.description}</p>
                <p className="usa-prose">{section.summary}</p>
                <a
                    href={
                        "mailto:" +
                        site.orgs.RS.email +
                        "?subject=Getting started with ReportStream"
                    }
                    className="usa-button"
                >
                    Get in touch
                </a>
            </div>
        </>
    );
};

const LiveMapSection = ({ section }: { section: SectionProp }) => {
    return (
        <>
            <div>
                <h2 className="font-sans-lg tablet:font-sans-xl margin-top-0 tablet:margin-bottom-0">
                    {section.title}
                </h2>
                <p className="usa-intro margin-top-1 text-base">
                    {section.summary}
                </p>
                <div className="tablet:grid-col-10">
                    <CdcMap config={live} />
                </div>
                <p
                    className="usa-prose margin-top-2"
                    dangerouslySetInnerHTML={{ __html: section!.description! }}
                ></p>
            </div>
        </>
    );
};

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
