import { Helmet } from "react-helmet";
import { Route, Switch } from "react-router-dom";

import {
    ContentDirectory,
    ElementDirectory,
    GeneratedRouter,
} from "../../components/Content/MarkdownDirectory";
import { IACardList } from "../../components/IACard";
import {
    contentContainer,
    ContentDirectoryTools,
    SlugParams,
} from "../../components/Content/PageGenerationTools";

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
    new ElementDirectory()
        .setTitle("Contact")
        .setSlug(DirectoryTools.prependRoot("CONTACT"))
        .setDescription(
            "Questions, issues, or a bug to report? We're happy to help!"
        )
        .addElement(
            contentContainer(<Contact />, DirectoryTools.makeCrumb("Contact"))
        ),
    new ElementDirectory()
        .setTitle("Frequently asked questions")
        .setSlug(DirectoryTools.prependRoot("FAQ"))
        .setDescription(
            "Answers to common questions about working with ReportStream."
        )
        .addElement(
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
                    <h2>Have questions? Here are a few ways we can help</h2>
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
