import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import { renderApp } from "../../utils/CustomRenderUtils";
import * as UseSenderSchemaOptionsExports from "../../senders/hooks/UseSenderSchemaOptions";
import { INITIAL_STATE } from "../../hooks/UseFileHandler";
import { FileType } from "../../config/endpoints/settings";

import FileHandlerSchemaSelectionStep from "./FileHandlerSchemaSelectionStep";

describe("FileHandlerSchemaSelectionStep", () => {
    const DEFAULT_PROPS = {
        ...INITIAL_STATE,
        onSchemaChange: jest.fn(),
        onPrevStepClick: jest.fn(),
        onNextStepClick: jest.fn(),
    };

    describe("when the schemas are still loading", () => {
        function setup() {
            jest.spyOn(
                UseSenderSchemaOptionsExports,
                "default",
            ).mockReturnValue({
                data: [],
                isLoading: true,
            } as any);

            renderApp(<FileHandlerSchemaSelectionStep {...DEFAULT_PROPS} />);
        }

        afterEach(() => {
            jest.resetAllMocks();
        });

        test("renders the loading text", () => {
            setup();
            expect(screen.getByText("Loading...")).toBeVisible();
        });
    });

    describe("when the schemas have been loaded", () => {
        const onSchemaChangeSpy = jest.fn();

        function setup() {
            jest.spyOn(
                UseSenderSchemaOptionsExports,
                "default",
            ).mockReturnValue({
                data: [
                    {
                        value: "csv",
                        format: FileType.CSV,
                        title: "csv",
                    },
                    {
                        value: "hl7",
                        format: FileType.HL7,
                        title: "hl7",
                    },
                ],
                isLoading: false,
            } as any);

            renderApp(
                <FileHandlerSchemaSelectionStep
                    {...DEFAULT_PROPS}
                    onSchemaChange={onSchemaChangeSpy}
                />,
            );
        }

        afterEach(() => {
            jest.resetAllMocks();
        });

        test("renders the options", () => {
            setup();
            expect(screen.getByRole("option", { name: "csv" })).toBeVisible();
            expect(screen.getByRole("option", { name: "hl7" })).toBeVisible();
        });

        describe("when selecting a schema", () => {
            test("triggers the onSchemaChange callback with the schema", async () => {
                setup();
                await userEvent.selectOptions(screen.getByRole("combobox"), [
                    "csv",
                ]);
                await waitFor(() =>
                    expect(onSchemaChangeSpy).toHaveBeenCalledWith({
                        format: "CSV",
                        title: "csv",
                        value: "csv",
                    }),
                );
            });
        });
    });
});
