import { screen, waitFor } from "@testing-library/react";
import { userEvent } from "@testing-library/user-event";

import FileHandlerSchemaSelectionStep from "./FileHandlerSchemaSelectionStep";
import { INITIAL_STATE } from "../../hooks/UseFileHandler/UseFileHandler";
import * as UseSenderSchemaOptionsExports from "../../hooks/UseSenderSchemaOptions/UseSenderSchemaOptions";
import { renderApp } from "../../utils/CustomRenderUtils";
import { FileType } from "../../utils/TemporarySettingsAPITypes";

describe("FileHandlerSchemaSelectionStep", () => {
    const DEFAULT_PROPS = {
        ...INITIAL_STATE,
        onSchemaChange: vi.fn(),
        onPrevStepClick: vi.fn(),
        onNextStepClick: vi.fn(),
    };

    describe("when the schemas are still loading", () => {
        function setup() {
            vi.spyOn(UseSenderSchemaOptionsExports, "default").mockReturnValue({
                data: [],
                isLoading: true,
            } as any);

            renderApp(<FileHandlerSchemaSelectionStep {...DEFAULT_PROPS} />);
        }

        test("renders the loading text", () => {
            setup();
            expect(screen.getByText("Loading...")).toBeVisible();
        });
    });

    describe("when the schemas have been loaded", () => {
        const onSchemaChangeSpy = vi.fn();

        function setup() {
            vi.spyOn(UseSenderSchemaOptionsExports, "default").mockReturnValue({
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

            renderApp(<FileHandlerSchemaSelectionStep {...DEFAULT_PROPS} onSchemaChange={onSchemaChangeSpy} />);
        }

        test("renders the options", () => {
            setup();
            expect(screen.getByRole("option", { name: "csv" })).toBeVisible();
            expect(screen.getByRole("option", { name: "hl7" })).toBeVisible();
        });

        describe("when selecting a schema", () => {
            test("triggers the onSchemaChange callback with the schema", async () => {
                setup();
                await userEvent.selectOptions(screen.getByRole("combobox"), ["csv"]);
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
