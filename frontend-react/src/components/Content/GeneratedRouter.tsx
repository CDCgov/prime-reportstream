import { Route } from "react-router-dom";

import DirectoryAsPage from "./DirectoryAsPage";
import {
    ContentDirectory,
    ElementDirectory,
    MarkdownDirectory,
} from "./MarkdownDirectory";

/** Takes a `ContentDirectory` and returns a react-router `Route` */
export const GeneratedRoute = ({ dir }: { dir: ContentDirectory }) => {
    if (dir instanceof MarkdownDirectory) {
        return (
            <Route
                key={`${dir.slug}-route`}
                path={`${dir.slug}`}
                element={
                    <DirectoryAsPage
                        key={`${dir.slug}-dir-as-page`}
                        directory={dir}
                    />
                }
            />
        );
    } else {
        const castDir = dir as ElementDirectory;
        return (
            <Route
                key={`${castDir.slug}-route`}
                path={`${castDir.slug}`}
                element={castDir.element}
            />
        );
    }
};
/** Takes a `ContentDirectory[]` and generates a React Fragment containing
 * each directory's `GeneratedRoute` */
export const GeneratedRouter = ({
    directories,
}: {
    directories: ContentDirectory[];
}) => {
    return (
        <>
            {directories.map((dir, idx) => (
                <GeneratedRoute dir={dir} key={idx} />
            ))}
            {/* Handles any undefined route */}
        </>
    );
};
