import { MarkdownDirectory } from "../../components/Markdown/MarkdownDirectory";
import StaticPageFromDirectories from "../../components/Markdown/StaticPageFromDirectories";
/* Markdown files must be imported as modules and passed along to the
 * MarkdownContent component through props. */
import may2022 from "../../content/built-for-you/2022-may.md";
import june2022 from "../../content/built-for-you/2022-june.md";

/* This controls the content for Built For You! To add a directory:
 *
 * 1. copy-paste an existing directory and add it in the array where
 * you want it to show up in the side-nav. Then, alter the title,
 * slug, and files array to match your desired title, url slug, and
 * to pass in any files you wish to render on the page. */
export const BUILT_FOR_YOU: MarkdownDirectory[] = [
    new MarkdownDirectory("June 2022", "june-2022", [june2022]),
    new MarkdownDirectory("May 2022", "may-2022", [may2022]),
];

/* Houses the routing and layout for Built For You */
const BuiltForYouIndex = () => (
    <StaticPageFromDirectories directories={BUILT_FOR_YOU} />
);
export default BuiltForYouIndex;
