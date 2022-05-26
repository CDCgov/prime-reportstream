import { MarkdownPageProps } from "../../components/Markdown/MarkdownDirectory";
import { MarkdownContent } from "../../components/Markdown/MarkdownContent";

const MadeForYou = ({ directories }: MarkdownPageProps) => {
    return directories.forEach((dir) => {
        dir.files.map((file) => (
            <MarkdownContent markdownUrl={dir.getUrl(file)} />
        ));
    });
};
