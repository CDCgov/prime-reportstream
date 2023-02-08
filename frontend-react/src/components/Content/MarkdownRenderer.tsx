import React, { useEffect, useState } from "react";
import rehypeRaw from "rehype-raw";
import ReactMarkdown, { Options } from "react-markdown";
import rehypeSlug from "rehype-slug";
import remarkGfm from "remark-gfm";
import remarkToc from "remark-toc";

import { USExtLink, USLink } from "../USLink";

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

type ReactMarkdownComponentsProp = Exclude<Options["components"], undefined>;
/**
 * Helper type for typing MarkdownRenderer components.
 * Ex: ReactMarkdownComponentProps<"a"> for custom component for anchor elements.
 * See: https://github.com/remarkjs/react-markdown#appendix-b-components
 */
export type ReactMarkdownComponentProps<
    T extends string & keyof ReactMarkdownComponentsProp
> = Extract<ReactMarkdownComponentsProp[T], (...args: any) => any> extends never
    ? never
    : Parameters<
          Extract<ReactMarkdownComponentsProp[T], (...args: any) => any>
      >[0];

// Matches relative or cdc.gov absolute urls
const INTERNAL_LINK_REGEX = /^(\/\w*?|(https:\/\/)?\w*?\.cdc\.gov)\/?.*$/;

const ReactMarkdownLink = ({
    children,
    ...props
}: ReactMarkdownComponentProps<"a">) => {
    if (!INTERNAL_LINK_REGEX.test(props.href ?? "")) {
        return <USExtLink {...props}>{children}</USExtLink>;
    }
    return <USLink {...props}>{children}</USLink>;
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

    return (
        <ReactMarkdown
            {...baseOptions}
            children={markdownContent}
            components={{
                a: ReactMarkdownLink,
            }}
        />
    );
};
