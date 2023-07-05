/* Data that drives breadcrumb creation and slug appending */
import {
    ContentDirectoryTools,
    SlugParams,
} from "../../components/Content/PageGenerationTools";
import {
    ContentDirectory,
    ElementDirectory,
    MarkdownDirectory,
} from "../../components/Content/MarkdownDirectory";
import { WhereWereLive } from "../../pages/product/legacy-page-content/WhereWereLive";
import { About } from "../../pages/About";

import ProductIndexMd from "./product-index.md";
import ReleaseNotesMd from "./release-notes.md";

const slugs: SlugParams[] = [
    { key: "OVERVIEW", slug: "overview" },
    { key: "WHERE_WERE_LIVE", slug: "where-were-live" },
    { key: "RELEASE_NOTES", slug: "release-notes" },
    { key: "ABOUT", slug: "about" },
];

/* Tools to help generate Directories */
export const ProductDirectoryTools = new ContentDirectoryTools()
    .setTitle("Product")
    .setSubtitle("Explore ReportStream and how it works")
    .setRoot("/product")
    .setSlugs(slugs);

export const productDirectories: ContentDirectory[] = [
    new MarkdownDirectory()
        .setTitle("Overview")
        .setSlug(ProductDirectoryTools.getSlug("OVERVIEW"))
        .addFile(ProductIndexMd),
    new ElementDirectory()
        .setTitle("Where we're live")
        .setSlug(ProductDirectoryTools.getSlug("WHERE_WERE_LIVE"))
        .addElement(WhereWereLive),
    new MarkdownDirectory()
        .setTitle("Release notes")
        .setSlug(ProductDirectoryTools.getSlug("RELEASE_NOTES"))
        .addFile(ReleaseNotesMd),
    new ElementDirectory()
        .setTitle("About")
        .setSlug(ProductDirectoryTools.getSlug("ABOUT"))
        .addElement(About),
];
