import { screen } from "@testing-library/react";
import { PropsWithChildren, ReactNode, useMemo, useState } from "react";

import MarkdownLayoutContext from "./Context";
import { LayoutMain, LayoutSidenav } from "./LayoutComponents";
import { renderApp } from "../../utils/CustomRenderUtils";

describe("LayoutComponents", () => {
    function TestComponent(props: PropsWithChildren<object>) {
        const [sidenavContent, setSidenavContent] =
            useState<ReactNode>(undefined);
        const [mainContent, setMainContent] = useState<ReactNode>(undefined);
        const ctx = useMemo(() => {
            return {
                sidenavContent,
                setSidenavContent,
                mainContent,
                setMainContent,
            };
        }, [mainContent, sidenavContent]);

        return (
            <MarkdownLayoutContext.Provider value={ctx}>
                {ctx.sidenavContent ? (
                    <section data-testid="sidenav">
                        {ctx.sidenavContent}
                    </section>
                ) : null}
                {ctx.mainContent ? (
                    <section data-testid="main">{ctx.mainContent}</section>
                ) : null}
                {props.children ? (
                    <section data-testid="children">{props.children}</section>
                ) : null}
            </MarkdownLayoutContext.Provider>
        );
    }

    test("LayoutSidenav", () => {
        renderApp(
            <TestComponent>
                <LayoutSidenav>Test Sidenav</LayoutSidenav>
            </TestComponent>,
        );
        expect(screen.getByTestId("sidenav")).toHaveTextContent("Test Sidenav");
        expect(screen.getByTestId("children")).not.toHaveTextContent(
            "Test Sidenav",
        );
    });

    test("LayoutMain", () => {
        renderApp(
            <TestComponent>
                <LayoutMain>Test Main</LayoutMain>
            </TestComponent>,
        );
        expect(screen.getByTestId("main")).toHaveTextContent("Test Main");
        expect(screen.getByTestId("children")).not.toHaveTextContent(
            "Test Main",
        );
    });
});
