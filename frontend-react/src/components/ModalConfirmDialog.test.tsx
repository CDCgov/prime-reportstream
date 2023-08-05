import { screen, renderHook } from "@testing-library/react";
import React, { useRef } from "react";
import { act } from "react-dom/test-utils";
import userEvent from "@testing-library/user-event";

import { renderApp } from "../utils/CustomRenderUtils";

import { ModalConfirmDialog, ModalConfirmRef } from "./ModalConfirmDialog";

describe("ConfirmDialog", () => {
    test("Basic Render", async () => {
        const ITEM_ID = "TestItemId";
        const id = "test";
        let callbackCount = 0; // roll our own callback counter since we check itemId too.

        const confirmCallback = (itemId: string) => {
            expect(itemId).toBe(ITEM_ID);
            callbackCount++;
        };
        const { result } = renderHook(() => {
            return useRef<ModalConfirmRef>(null);
        });

        const modalRef = result.current;
        renderApp(
            <div>
                <ModalConfirmDialog
                    ref={modalRef}
                    id={id}
                    onConfirm={confirmCallback}
                ></ModalConfirmDialog>
            </div>,
        );

        // should NOT be visible before we call showModal()
        expect(screen.queryByText(/TestTitle/)).not.toBeInTheDocument();
        act(() => {
            // @ts-ignore
            modalRef?.current?.showModal({
                title: "TestTitle",
                message: "TestMessage",
                itemId: ITEM_ID,
                okButtonText: "TestOKButton",
            });
        });
        // should be visible AFTER we call show modal
        expect(screen.getByText(/TestTitle/)).toBeVisible();

        // click the cancel button and make sure it goes away and our callback was called.
        // eslint-disable-next-line testing-library/no-unnecessary-act
        await act(async () => {
            await userEvent.click(screen.getByTestId(`${id}-closebtn`));
        });
        expect(callbackCount).toBe(1);
    });
});
