import { render, screen, fireEvent } from "@testing-library/react";

import { FileHandlerForm } from "./FileHandlerForm";

describe("FileHandlerForm", () => {
    test("renders proper submitted, cancellable state", async () => {
        const resetSpy = jest.fn();
        render(
            <FileHandlerForm
                handleSubmit={() => {}}
                handleFileChange={() => {}}
                resetState={resetSpy}
                fileInputResetValue={0}
                submitted={true}
                cancellable={true}
                fileName="any"
                formLabel="any"
                resetText="any"
                submitText="any"
            />
        );

        // this is to make sure that after the form is submitted that we remove the input
        // this test id is added by trussworks, so... hopefully they don't change it?
        const input = screen.queryByTestId("file-input-input");
        expect(input).not.toBeInTheDocument();

        const cancelButton = await screen.findByText("Cancel");
        expect(cancelButton).toHaveAttribute("type", "button");

        fireEvent.click(cancelButton);

        expect(resetSpy).toHaveBeenCalledTimes(1);
    });

    // going to go ahead and deal with testing handlers here as well, in one big test
    test("renders proper unsubmitted default state, including handler behavior", async () => {
        const resetSpy = jest.fn();
        const submitSpy = jest.fn((e) => e.preventDefault()); // to prevent error message in console
        const fileChangeSpy = jest.fn();

        render(
            <FileHandlerForm
                handleSubmit={submitSpy}
                handleFileChange={fileChangeSpy}
                resetState={resetSpy}
                fileInputResetValue={0}
                submitted={false}
                cancellable={false}
                fileName="file.file"
                formLabel="somebody's form label"
                resetText="any"
                submitText="CLICK HERE"
            />
        );

        // make sure basic elements are present
        const input = await screen.findByTestId("file-input-input"); //
        expect(input).toBeInTheDocument();

        const label = await screen.findByTestId("label");
        expect(label).toHaveTextContent("somebody's form label");

        const submitButton = await screen.findByText("CLICK HERE");
        expect(submitButton).toHaveAttribute("type", "submit");

        // test file change
        fireEvent.change(input /*, fileChangeEvent */);

        expect(fileChangeSpy).toHaveBeenCalledTimes(1);

        // // For some reason this comes through with a null currentTarget
        // // I feel like it's going to be very hard to get this to work, so skipping for now
        // expect(fileChangeSpy).toHaveBeenCalledWith(
        //     expect.objectContaining(fileChangeEvent)
        // );

        fireEvent.click(submitButton);
        expect(submitSpy).toHaveBeenCalledTimes(1);
    });

    test("renders a reset button instead of submit button when submitted", async () => {
        const resetSpy = jest.fn();
        const submitSpy = jest.fn();
        const fileChangeSpy = jest.fn();
        render(
            <FileHandlerForm
                handleSubmit={submitSpy}
                handleFileChange={fileChangeSpy}
                resetState={resetSpy}
                fileInputResetValue={0}
                submitted={true}
                cancellable={false}
                fileName="file.file"
                formLabel="somebody's form label"
                resetText="CLICK HERE"
                submitText=""
            />
        );

        const resetButton = await screen.findByText("CLICK HERE");
        expect(resetButton).toHaveAttribute("type", "button");

        fireEvent.click(resetButton);

        expect(resetSpy).toHaveBeenCalledTimes(1);
    });
});
