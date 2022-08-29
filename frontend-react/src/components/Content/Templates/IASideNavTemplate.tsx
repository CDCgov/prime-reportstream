import { Route, Routes } from "react-router-dom";

import { ContentDirectory, getDirectoryElement } from "../MarkdownDirectory";
import GeneratedSideNav from "../GeneratedSideNav";

export interface IASideNavProps {
    directories: ContentDirectory[];
}

const IASideNavTemplate = ({ directories }: IASideNavProps) => {
    return (
        <section className="grid-container tablet:margin-top-6 margin-bottom-5">
            <div className="grid-row grid-gap">
                <section className="tablet:grid-col-4 margin-bottom-6">
                    <GeneratedSideNav directories={directories} />
                </section>
                <section className="tablet:grid-col-8 usa-prose rs-documentation">
                    <Routes>
                        {directories.map((dir, idx) => (
                            <Route
                                key={idx}
                                path={dir.slug}
                                element={getDirectoryElement(dir)}
                            />
                        ))}
                    </Routes>
                </section>
            </div>
        </section>
    );
};

export default IASideNavTemplate;
