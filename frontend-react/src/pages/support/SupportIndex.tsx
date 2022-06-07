import React from "react";

import { MarkdownDirectory } from "../../components/Markdown/MarkdownDirectory";
import StaticPageFromDirectories from "../../components/Markdown/StaticPageFromDirectories";
import faq from "../../content/support/faq.md";
import contact from "../../content/support/contact.md";
import DropdownNav from "../../components/header/DropdownNav";

/* This controls the content for Support
 *
 * visit: https://reportstream.cdc.gov/admin/guides/create-markdown-pages
 * for mor info on how to edit this directory */
export const SUPPORT: MarkdownDirectory[] = [
    new MarkdownDirectory("Frequently asked questions", "faq", [faq]),
    new MarkdownDirectory("Contact", "contact", [contact]),
];

/* This generates the navigation item for Built For You
 * Import and use this in ReportStreamHeader */
export const SupportDropdown = () => {
    return (
        <DropdownNav label="Support" root="/support" directories={SUPPORT} />
    );
};

/* Creates the page and side-nav from your directory array */
const SupportIndex = () => <StaticPageFromDirectories directories={SUPPORT} />;
export default SupportIndex;
