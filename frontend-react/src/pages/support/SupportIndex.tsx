import { Helmet } from "react-helmet";
import { Route, Switch } from "react-router-dom";

import {
    ContentDirectory,
    ElementDirectory,
    GeneratedRouter,
} from "../../components/Markdown/MarkdownDirectory";
import { IACardList } from "../../components/IACard";
import {
    contentContainer,
    ContentDirectoryTools,
    SlugParams,
} from "../../components/Markdown/DirectoryGenerationTools";

import { Contact } from "./Contact";
import { Faq } from "./Faq";

/* Data that drives breadcrumb creation and slug appending */
const slugs: SlugParams[] = [
    { key: "CONTACT", slug: "contact" },
    { key: "FAQ", slug: "faq" },
];

/* Tools to help generate Directories */
const DirectoryTools = new ContentDirectoryTools()
    .setName("Support")
    .setRoot("/support")
    .setSlugs(slugs);

/* An array of directories to be rendered */
const directories: ContentDirectory[] = [
    new ElementDirectory(
        "Contact",
        DirectoryTools.prependRoot("CONTACT"),
        "Want to get in touch with ReportStream? Email us at reportstream@cdc.gov.",
        contentContainer(<Contact />, DirectoryTools.makeCrumb("Contact"))
    ),
    new ElementDirectory(
        "Frequently asked questions",
        DirectoryTools.prependRoot("FAQ"),
        "Answers to common questions about working with ReportStream.",
        contentContainer(
            <Faq />,
            DirectoryTools.makeCrumb("Frequently asked questions")
        )
    ),
];

export const Support = () => {
    const path = "/support";
    return (
        <>
            <Helmet>
                <title>Support | {process.env.REACT_APP_TITLE}</title>
            </Helmet>

            <Switch>
                {/* Handles anyone going to /getting-started without extension */}
                <Route exact path={path} component={SupportIndex} />
                <GeneratedRouter directories={directories} />
            </Switch>
        </>
    );
};

export const SupportIndex = () => {
    return (
        <>
            <div className="rs-hero__index">
                <div className="grid-container">
                    <h1>Support</h1>
                    <h2>Have questions? Here are a few ways we can help.</h2>
                </div>
            </div>
            <div className="grid-container usa-prose margin-top-6">
                <div className="grid-row grid-gap">
                    <section>
                        <IACardList dirs={directories} />
                    </section>
                </div>
            </div>
        </>
    );
};
