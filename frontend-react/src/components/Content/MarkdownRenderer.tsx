import React, { useEffect, useState } from "react";
import rehypeRaw from "rehype-raw";
import ReactMarkdown, { Options } from "react-markdown";
import rehypeSlug from "rehype-slug";
import remarkGfm from "remark-gfm";
import remarkToc from "remark-toc";

const baseOptions: Partial<Options> = {
    remarkPlugins: [
        // Use GitHub-flavored markdown
        remarkGfm,
        // Generate a table of contents
        [remarkToc, { tight: true }],
    ],
    rehypePlugins: [
        // Add ids to headings so the table of contents can link to each section
        rehypeSlug,
        rehypeRaw,
    ],
};

type MarkdownContentProps = {
    // Relative URL of the webpack-bundled markdown file. This value can be determined by importing
    // the file as long as webpack is not configured to load the contents of the file, which is the
    // state that our version of Create React App is in.
    markdownUrl: string;
};

export const MarkdownRenderer: React.FC<MarkdownContentProps> = ({
    markdownUrl,
}) => {
    const [markdownContent, setMarkdownContent] = useState("");

    // Fetch the contents of the markdown file.
    // See: https://stackoverflow.com/questions/65395125/how-to-load-an-md-file-on-build-when-using-create-react-app-and-typescript
    useEffect(() => {
        fetch(markdownUrl)
            .then((response) => response.text())
            .then((text) => {
                setMarkdownContent(text);
            });
    }, [markdownUrl]);

    return <ReactMarkdown {...baseOptions} children={markdownContent} />;
};
