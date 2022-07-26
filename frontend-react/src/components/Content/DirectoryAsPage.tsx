/* Renders all files on a page for a directory */
import { MarkdownDirectory } from "./MarkdownDirectory";
import { MarkdownContent } from "./MarkdownContent";

//TODO: Update so it can take all content directories, OR rename to markdown specific
const DirectoryAsPage = ({ directory }: { directory: MarkdownDirectory }) => {
    return (
        <div>
            {directory.files.map((file, idx) => (
                /* Because file != typeof string but this is what our spike showed us works. */
                // @ts-ignore
                <MarkdownContent key={idx} markdownUrl={file} />
            ))}
        </div>
    );
};

export default DirectoryAsPage;
