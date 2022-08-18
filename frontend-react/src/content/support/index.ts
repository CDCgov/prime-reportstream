/* Data that drives breadcrumb creation and slug appending */
import {
    contentContainer,
    ContentDirectoryTools,
    SlugParams,
} from "../../components/Content/PageGenerationTools";
import {
    ContentDirectory,
    ElementDirectory,
} from "../../components/Content/MarkdownDirectory";
import { Contact, Faq } from "../../pages/support/index-legacy";

enum SupportTitles {
    CONTACT = "Contact",
    FAQ = "Frequently asked questions",
}
const slugs: SlugParams[] = [
    { key: SupportTitles.CONTACT, slug: "contact" },
    { key: SupportTitles.FAQ, slug: "faq" },
];

/* Tools to help generate Directories */
export const SupportDirectoryTools = new ContentDirectoryTools()
    .setTitle("Support")
    .setSubtitle("Have questions? Here are a few ways we can help")
    .setRoot("/support")
    .setSlugs(slugs);

/* An array of directories to be rendered */
export const supportDirectories: ContentDirectory[] = [
    new ElementDirectory()
        .setTitle(SupportTitles.CONTACT)
        .setSlug(SupportDirectoryTools.prependRoot(SupportTitles.CONTACT))
        .setDescription(
            "Questions, issues, or a bug to report? We're happy to help!"
        )
        .addElement(
            contentContainer(
                Contact,
                SupportDirectoryTools.makeCrumb(SupportTitles.CONTACT)
            )
        ),
    new ElementDirectory()
        .setTitle(SupportTitles.FAQ)
        .setSlug(SupportDirectoryTools.prependRoot(SupportTitles.FAQ))
        .setDescription(
            "Answers to common questions about working with ReportStream."
        )
        .addElement(
            contentContainer(
                Faq,
                SupportDirectoryTools.makeCrumb(SupportTitles.FAQ)
            )
        ),
];
