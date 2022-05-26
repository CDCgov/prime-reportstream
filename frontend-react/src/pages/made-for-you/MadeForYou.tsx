import { MarkdownPageProps } from "../../components/Markdown/MarkdownDirectory";
import { MarkdownContent } from "../../components/Markdown/MarkdownContent";

const MadeForYou = ({ directories }: MarkdownPageProps) => {
    return directories.forEach((dir) => {
        dir.files.map((fileName) => {
            const contentURL = dir.getUrl(fileName);
            if (contentURL !== undefined) {
                return <MarkdownContent markdownUrl={contentURL} />;
            } else {
                return null;
            }
        });
    });
};

export default MadeForYou;
