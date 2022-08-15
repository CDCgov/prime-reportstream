import * as module from "module";

import { Route } from "react-router-dom";

import DirectoryAsPage from "./DirectoryAsPage";

type ContentElement = () => JSX.Element;

/** A base type that holds directory information
 *
 * @property title
 * @property slug
 * @property desc */
export abstract class ContentDirectory {
    title: string = "";
    slug: string = "";
    desc: string = "";
    setTitle(title: string) {
        this.title = title;
        return this;
    }
    setSlug(slug: string) {
        this.slug = slug;
        return this;
    }
    setDescription(description: string) {
        this.desc = description;
        return this;
    }
}
/** Creates a backwards-compatible method of rendering old React elements
 * as pages until converted to markdown
 *
 * @property title
 * @property slug
 * @property desc
 * @property element - Element to render
 */
export class ElementDirectory extends ContentDirectory {
    element: ContentElement = () => <></>; //Empty element default
    addElement(element: ContentElement) {
        this.element = element;
        return this;
    }
}
/** Used to create objects that hold pointers to markdown directories and the
 * info needed to query them. This is because we cannot access the filesystem
 * at runtime
 *
 * @property title
 * @property slug
 * @property desc
 * @property files - markdown files to render */
export class MarkdownDirectory extends ContentDirectory {
    files: module[] = []; //Empty module array default
    addFile(file: module) {
        this.files.push(file);
        return this;
    }
    addAllFiles(files: module[]) {
        this.files = files;
        return this;
    }
}
/** Takes a `ContentDirectory` and returns a react-router `Route` */
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
/** Takes a `ContentDirectory[]` and generates a React Fragment containing
 * each directory's `GeneratedRoute` */
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
