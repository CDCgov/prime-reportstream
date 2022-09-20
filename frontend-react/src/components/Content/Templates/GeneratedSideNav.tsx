import { ReactNode } from "react";
import { NavLink } from "react-router-dom";
import { SideNav } from "@trussworks/react-uswds";

import { ContentDirectory } from "../MarkdownDirectory";

export const GeneratedSideNav = ({
    directories,
}: {
    directories: ContentDirectory[];
}) => {
    const navItems: ReactNode[] = directories.map((dir) => {
        return (
            <NavLink
                to={dir.slug}
                className={({ isActive }) =>
                    isActive ? "usa-current" : "usa-nav__link"
                }
            >
                {dir.title}
            </NavLink>
        );
    });
    return <SideNav items={navItems} />;
};

export default GeneratedSideNav;
