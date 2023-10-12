import React, { useEffect, useState } from "react";
import rehypeRaw from "rehype-raw";
import ReactMarkdown from "react-markdown";
import rehypeSlug from "rehype-slug";
import { PluggableList } from "react-markdown/lib";

import { USSmartLink } from "../USLink";

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

    return (
        <ReactMarkdown
            rehypePlugins={[rehypeSlug, rehypeRaw] as PluggableList}
            children={markdownContent}
            components={{
                a: USSmartLink,
            }}
        />
    );
};
