/* Used to instantiate a set of static pages, like BuiltForYouIndex
 * or HowItWorks */
import * as module from "module";

export interface MarkdownPageProps {
    directories: MarkdownDirectory[];
}

/* Used to create objects that hold pointers to markdown directories and the
 * info needed to query them. This is because we cannot access the filesystem
 * at runtime */
export class MarkdownDirectory {
    title: string;
    slug: string;
    files: module[];

    constructor(title: string, slug: string, files: module[]) {
        this.title = title;
        this.slug = slug;
        this.files = files;
    }
}
