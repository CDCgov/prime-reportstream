import { NavLink, Route, Switch } from "react-router-dom";
import { SideNav } from "@trussworks/react-uswds";

import {
    MadeForYouDirectories,
    MarkdownPageProps,
} from "../../components/Markdown/MarkdownDirectory";
import { CODES, ErrorPage } from "../error/ErrorPage";

/* For Tuesday:
 * TODO: Generate dropdown nav item and subnav menu
 * TODO: layout page to match UI mocks
 * TODO: hide behind feature flag and ship with sample content */

const GeneratedSideNav = () => {
    const navItems = MadeForYouDirectories.map((dir) => (
        <NavLink
            to={dir.slug}
            activeClassName="usa-current"
            className="usa-nav__link"
        >
            {dir.title}
        </NavLink>
    ));
    return <SideNav items={navItems} />;
};

const BuiltForYouIndex = ({ directories }: MarkdownPageProps) => {
    // directories.forEach((dir) => {
    //     dir.files.map((fileName) => {
    //         const contentURL = dir.getUrl(fileName);
    //         if (contentURL !== undefined) {
    //             return <MarkdownContent markdownUrl={contentURL} />;
    //         } else {
    //             return null;
    //         }
    //     });
    // });
    return (
        <section className="grid-container tablet:margin-top-6 margin-bottom-5">
            <div className="grid-row grid-gap">
                <section className="tablet:grid-col-4 margin-bottom-6">
                    <GeneratedSideNav />
                </section>
                <section className="tablet:grid-col-8 usa-prose rs-documentation">
                    <Switch>
                        {/* SubRouter for /built-for-you */}

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
