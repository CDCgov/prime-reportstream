import { describe, test } from "@jest/globals";
import { renderHook, screen } from "@testing-library/react";
import React from "react";
import user from "@testing-library/user-event";

import { renderApp } from "../utils/CustomRenderUtils";

import {
    Tooltip,
    useTooltipContext,
    TooltipContext,
    TooltipContextType,
    TooltipTrigger,
    TooltipContent,
} from "./Tooltip";

const TooltipContextReceiverComponent = () => {
    const { placement } = useTooltipContext();
    return <>{placement}</>;
};

const TooltipContextCreatorComponent = ({
    children,
}: {
    children: React.ReactNode;
}) => {
    const ctx: TooltipContextType = {
        placement: "Test",
    } as any;
    return (
        <TooltipContext.Provider value={ctx}>
            {children}
        </TooltipContext.Provider>
    );
};

describe("Tooltip", () => {
    describe("useTooltipContext", () => {
        test("no context - error", () => {
            expect(() => renderHook(() => useTooltipContext())).toThrowError(
                "Tooltip components must be wrapped in <Tooltip />"
            );
        });

        test("context", () => {
            renderApp(
                <TooltipContextCreatorComponent>
                    <TooltipContextReceiverComponent />
                </TooltipContextCreatorComponent>
            );
            expect(screen.getByText("Test")).toBeInTheDocument();
        });
    });

    describe("Tooltip", () => {
        test("creates tooltip context", () => {
            renderApp(
                <Tooltip placement={"Test" as any}>
                    <TooltipContextReceiverComponent />
                </Tooltip>
            );
            expect(screen.getByText("Test")).toBeInTheDocument();
        });
    });

    describe("TooltipTrigger", () => {
        test("default button", () => {
            renderApp(
                <Tooltip>
                    <TooltipTrigger>Test</TooltipTrigger>
                </Tooltip>
            );
            const button = screen.getByRole("button");
            expect(button).toHaveTextContent("Test");
        });

        test("asChild", () => {
            const Child = React.forwardRef(
                (_, ref: React.ForwardedRef<any>) => <span ref={ref}>Test</span>
            );
            renderApp(
                <Tooltip>
                    <TooltipTrigger asChild={true}>
                        <Child />
                    </TooltipTrigger>
                </Tooltip>
            );
            const element = screen.getByText("Test");
            expect(element).toBeInstanceOf(HTMLSpanElement);
        });
    });

    describe("TooltipContent", () => {
        test("default hover", async () => {
            renderApp(
                <Tooltip>
                    <TooltipTrigger>Trigger</TooltipTrigger>
                    <TooltipContent>Test</TooltipContent>
                </Tooltip>
            );
            expect(screen.queryByRole("tooltip")).not.toBeInTheDocument();
            const trigger = screen.getByRole("button");

            await user.hover(trigger);
            const tooltip = screen.getByRole("tooltip");
            expect(tooltip).toBeVisible();
            expect(tooltip).toHaveTextContent("Test");

            await user.unhover(trigger);
            expect(tooltip).not.toBeInTheDocument();
        });
    });
});
