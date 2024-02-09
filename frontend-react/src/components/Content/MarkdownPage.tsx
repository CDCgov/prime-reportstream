/* Renders all files on a page for a directory */
import { MarkdownDirectory } from "./MarkdownDirectory";
import { MarkdownRenderer } from "./MarkdownRenderer";

//TODO: Update so it can take all content directories, OR rename to markdown specific
const MarkdownPage = ({
    directory,
    ...props
}: React.PropsWithChildren<{ directory: MarkdownDirectory }>) => {
    return (
        <div {...props}>
            {directory.files.map((file, idx) => (
                <MarkdownRenderer key={idx} markdownUrl={file} />
            ))}
        </div>
    );
};

export default MarkdownPage;
