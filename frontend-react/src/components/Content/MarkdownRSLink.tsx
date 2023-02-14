import { Options } from "react-markdown";

import { USExtLink, USLink } from "../USLink";

type ReactMarkdownComponentsProp = Exclude<Options["components"], undefined>;
/**
 * Helper type for typing MarkdownRenderer components.
 * Ex: ReactMarkdownComponentProps<"a"> for custom component for anchor elements.
 * See: https://github.com/remarkjs/react-markdown#appendix-b-components
 */
export type ReactMarkdownComponentProps<
    T extends string & keyof ReactMarkdownComponentsProp
> = Extract<ReactMarkdownComponentsProp[T], (...args: any) => any> extends never
    ? never
    : Parameters<
          Extract<ReactMarkdownComponentsProp[T], (...args: any) => any>
      >[0];
export type MarkdownComponentProps<
    T extends string & keyof ReactMarkdownComponentsProp
> = Omit<ReactMarkdownComponentProps<T>, "children"> & {
    children: React.ReactNode;
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
            href.replace(/^\/\//, `${window.location.protocol}//`)
        );
        return (
            url.protocol.startsWith("http") &&
            url.host !== "cdc.gov" &&
            !url.host.endsWith(".cdc.gov")
        );
    } catch (e: any) {
        return false;
    }
}

export type MarkdownRSLinkProps = MarkdownComponentProps<"a">;

export const MarkdownRSLink = ({ children, ...props }: MarkdownRSLinkProps) => {
    let isExternal = props.href !== undefined;

    if (props.href !== undefined) {
        isExternal = isExternalUrl(props.href);
    }

    if (isExternal) {
        return <USExtLink {...props}>{children}</USExtLink>;
    }
    return <USLink {...props}>{children}</USLink>;
};
