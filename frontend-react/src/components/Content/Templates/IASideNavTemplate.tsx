import { Navigate, Route, Routes } from "react-router-dom";

import { getDirectoryElement } from "../MarkdownDirectory";

import GeneratedSideNav from "./GeneratedSideNav";
import { IAComponentProps } from "./IAComponentProps";

export interface IASideNavProps extends IAComponentProps {
    rootRedirect?: string;
}

const IASideNavTemplate = ({ directories, rootRedirect }: IASideNavProps) => {
    const dirArr =
        directories instanceof Map
            ? Array.from(new Set(Array.from(directories.values()).flat()))
            : directories;
    return (
        <section className="grid-container tablet:margin-top-6 margin-bottom-5">
            <div className="grid-row grid-gap">
                <section className="tablet:grid-col-4 margin-bottom-6">
                    <GeneratedSideNav directories={dirArr} />
                </section>
                <section className="tablet:grid-col-8 usa-prose rs-documentation">
                    <Routes>
                        <Route
                            path={"/"}
                            element={
                                <Navigate to={rootRedirect || ""} replace />
                            }
                        />
                        {dirArr.map((dir, idx) => (
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
