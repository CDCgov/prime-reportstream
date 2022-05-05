import { useEffect, useState } from "react";
import ReactMarkdown from "react-markdown";
import rehypeSlug from "rehype-slug";
import remarkGfm from "remark-gfm";
import remarkToc from "remark-toc";

import programmersGuidePath from "../../../content/programmers-guide.md";

export const ProgrammersGuide = () => {
    const [markdownContent, setMarkdownContent] = useState("");

    // Approach from: https://stackoverflow.com/questions/65395125/how-to-load-an-md-file-on-build-when-using-create-react-app-and-typescript
    // TODO(mreifman): Explore whether it makes sense to eject from create-react-app and modify the
    // webpack config
    useEffect(() => {
        fetch(programmersGuidePath)
            .then((response) => response.text())
            .then((text) => {
                setMarkdownContent(text);
            });
    }, []);

    // console.log(md)
    return (
        <ReactMarkdown
            children={markdownContent}
            remarkPlugins={[
                remarkGfm,
                [remarkToc, { heading: "contents", tight: true }],
            ]}
            rehypePlugins={[rehypeSlug]}
        />
    );
};
