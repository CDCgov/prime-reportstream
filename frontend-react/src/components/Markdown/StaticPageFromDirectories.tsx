import { Route, Switch, useRouteMatch } from "react-router-dom";

import { CODES, ErrorPage } from "../../pages/error/ErrorPage";

import { MarkdownDirectory } from "./MarkdownDirectory";
import GeneratedSideNav from "./GeneratedSideNav";
import DirectoryAsPage from "./DirectoryAsPage";

const StaticPageFromDirectories = ({
    directories,
}: {
    directories: MarkdownDirectory[];
}) => {
    const { path } = useRouteMatch();

    return (
        <section className="grid-container tablet:margin-top-6 margin-bottom-5">
            <div className="grid-row grid-gap">
                <section className="tablet:grid-col-4 margin-bottom-6">
                    <GeneratedSideNav directories={directories} />
                </section>
                <section className="tablet:grid-col-8 usa-prose rs-documentation">
                    <Switch>
                        {/* SubRouter for /built-for-you */}
                        {directories.map((dir) => (
                            <Route
                                key={`${dir.slug}-route`}
                                path={`${path}/${dir.slug}`}
                                render={() => (
                                    <DirectoryAsPage
                                        key={`${dir.slug}-dir-as-page`}
                                        directory={dir}
                                    />
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

export default StaticPageFromDirectories;
