import useMarkdownDirectory, {
    UseMarkdownInit,
} from "../../hooks/UseMarkdownDirectory";

interface MarkdownPageProps extends UseMarkdownInit {}

const MarkdownPage = ({ fromDir, files }: MarkdownPageProps) => {
    const markdownDirectory = useMarkdownDirectory({ fromDir, files });
    return <div>{markdownDirectory.mdFiles.length}</div>;
};

export default MarkdownPage;
