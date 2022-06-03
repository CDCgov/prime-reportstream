import { NavLink } from "react-router-dom";
import { SideNav } from "@trussworks/react-uswds";

import { MarkdownDirectory } from "./MarkdownDirectory";

export const GeneratedSideNav = ({
    directories,
}: {
    directories: MarkdownDirectory[];
}) => {
    const navItems = directories.map((dir) => (
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

export default GeneratedSideNav;
