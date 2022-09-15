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
        .setSlug(SupportDirectoryTools.getSlug(SupportTitles.CONTACT))
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
        .setSlug(SupportDirectoryTools.getSlug(SupportTitles.FAQ))
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

/* HOW TO CREATE SECTIONS
 *
 * 1. Use the `enum` class to make an array of cards for each section.
 *
 * 2. Create a section from the array using makeSectionFromTitles() and passing in
 *    your titles, followed by the array of all directories.
 *
 * 3. Use a ContentMap and set your sections using .set(), passing in the section name
 *    as a string, and the section you made in step 2 as your second parameter.
 * */
