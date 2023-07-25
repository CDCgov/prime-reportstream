import MarkdownPage from "./MarkdownPage";

type ContentElement = () => JSX.Element;

/** A base type that holds directory information
 *
 * @property title
 * @property slug
 * @property desc */
export abstract class ContentDirectory {
    title: string = "";
    root: string = "";
    slug: string = "";
    desc: string = "";
    setTitle(title: string) {
        this.title = title;
        return this;
    }
    setRoot(root: string) {
        this.root = root;
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
    files: string[] = []; //Empty module array default
    addFile(file: string) {
        this.files.push(file);
        return this;
    }
    addAllFiles(files: string[]) {
        this.files = files;
        return this;
    }
}

export const getDirectoryElement = (dir: ContentDirectory) => {
    if (dir instanceof MarkdownDirectory) {
        return <MarkdownPage key={`${dir.slug}-dir-as-page`} directory={dir} />;
    } else if (dir instanceof ElementDirectory) {
        return dir.element();
    } else {
        const message = `${dir.title} is not a valid content directory type.`;
        console.error(message);
        throw Error(message);
    }
};
