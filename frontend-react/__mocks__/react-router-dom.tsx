import { vi } from "vitest";
import type { LinkProps as LinkPropsOrig } from "react-router-dom";
import React from "react";

const ReactRouterDom =
    await vi.importActual<typeof import("react-router-dom")>(
        "react-router-dom",
    );

interface LinkProps extends Omit<LinkPropsOrig, "className" | "to"> {
    className?: string | ((...args: any[]) => string);
    to?: string;
}

const LinkBase = ({
    to,
    className,
    state: _state,
    children,
    ...props
}: LinkProps) => (
    <a
        className={typeof className === "function" ? className({}) : className}
        href={to}
        {...props}
    >
        {children}
    </a>
);
const Link = vi.fn(LinkBase);
(Link as unknown as React.ComponentType).displayName = "Link";

module.exports = {
    ...ReactRouterDom,
    useMatch: vi.fn(),
    useNavigation: vi.fn(),
    useHref: vi.fn(),
    useRoutes: vi.fn(),
    useNavigate: vi.fn(),
    useLocation: vi.fn(() => window.location),
    useParams: vi.fn(() => ({})),
    useMatches: vi.fn(() => []),
    useSearchParams: vi.fn(),
    useResolvedPath: vi.fn(),
    useLoaderData: vi.fn(),
    useFetcher: vi.fn(),
    useOutlet: vi.fn(),
    useOutletContext: vi.fn(),
    useRouteLoaderData: vi.fn(),
    useSubmit: vi.fn(),
    useNavigateType: vi.fn(),
    useInRouterContext: vi.fn(),
    useLinkClickHandler: vi.fn(),
    useLinkPressHandler: vi.fn(),
    useActionData: vi.fn(),
    Link,
    NavLink: Link,
};
