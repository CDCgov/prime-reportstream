import { render, renderHook, screen } from "@testing-library/react";
import { useContext } from "react";

import ToastProvider, { ToastContext, ToastCtx, useToast } from "./";

describe("Toast", () => {
    describe("Provider", () => {
        test("initializes", () => {
            let ctx: ToastCtx | undefined;
            function Component() {
                ctx = useContext(ToastContext);

                return null;
            }

            render(
                <ToastProvider>
                    <Component />
                </ToastProvider>,
            );

            expect(ctx).toBeDefined();
            expect(ctx?.toast).toBeDefined();
        });

        test("renders children", () => {
            function Component() {
                return <>Success</>;
            }

            render(
                <ToastProvider>
                    <Component />
                </ToastProvider>,
            );

            expect(screen.getByText("Success")).toBeInTheDocument();
        });
    });

    describe("Hook", () => {
        test("returns context", () => {
            const {
                result: { current: ctx },
            } = renderHook(() => useToast(), {
                wrapper: ({ children }) => (
                    <ToastProvider>{children}</ToastProvider>
                ),
            });
            expect(ctx).toBeDefined();
            expect(ctx.toast).toBeDefined();
        });
    });

    // TODO
    describe.skip("toast function", () => {
        test("renders toast", () => void 0);
    });
});
