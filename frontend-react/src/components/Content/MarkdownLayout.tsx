import { Grid, GridContainer } from "@trussworks/react-uswds";
import { MDXProvider } from "@mdx-js/react";

import { USSmartLink } from "../USLink";

export interface MarkdownLayoutProps {
    sidenav?: JSX.Element;
    children?: React.ReactNode;
    mainProps?: React.HTMLAttributes<HTMLElement> & {
        to?:
            | string
            | React.FunctionComponent<React.HTMLAttributes<HTMLElement>>;
    };
    sidenavProps?: React.HTMLAttributes<HTMLElement> & {
        to?:
            | string
            | React.FunctionComponent<React.HTMLAttributes<HTMLElement>>;
    };
    mdx?: React.ComponentProps<typeof MDXProvider>;
}

/**
 * FUTURE_TODO: Remove GridContainer once implemented in higher-level component.
 *
 * Default markdown layout.
 * @example
 * With sidenav:
 * +------+-----------------+
 * |      |                 |
 * |      |                 |
 * | SIDE |      MAIN       |
 * |      |                 |
 * |      |                 |
 * +------+-----------------+
 * Without sidenav:
 * +------------------------+
 * |                        |
 * |                        |
 * |         MAIN           |
 * |                        |
 * |                        |
 * +------------------------+
 */
export function MarkdownLayout({
    sidenav,
    children,
    mainProps: { to: Main = "main", ...mainProps } = {},
    sidenavProps: { to: Nav = "nav", ...sidenavProps } = {},
    mdx,
}: MarkdownLayoutProps) {
    return (
        <MDXProvider
            components={{
                a: USSmartLink,
            }}
            {...mdx}
        >
            <GridContainer className="usa-prose">
                <Grid row className="flex-justify">
                    {sidenav ? (
                        <Nav
                            aria-label="side-navigation"
                            className="tablet:grid-col-3"
                            {...sidenavProps}
                        >
                            {sidenav}
                        </Nav>
                    ) : undefined}
                    <Main
                        className={
                            sidenav ? "tablet:grid-col-8" : "tablet:grid-col-12"
                        }
                        {...mainProps}
                    >
                        {children}
                    </Main>
                </Grid>
            </GridContainer>
        </MDXProvider>
    );
}

export default MarkdownLayout;
