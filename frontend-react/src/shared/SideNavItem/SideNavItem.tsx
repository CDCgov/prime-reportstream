import { SideNav } from "@trussworks/react-uswds";
import { useLocation } from "react-router-dom";

import { USNavLink } from "../../components/USLink";

export interface SideNavItemProps
    extends React.AnchorHTMLAttributes<HTMLAnchorElement> {
    items?: React.ReactNode[];
    children: React.ReactNode;
    to?:
        | string
        | React.FunctionComponent<
              React.AnchorHTMLAttributes<HTMLAnchorElement> & {
                  children: React.ReactNode;
              }
          >;
    isActive?: boolean;
}

/**
 * Wrapper component that provides the NavLink and SideNav sibling elements (if items
 * provided). The parent navlink can be customized by passing a functional component
 * or html element string as the "to" prop (defaults to USNavLink). The SideNav sibling
 * can be forced visible via the "isActive" prop (otherwise defaults to checking path).
 */
export function SideNavItem({
    href,
    children,
    items,
    to: NavLink = USNavLink,
    isActive,
    ...props
}: SideNavItemProps) {
    const { pathname } = useLocation();
    const isSubnavVisible =
        isActive !== undefined
            ? isActive
            : href === undefined ||
              href === "" ||
              pathname === href ||
              pathname.startsWith(href);
    const subnavClassname = !isSubnavVisible ? "display-none" : "";

    // SideNav doesn't allow custom classes so we have to wrap in a div 😡
    return (
        <>
            <NavLink href={href} {...props}>
                {children}
            </NavLink>
            {items ? (
                <div className={subnavClassname} aria-hidden={!isSubnavVisible}>
                    <SideNav isSubnav={true} items={items} />
                </div>
            ) : undefined}
        </>
    );
}

export default SideNavItem;
