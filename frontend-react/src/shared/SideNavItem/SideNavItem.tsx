import { SideNav } from "@trussworks/react-uswds";
import { useLocation } from "react-router-dom";

import { Link } from "../../shared/Link/Link";

export interface SideNavItemProps
    extends React.AnchorHTMLAttributes<HTMLAnchorElement> {
    items?: React.ReactNode[];
    children: React.ReactNode;
    isActive?: boolean;
}

/**
 * Wrapper component that provides the NavLink and SideNav sibling elements (if items
 * provided). The SideNav sibling can be forced visible via the "isActive" prop
 * (otherwise defaults to checking path).
 */
export function SideNavItem({
    href,
    children,
    items,
    isActive,
    ...props
}: SideNavItemProps) {
    const { pathname } = useLocation();
    const isSubnavVisible =
        isActive ?? (!href || pathname === href || pathname.startsWith(href));
    const subnavClassname = !isSubnavVisible ? "display-none" : "";

    // SideNav doesn't allow custom classes so we have to wrap in a div 😡
    return (
        <>
            <Link href={href} variant="nav" {...props}>
                {children}
            </Link>
            {items ? (
                <div className={subnavClassname} aria-hidden={!isSubnavVisible}>
                    <SideNav isSubnav={true} items={items} />
                </div>
            ) : undefined}
        </>
    );
}

export default SideNavItem;
