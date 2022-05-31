/* Renders all files on a page for a directory */
import { MarkdownDirectory } from "./MarkdownDirectory";
import { MarkdownContent } from "./MarkdownContent";

const DirectoryAsPage = ({ directory }: { directory: MarkdownDirectory }) => {
    return (
        <>
            {directory.files.map((file) => (
                /* Because file != typeof string but this is what our spike showed us works. */
                // @ts-ignore
                <MarkdownContent markdownUrl={file} />
            ))}
        </>
    );
};

export default DirectoryAsPage;
