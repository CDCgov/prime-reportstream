import { IEventTelemetry } from "@microsoft/applicationinsights-web";
import { ButtonProps } from "@trussworks/react-uswds/lib/components/Button/Button";
import classnames from "classnames";
import DOMPurify from "dompurify";
import {
    AnchorHTMLAttributes,
    MouseEvent as ReactMouseEvent,
    ReactNode,
    useMemo,
} from "react";
import { Link, NavLink, useLocation } from "react-router-dom";
import useAppInsightsContext from "../hooks/UseAppInsightsContext";

/** PropsWithChildren has known issues with generic extension in React 18,
 * so rather than using it here, we are using our own definition of child types.
 * One less headache when updating to React 18 in the future! */
interface CustomLinkProps {
    children: ReactNode;
    className?: string;
    activeClassName?: string;
    state?: any;
}
type USLinkProps = AnchorHTMLAttributes<object> &
    Omit<CustomLinkProps, "activeClassName">;
type USNavLinkProps = Pick<AnchorHTMLAttributes<object>, "href"> &
    CustomLinkProps;

/**
 * Stateless function to get route href from href that could be
 * absolute, shorthand, relative, and/or a non-route.
 * Attempt to parse href as URL (taking into account "//" shorthand).
 * If it errors, then assume its a relative url (aka route). If it
 * parses, then verify its an absolute route through origins.
 *
 * If href is a hash anchor link, return undefined so as to bypass
 * passing through react-router.
 */
export function getHrefRoute(href?: string): string | undefined {
    if (href === undefined) return undefined;

    try {
        const url = new URL(
            href.replace(/^\/\//, `${window.location.protocol}//`),
        );
        if (
            url.protocol.startsWith("http") &&
            url.origin === window.location.origin
        )
            return `${url.pathname}${url.search}`;
    } catch (e: any) {
        return href;
    }

    return undefined;
}

export interface SafeLinkProps extends AnchorHTMLAttributes<Element> {
    state?: any;
}

const sanitizeHref = (href: string | undefined) => {
    return href ? DOMPurify.sanitize(href) : href;
};

/**
 * Sanitizes href and determines if href is an app route or regular
 * link.
 */
export const SafeLink = ({
    children,
    href,
    state,
    ...anchorHTMLAttributes
}: SafeLinkProps) => {
    const sanitizedHref = sanitizeHref(href);
    const routeHref = getHrefRoute(sanitizedHref);
    const isFile = sanitizedHref?.startsWith("/assets/");

    if (routeHref !== undefined && !isFile) {
        return (
            <Link to={href!} state={state} {...anchorHTMLAttributes}>
                {children}
            </Link>
        );
    }

    return (
        <a href={sanitizedHref} {...anchorHTMLAttributes}>
            {children}
        </a>
    );
};

/**
 * USWDS Link via SafeLink
 */
export const USLink = ({ children, className, ...props }: USLinkProps) => {
    return (
        <SafeLink className={classnames("usa-link", className)} {...props}>
            {children}
        </SafeLink>
    );
};

export interface USLinkButtonProps
    extends USLinkProps,
        Omit<ButtonProps, "type"> {}

export const USLinkButton = ({
    className,
    secondary,
    accentStyle,
    base,
    outline,
    inverse,
    size,
    unstyled,
    ...anchorHTMLAttributes
}: USLinkButtonProps) => {
    const linkClassname = classnames(
        "usa-button",
        {
            "usa-button--secondary": secondary,
            [`usa-button--accent-${accentStyle}`]: accentStyle,
            "usa-button--base": base,
            "usa-button--outline": outline,
            "usa-button--inverse": inverse,
            [`usa-button--${size}`]: size,
            "usa-button--unstyled": unstyled,
        },
        className,
    );
    if (isExternalUrl(sanitizeHref(anchorHTMLAttributes.href))) {
        return (
            <USExtLink {...anchorHTMLAttributes} className={linkClassname} />
        );
    }
    return <SafeLink {...anchorHTMLAttributes} className={linkClassname} />;
};

/** A single link for rendering external links. Uses {@link USLink} as a baseline.
 * Handles target and rel, and will disperse all other anchor attributes given.
 * @example
 * <USExtLink href="www.mysite.com">My Site</USExtLink>
 * // Same as the following:
 * <USLink
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
}: Omit<USLinkProps, "rel" | "target">) => {
    return (
        <USLink
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
}: USLinkProps) => (
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
    ...props
}: USNavLinkProps) => {
    const { hash: currentHash } = useLocation();
    const hashIndex = href?.indexOf("#") ?? -1;
    const hash = hashIndex > -1 ? href?.slice(hashIndex) : "";

    return (
        <NavLink
            to={href ?? ""}
            className={({ isActive: isPathnameActive }) => {
                // Without this, all hash links would be considered active for a path
                const isActive =
                    isPathnameActive && (hash === "" || currentHash === hash);

                return classnames("usa-nav__link", {
                    "usa-current": isActive,
                    [activeClassName as any]: isActive, // `as any` because string may be undefined
                    [className as any]: !isActive, // `as any` because string may be undefined
                });
            }}
            {...props}
        >
            {children}
        </NavLink>
    );
};

/**
 * Try to parse the href as a URL. If it throws, then it's not
 * an absolute href (aka is internal). If it parses, verify it is
 * from the cdc.gov domain (aka is internal).
 */
export function isExternalUrl(href?: string) {
    if (href === undefined) return false;
    try {
        // Browsers allow // shorthand in anchor urls but URL does not
        const url = new URL(
            href.replace(/^\/\//, `${window.location.protocol}//`),
        );
        return (
            (url.protocol.startsWith("http") &&
                url.host !== "cdc.gov" &&
                !url.host.endsWith(".cdc.gov")) ||
            href.startsWith("mailto:")
        );
    } catch (e: any) {
        return false;
    }
}

export interface USSmartLinkProps
    extends AnchorHTMLAttributes<HTMLAnchorElement> {
    trackClick?: IEventTelemetry;
}

export function USSmartLink({
    children,
    onClick,
    trackClick,
    ...props
}: USSmartLinkProps) {
    const appInsights = useAppInsightsContext();
    let isExternal = props.href !== undefined;
    const finalOnClick = useMemo(
        () =>
            appInsights && trackClick
                ? (ev: ReactMouseEvent<HTMLAnchorElement, MouseEvent>) => {
                      appInsights.trackEvent(trackClick);
                      onClick?.(ev);
                  }
                : undefined,
        [appInsights, onClick, trackClick],
    );

    if (props.href !== undefined) {
        isExternal = isExternalUrl(props.href);
    }

    if (isExternal) {
        return (
            <USExtLink onClick={finalOnClick} {...props}>
                {children}
            </USExtLink>
        );
    }
    return (
        <USLink onClick={finalOnClick} {...props}>
            {children}
        </USLink>
    );
}
