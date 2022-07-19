import * as module from "module";

import { Route } from "react-router-dom";

import DirectoryAsPage from "./DirectoryAsPage";

/* Used to instantiate a set of static pages, like BuiltForYouIndex
 * or HowItWorks */

type ContentElement = () => JSX.Element;

export interface MarkdownPageProps {
    directories: MarkdownDirectory[];
}

export abstract class ContentDirectory {
    title: string = "";
    slug: string = "";
    desc: string = "";
    protected constructor(title: string, slug: string, desc: string) {
        this.title = title;
        this.slug = slug;
        this.desc = desc;
    }
}

/** Creates a directory (page) consisting of one ore many elements to render in order
 *
 * @param title {string} Value displayed in GeneratedSideNav
 * @param slug {string} the url slug to navigate to
 * @param element {ContentElement[]} one or more elemnets to render on a given page
 */
export class ElementDirectory extends ContentDirectory {
    element: ContentElement;
    constructor(
        title: string = "",
        slug: string = "",
        desc: string = "",
        element: ContentElement
    ) {
        super(title, slug, desc);
        this.element = element;
    }
}

/* Used to create objects that hold pointers to markdown directories and the
 * info needed to query them. This is because we cannot access the filesystem
 * at runtime */
export class MarkdownDirectory extends ContentDirectory {
    files: module[];
    constructor(title: string, slug: string, files: module[], desc?: string) {
        super(title, slug, desc || "");
        this.files = files;
    }
}

export const GeneratedRoute = ({ dir }: { dir: ContentDirectory }) => {
    if (dir instanceof MarkdownDirectory) {
        return (
            <Route
                key={`${dir.slug}-route`}
                path={`${dir.slug}`}
                render={() => (
                    <DirectoryAsPage
                        key={`${dir.slug}-dir-as-page`}
                        directory={dir}
                    />
                )}
            />
        );
    } else {
        const castDir = dir as ElementDirectory;
        return (
            <Route
                key={`${castDir.slug}-route`}
                path={`${castDir.slug}`}
                render={castDir.element}
            />
        );
    }
};

export const GeneratedRouter = ({
    directories,
}: {
    directories: ContentDirectory[];
}) => {
    return (
        <>
            {directories.map((dir, idx) => (
                <GeneratedRoute dir={dir} key={idx} />
            ))}
            {/* Handles any undefined route */}
        </>
    );
};
