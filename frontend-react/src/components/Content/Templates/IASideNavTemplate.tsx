import { Navigate, Route, Routes } from "react-router-dom";

import { ContentDirectory, getDirectoryElement } from "../MarkdownDirectory";

import GeneratedSideNav from "./GeneratedSideNav";

export interface IASideNavProps {
    directories: ContentDirectory[];
    rootRedirect?: string;
}

const IASideNavTemplate = ({ directories, rootRedirect }: IASideNavProps) => {
    return (
        <section className="grid-container">
            <div className="grid-row grid-gap">
                <section className="tablet:grid-col-4 margin-bottom-6">
                    <GeneratedSideNav directories={directories} />
                </section>
                <section className="tablet:grid-col-8 usa-prose rs-documentation">
                    <Routes>
                        <Route
                            path={"/"}
                            element={
                                <Navigate to={rootRedirect || ""} replace />
                            }
                        />
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
