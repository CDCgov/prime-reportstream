import { Helmet } from "react-helmet";
import { Redirect, Switch } from "react-router-dom";
import React from "react";

import {
    ContentDirectory,
    MarkdownDirectory,
    ElementDirectory,
} from "../../components/Content/MarkdownDirectory";
import ProductIndexMd from "../../content/product/product-index.md";
import ReleaseNotesMd from "../../content/product/release-notes.md";
import StaticPagesFromDirectories from "../../components/Content/StaticPagesFromDirectories";
import {
    ContentDirectoryTools,
    SlugParams,
} from "../../components/Content/PageGenerationTools";

import { WhereWereLiveIa } from "./WhereWereLive";

/* Data that drives breadcrumb creation and slug appending */
const slugs: SlugParams[] = [
    { key: "OVERVIEW", slug: "overview" },
    { key: "WHERE_WERE_LIVE", slug: "where-were-live" },
    { key: "RELEASE_NOTES", slug: "release-notes" },
];

/* Tools to help generate Directories */
const DirectoryTools = new ContentDirectoryTools()
    .setName("Product")
    .setRoot("/product")
    .setSlugs(slugs);

const directories: ContentDirectory[] = [
    new MarkdownDirectory(
        "Overview",
        DirectoryTools.prependRoot("OVERVIEW"),
        [ProductIndexMd],
        ""
    ),
    new ElementDirectory(
        "Where we're live",
        DirectoryTools.prependRoot("WHERE_WERE_LIVE"),
        "",
        WhereWereLiveIa
    ),
    new MarkdownDirectory(
        "Release notes",
        DirectoryTools.prependRoot("RELEASE_NOTES"),
        [ReleaseNotesMd],
        ""
    ),
];

export const Product = () => {
    return (
        <>
            <Helmet>
                <title>Product | {process.env.REACT_APP_TITLE}</title>
            </Helmet>
            <div className="rs-hero__index">
                <div className="grid-container">
                    <p id="product-heading-description" className="heading-alt">
                        Product
                        <br />
                        <span>How ReportStream works</span>
                    </p>
                </div>
            </div>
            <Switch>
                {/* Workaround to allow links to /product to work */}
                <Redirect from={"/product"} to={"/product/overview"} />
            </Switch>
            <div className="rs-prose">
                <StaticPagesFromDirectories directories={directories} />
            </div>
        </>
    );
};
