import { screen, render, fireEvent } from "@testing-library/react";

import { FileHandlerSubmitButton } from "./FileHandlerButton";

describe("FileHandlerSubmitButton", () => {
    test("renders a submit button with proper props", async () => {
        render(
            <FileHandlerSubmitButton
                submitted={false}
                disabled={false}
                reset={() => {}}
                resetText=""
                submitText="ARBITRARY"
            />
        );

        const button = await screen.findByRole("button");
        expect(button).toHaveAttribute("type", "submit");
        expect(button).toHaveTextContent("ARBITRARY");
    });

    test("renders a reset button with proper props", async () => {
        const resetSpy = jest.fn();
        render(
            <FileHandlerSubmitButton
                submitted={true}
                disabled={false}
                reset={resetSpy}
                resetText="ARBITRARY"
                submitText=""
            />
        );

        const button = await screen.findByRole("button");
        expect(button).toHaveAttribute("type", "button");
        expect(button).toBeEnabled();
        expect(button).toHaveTextContent("ARBITRARY");

        fireEvent.click(button);

        expect(resetSpy).toHaveBeenCalledTimes(1);
    });

    test("renders a disabled button with proper props", async () => {
        render(
            <FileHandlerSubmitButton
                submitted={false}
                disabled={true}
                reset={() => {}}
                resetText=""
                submitText="ARBITRARY"
            />
        );

        const button = await screen.findByRole("button");
        expect(button).toBeDisabled();
    });
});
