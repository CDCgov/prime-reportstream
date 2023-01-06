/* Uniformly supplying resourcesCrumb */

import { CrumbConfig, WithCrumbs } from "../Crumbs";

import { ContentDirectory } from "./MarkdownDirectory";

export const contentContainer = (
    content: () => JSX.Element,
    crumbs: CrumbConfig[]
) => {
    const wrappedElement = (
        <div className="grid-container rs-documentation usa-prose">
            {content()}
        </div>
    );
    return renderWithCrumbs(wrappedElement, crumbs);
};

/** Generates crumbs array to make crumbs from `directory` back from `pageLabel`
 * @param directory {string} Directory name you're navigating back to (First Crumb)
 * @param dirPath {string} Path to `directory`
 * @param pageLabel {string} Label of page navigating back from */
export const crumbsFromHere = (
    directory: string,
    dirPath: string,
    pageLabel: string
): CrumbConfig[] => [{ label: directory, path: dirPath }, { label: pageLabel }];

/** Renders a given element with breadcrumbs
 * @todo make private method of ContentDirectoryTools
 * @param element {JSX.Element} The element to be rendered
 * @param crumbs {CrumbConfig[]} A list of crumbs to render */
export const renderWithCrumbs =
    (element: JSX.Element, crumbs: CrumbConfig[]) => () =>
        <WithCrumbs page={element} crumbList={crumbs} />;

export interface SlugParams {
    key: string;
    slug: string;
}
export class ContentDirectoryTools {
    title: string = "";
    subtitle: string = "";
    slugs: Map<string, string> = new Map();
    root: string = "";
    setTitle(name: string) {
        this.title = name;
        return this;
    }
    setSubtitle(subtitle: string) {
        this.subtitle = subtitle;
        return this;
    }
    setRoot(root: string) {
        this.root = root;
        return this;
    }
    setSlugs(slugs: SlugParams[]) {
        slugs.forEach((config) => this.slugs.set(config.key, config.slug));
        return this;
    }
    getSlug(slugMapKey: string) {
        const slug = this.slugs.get(slugMapKey);
        if (slug === undefined)
            throw Error(
                `Slug not found in ${this.title} directory tools slug map: ${slug}`
            );
        return slug;
    }
    makeCrumb(nextPage: string) {
        return crumbsFromHere(this.title, this.root, nextPage);
    }
}

export const makeSectionFromTitles = (
    titles: string[],
    directories: ContentDirectory[]
) => directories.filter((dir) => titles.includes(dir.title));
