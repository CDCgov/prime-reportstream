import { Route, Switch, useRouteMatch } from "react-router-dom";

import { MarkdownDirectory } from "../../components/Markdown/MarkdownDirectory";
import { CODES, ErrorPage } from "../error/ErrorPage";
import GeneratedSideNav from "../../components/Markdown/GeneratedSideNav";
/* Markdown files must be imported as modules and passed along to the
 * MarkdownContent component through props. */
import may2022 from "../../content/built-for-you/2022-may.md";
import june2022 from "../../content/built-for-you/2022-june.md";
import DirectoryAsPage from "../../components/Markdown/DirectoryAsPage";

/* This controls the content for Built For You! To add a directory:
 *
 * 1. copy-paste an existing directory and add it in the array where
 * you want it to show up in the side-nav. Then, alter the title,
 * slug, and files array to match your desired title, url slug, and
 * to pass in any files you wish to render on the page. */
const DIRECTORIES = [
    new MarkdownDirectory("June 2022", "june-2022", [june2022]),
    new MarkdownDirectory("May 2022", "may-2022", [may2022]),
];

/* Houses the routing and layout for Built For You */
const BuiltForYouIndex = () => {
    let { path } = useRouteMatch();

    return (
        <section className="grid-container tablet:margin-top-6 margin-bottom-5">
            <div className="grid-row grid-gap">
                <section className="tablet:grid-col-4 margin-bottom-6">
                    <GeneratedSideNav directories={DIRECTORIES} />
                </section>
                <section className="tablet:grid-col-8 usa-prose rs-documentation">
                    <Switch>
                        {/* SubRouter for /built-for-you */}
                        {DIRECTORIES.map((dir) => (
                            <Route
                                key={dir.slug}
                                path={`${path}/${dir.slug}`}
                                render={() => (
                                    <DirectoryAsPage directory={dir} />
                                )}
                            />
                        ))}
                        {/* Handles any undefined route */}
                        <Route
                            render={() => (
                                <ErrorPage code={CODES.NOT_FOUND_404} />
                            )}
                        />
                    </Switch>
                </section>
            </div>
        </section>
    );
};

export default BuiltForYouIndex;
