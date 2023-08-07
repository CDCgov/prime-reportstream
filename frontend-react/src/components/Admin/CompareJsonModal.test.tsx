import React, { useRef } from "react";
import { fireEvent, screen } from "@testing-library/react";

import { renderApp } from "../../utils/CustomRenderUtils";

import {
    ConfirmSaveSettingModal,
    ConfirmSaveSettingModalRef,
    CompareSettingsModalProps,
} from "./CompareJsonModal";

describe("ConfirmSaveSettingModal", () => {
    const VALID_JSON = JSON.stringify({ a: 1 });
    const VALID_JSON_UPDATED = JSON.stringify({ a: 1, b: 2 });
    const INVALID_JSON = "{ nope }";
    let errorDiffNode: HTMLElement | null;
    let textareaNode: HTMLElement;
    let saveButtonNode: HTMLElement;
    let checkSyntaxButtonNode: HTMLElement;

    function TestWrapper(props?: Partial<CompareSettingsModalProps>) {
        const confirmModalRef = useRef<ConfirmSaveSettingModalRef>(null);

        return (
            <ConfirmSaveSettingModal
                uniquid={new Date().getTime().toString()}
                onConfirm={jest.fn()}
                ref={confirmModalRef}
                oldjson={VALID_JSON}
                newjson={VALID_JSON}
                {...props}
            />
        );
    }

    function renderComponent(props: Partial<CompareSettingsModalProps> = {}) {
        renderApp(<TestWrapper {...props} />);

        textareaNode = screen.getByTestId("EditableCompare__textarea");
        saveButtonNode = screen.getByText("Save");
        checkSyntaxButtonNode = screen.getByText("Check syntax");
    }

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe("on initial mount", () => {
        describe("when the updated JSON is valid", () => {
            beforeEach(() => {
                renderComponent();
            });

            test("disables the save button", () => {
                expect(saveButtonNode).toBeDisabled();
            });
        });

        describe("when the updated JSON is invalid", () => {
            beforeEach(() => {
                renderComponent({
                    newjson: INVALID_JSON,
                });
            });

            test("disables the save button", () => {
                expect(saveButtonNode).toBeDisabled();
            });
        });
    });

    describe("on change", () => {
        describe("when the updated JSON is valid", () => {
            beforeEach(() => {
                renderComponent();

                fireEvent.change(textareaNode, {
                    target: { value: VALID_JSON_UPDATED },
                });
            });

            test("disables the save button", () => {
                expect(saveButtonNode).toBeDisabled();
            });
        });

        describe("when the updated JSON is invalid", () => {
            beforeEach(() => {
                renderComponent();

                fireEvent.change(textareaNode, {
                    target: { value: INVALID_JSON },
                });
            });

            test("disables the save button", () => {
                expect(saveButtonNode).toBeDisabled();
            });
        });
    });

    describe("on clicking the 'Check syntax' button", () => {
        describe("when the updated JSON is valid", () => {
            describe("when there are no changes", () => {
                beforeEach(() => {
                    renderComponent();

                    fireEvent.click(checkSyntaxButtonNode);

                    errorDiffNode = screen.queryByTestId(
                        "EditableCompare__errorDiff",
                    );
                });

                test("does not render an error diff", () => {
                    expect(errorDiffNode).toBeNull();
                });

                test("does not render an error toast", () => {
                    expect(
                        screen.queryByText(/JSON data generated/),
                    ).not.toBeInTheDocument();
                });

                test("pretty-prints the JSON in the textarea", () => {
                    expect(textareaNode.innerHTML).toEqual('{\n  "a": 1\n}');
                });
            });

            describe("when there are changes", () => {
                beforeEach(() => {
                    renderComponent();

                    fireEvent.change(textareaNode, {
                        target: { value: VALID_JSON_UPDATED },
                    });
                    fireEvent.click(checkSyntaxButtonNode);

                    errorDiffNode = screen.queryByTestId(
                        "EditableCompare__errorDiff",
                    );
                });

                test("does not render an error diff", () => {
                    expect(errorDiffNode).toBeNull();
                });

                test("does not render an error toast", () => {
                    expect(
                        screen.queryByText(/JSON data generated/),
                    ).not.toBeInTheDocument();
                });

                test("pretty-prints the JSON in the textarea", () => {
                    expect(textareaNode.innerHTML).toEqual(
                        '{\n  "a": 1,\n  "b": 2\n}',
                    );
                });
            });
        });

        describe("when the updated JSON is invalid", () => {
            const consoleTraceSpy = jest.fn();

            beforeEach(() => {
                renderComponent();

                jest.spyOn(console, "trace").mockImplementationOnce(
                    consoleTraceSpy,
                );

                fireEvent.change(textareaNode, {
                    target: { value: INVALID_JSON },
                });
                fireEvent.click(checkSyntaxButtonNode);

                errorDiffNode = screen.getByTestId(
                    "EditableCompare__errorDiff",
                );
            });

            afterEach(() => {
                jest.resetAllMocks();
            });

            test("renders an error diff highlighting the error", () => {
                expect(errorDiffNode).toBeVisible();
                expect(errorDiffNode?.innerHTML).toContain("{ nope");
            });

            test("renders an error toast", () => {
                expect(consoleTraceSpy).toHaveBeenCalled();
                expect(
                    screen.queryByText(/JSON data generated/),
                ).not.toBeInTheDocument();
            });

            describe("when the user starts typing again", () => {
                test("it removes the highlighting", () => {
                    expect(errorDiffNode).toBeVisible();

                    fireEvent.change(textareaNode, {
                        target: { value: "lalalalala" },
                    });

                    errorDiffNode = screen.queryByTestId(
                        "EditableCompare__errorDiff",
                    );
                    expect(errorDiffNode).toBeNull();
                });
            });
        });
    });
});
