import { ReactNode } from "react";
import { SideNav } from "@trussworks/react-uswds";

import { ContentDirectory } from "../MarkdownDirectory";
import { Link } from "../../../shared/Link/Link";

export const GeneratedSideNav = ({
    directories,
}: {
    directories: ContentDirectory[];
}) => {
    const navItems: ReactNode[] = directories.map((dir) => {
        return <Link href={dir.slug}>{dir.title}</Link>;
    });
    return <SideNav items={navItems} />;
};

export default GeneratedSideNav;
