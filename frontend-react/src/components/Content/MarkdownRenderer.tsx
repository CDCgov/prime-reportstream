import { FC, useEffect, useState } from "react";
import rehypeRaw from "rehype-raw";
import ReactMarkdown, { Options } from "react-markdown";
import rehypeSlug from "rehype-slug";

import { USSmartLink } from "../USLink";

type MarkdownContentProps = {
    // Relative URL of the webpack-bundled markdown file. This value can be determined by importing
    // the file as long as webpack is not configured to load the contents of the file, which is the
    // state that our version of Create React App is in.
    markdownUrl: string;
};

export const MarkdownRenderer: FC<MarkdownContentProps> = ({ markdownUrl }) => {
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
            rehypePlugins={
                [rehypeSlug, rehypeRaw] as unknown as Options["rehypePlugins"]
            }
            components={{
                a: USSmartLink,
            }}
        >
            {markdownContent}
        </ReactMarkdown>
    );
};
