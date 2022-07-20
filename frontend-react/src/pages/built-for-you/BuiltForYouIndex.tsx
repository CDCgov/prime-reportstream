import React from "react";

import { MarkdownDirectory } from "../../components/Content/MarkdownDirectory";
import StaticPagesFromDirectories from "../../components/Content/StaticPagesFromDirectories";
import may2022 from "../../content/built-for-you/2022-may.md";
import june2022 from "../../content/built-for-you/2022-june.md";
import DropdownNav from "../../components/header/DropdownNav";

/* This controls the content for Built For You
 *
 * visit: https://reportstream.cdc.gov/admin/guides/create-markdown-pages
 * for mor info on how to edit this directory */
export const BUILT_FOR_YOU: MarkdownDirectory[] = [
    new MarkdownDirectory("June 2022", "june-2022", [june2022]),
    new MarkdownDirectory("May 2022", "may-2022", [may2022]),
];

/* This generates the navigation item for Built For You
 * Import and use this in ReportStreamHeader */
export const BuiltForYouDropdown = () => {
    return (
        <DropdownNav
            label="Built For You"
            root="/built-for-you"
            directories={BUILT_FOR_YOU}
        />
    );
};

/* Creates the page and side-nav from your directory array */
const BuiltForYouIndex = () => (
    <StaticPagesFromDirectories directories={BUILT_FOR_YOU} />
);
export default BuiltForYouIndex;
