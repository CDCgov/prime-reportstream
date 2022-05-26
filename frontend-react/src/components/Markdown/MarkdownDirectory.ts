export interface MarkdownPageProps {
    directories: MarkdownDirectory[];
}

interface MarkdownDirectoryInit {
    title: string;
    slug: string;
    root: string;
    files: string[];
}

export class MarkdownDirectory {
    title: string;
    slug: string;
    root: string;
    files: string[];

    constructor({ title, slug, root, files }: MarkdownDirectoryInit) {
        this.title = title;
        this.slug = slug;
        this.root = root;
        this.files = files;
    }

    getUrl(item: string) {
        if (!this.files.includes(item)) return undefined;
        return `${this.root}/${item}`;
    }
}

export const MadeForYouDirectories = [
    new MarkdownDirectory({
        title: "March 2022",
        slug: "/built-for-you/march-2022",
        root: "/content/built-for-you/2022-march",
        files: ["update-2022-03-01.md, update-2022-03-15.md"],
    }),
];
