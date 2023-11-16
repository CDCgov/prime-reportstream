import { screen, waitFor } from "@testing-library/react";
import { useRef } from "react";
import userEvent from "@testing-library/user-event";

import { render, renderHook } from "../utils/Test/render";

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
        render(
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
        await waitFor(() => {
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
        await userEvent.click(screen.getByTestId(`${id}-closebtn`));
        expect(callbackCount).toBe(1);
    });
});
