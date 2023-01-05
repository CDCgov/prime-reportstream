import React, { AnchorHTMLAttributes } from "react";
import { Link, NavLink } from "react-router-dom";
import classnames from "classnames";

/** React.PropsWithChildren has known issues with generic extension in React 18,
 * so rather than using it here, we are using our own definition of child types.
 * One less headache when updating to React 18 in the future! */
interface CustomLinkProps {
    children: React.ReactNode;
    anchor?: boolean;
    className?: string;
    activeClassName?: string;
}
type USLinkProps = AnchorHTMLAttributes<{}> &
    Omit<CustomLinkProps, "activeClassName">;
type USNavLinkProps = Pick<AnchorHTMLAttributes<{}>, "href"> & CustomLinkProps;

/** A single link for rendering standard links. Uses a `Link` by default
 * but adding `anchor` will make this a generic anchor tag.
 * @example
 * <USLink href="/page">To Page</USLink> // uses <Link> from react-router-dom
 * <USLink anchor href="#this-section-on-my-page">To Section</USLink> // uses <a>
 * */
export const USLink = ({
    anchor = false,
    children,
    className,
    href,
    ...anchorHTMLAttributes
}: USLinkProps) => {
    return !anchor ? (
        <Link
            to={href || ""}
            className={classnames("usa-link", className)}
            {...anchorHTMLAttributes}
        >
            {children}
        </Link>
    ) : (
        <a
            href={href}
            className={classnames("usa-link", className)}
            {...anchorHTMLAttributes}
        >
            {children}
        </a>
    );
};

/** A single link for rendering external links. Uses {@link USLink} as a baseline,
 * with `anchor` applied, so it is not a Router link. Also handles target
 * and rel, and will disperse all other anchor attributes given.
 * @example
 * <USExtLink href="www.mysite.com">My Site</USExtLink>
 * // Same as the following:
 * <USLink
 *      anchor
 *      target="_blank"
 *      rel="noreferrer noopener"
 *      href="www.mysite.com"
 *      className="usa-link--external">
 *          My Site
 *  </USLink>
 * */
export const USExtLink = ({
    className,
    children,
    ...anchorHTMLAttributes
}: Omit<USLinkProps, "anchor" | "rel" | "target">) => {
    return (
        <USLink
            anchor
            target="_blank"
            rel="noreferrer noopener"
            className={classnames("usa-link--external", className)}
            {...anchorHTMLAttributes}
        >
            {children}
        </USLink>
    );
};

/** A single link for building breadcrumbs. Uses `USLink` as a base and renders a
 * react-router-dom `Link` with applied uswds styling for specific use in breadcrumbs */
export const USCrumbLink = ({
    className,
    children,
    ...anchorHTMLAttributes
}: Omit<USLinkProps, "anchor">) => (
    <USLink
        className={classnames("usa-breadcrumb__link", className)}
        {...anchorHTMLAttributes}
    >
        {children}
    </USLink>
);

/** A single link to replace NavLink (react-router-dom). Applies uswds navigation link styling
 * and handles both active and standard style states. This DOES NOT use `USLink` as a base; it
 * relies on `NavLink` for additional functionality. */
export const USNavLink = ({
    href,
    children,
    className,
    activeClassName,
}: USNavLinkProps) => {
    return (
        <NavLink
            to={href || ""}
            className={({ isActive }) =>
                classnames("usa-nav__link", {
                    "usa-current": isActive,
                    [activeClassName as any]: isActive, // `as any` because string may be undefined
                    [className as any]: !isActive, // `as any` because string may be undefined
                })
            }
        >
            {children}
        </NavLink>
    );
};
