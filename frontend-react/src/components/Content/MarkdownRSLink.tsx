import { Options } from "react-markdown";

import { USSmartLink } from "../USLink";

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

export type MarkdownRSLinkProps = MarkdownComponentProps<"a">;

export const MarkdownRSLink = (props: MarkdownRSLinkProps) => {
    return <USSmartLink {...props} />;
};
