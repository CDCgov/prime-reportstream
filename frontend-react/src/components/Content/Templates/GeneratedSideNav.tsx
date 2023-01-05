import { ReactNode } from "react";
import { SideNav } from "@trussworks/react-uswds";

import { ContentDirectory } from "../MarkdownDirectory";
import { USNavLink } from "../../USLink";

export const GeneratedSideNav = ({
    directories,
}: {
    directories: ContentDirectory[];
}) => {
    const navItems: ReactNode[] = directories.map((dir) => {
        return <USNavLink href={dir.slug}>{dir.title}</USNavLink>;
    });
    return <SideNav items={navItems} />;
};

export default GeneratedSideNav;
