import {
    ContentDirectory,
    ElementDirectory,
} from "../../components/Content/MarkdownDirectory";
import {
    contentContainer,
    ContentDirectoryTools,
    SlugParams,
} from "../../components/Content/PageGenerationTools";
import {
    IACardGridProps,
    IACardGridTemplate,
} from "../../components/Content/Templates/IACardGridTemplate";
import { IARoot, IARootProps } from "../../components/Content/Templates/IARoot";

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

const PAGE_NAME = "Support";
const pageProps: IACardGridProps = {
    title: PAGE_NAME,
    subtitle: "Have questions? Here are a few ways we can help",
    directoriesToRender: directories,
};
/** This is our main page content */
export const SupportCardGrid = () => <IACardGridTemplate {...pageProps} />;

const rootProps: IARootProps = {
    path: "/support",
    pageName: PAGE_NAME,
    indexComponent: SupportCardGrid,
    directoriesToRoute: directories,
};
/** Use this component in the main App Router! It will handle rendering everything
 * and set the Helmet values */
export const Support = () => <IARoot {...rootProps} />;
