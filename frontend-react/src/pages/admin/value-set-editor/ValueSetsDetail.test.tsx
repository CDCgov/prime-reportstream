import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { AxiosError, AxiosResponse } from "axios";

import { ValueSetsMetaResponse } from "../../../hooks/UseValueSetsMeta";
import { ValueSetsTableResponse } from "../../../hooks/UseValueSetsTable";
import { renderApp } from "../../../utils/CustomRenderUtils";
import { RSNetworkError } from "../../../utils/RSNetworkError";
import { conditionallySuppressConsole } from "../../../utils/TestUtils";

import { ValueSetsDetail, ValueSetsDetailTable } from "./ValueSetsDetail";

const fakeRows = [
    {
        name: "a-path",
        display: "hi over here first value",
        code: "1",
        version: "1",
    },
    {
        name: "a-path",
        display: "test, yes, second value",
        code: "2",
        version: "1",
    },
];

const fakeMeta = {
    lookupTableVersionId: 1,
    tableName: "fake_table",
    tableVersion: 2,
    isActive: true,
    createdBy: "me",
    createdAt: "today",
    tableSha256Checksum: "sha",
};
const mockError = new RSNetworkError(new AxiosError("test-error"));
let mockSaveData = jest.fn();
let mockActivateTable = jest.fn();
let mockUseValueSetsTable = jest.fn();
let mockUseValueSetsMeta = jest.fn();

jest.mock("../../../hooks/UseValueSetsTable", () => {
    return {
        useValueSetsTable: (valueSetName: string) =>
            mockUseValueSetsTable(valueSetName),
    };
});
jest.mock("../../../hooks/UseValueSetUpdate", () => {
    return {
        useValueSetUpdate: () => ({
            saveData: mockSaveData,
        }),
    };
});
jest.mock("../../../hooks/UseValueSetActivation", () => {
    return {
        useValueSetActivation: () => ({
            activateTable: mockActivateTable,
        }),
    };
});
jest.mock("../../../hooks/UseValueSetsMeta", () => {
    return {
        useValueSetsMeta: () => mockUseValueSetsMeta(),
    };
});

describe("ValueSetsDetail", () => {
    test("Renders with no errors", () => {
        mockUseValueSetsTable = jest.fn(
            () =>
                ({
                    valueSetArray: fakeRows,
                } as ValueSetsTableResponse<any>)
        );
        mockUseValueSetsMeta = jest.fn(
            () =>
                ({
                    valueSetMeta: fakeMeta,
                } as ValueSetsMetaResponse)
        );
        // only render with query provider
        renderApp(<ValueSetsDetail />, {
            initialRouteEntries: ["/admin/value-sets/a-path"],
        });
        const headers = screen.getAllByRole("columnheader");
        const title = screen.getByText("ReportStream Core Values");
        const datasetActionButton = screen.getByText("Add item");
        const rows = screen.getAllByRole("row");

        expect(headers.length).toEqual(4);
        expect(title).toBeInTheDocument();
        expect(datasetActionButton).toBeInTheDocument();
        expect(rows.length).toBe(3); // +1 for header
    });

    test("Rows are editable", async () => {
        mockUseValueSetsTable = jest.fn(
            () =>
                ({
                    valueSetArray: fakeRows,
                } as ValueSetsTableResponse<any>)
        );
        mockUseValueSetsMeta = jest.fn(
            () =>
                ({
                    valueSetMeta: fakeMeta,
                } as ValueSetsMetaResponse)
        );
        renderApp(<ValueSetsDetail />, {
            initialRouteEntries: ["/admin/value-sets/a-path"],
        });
        const editButtons = screen.getAllByText("Edit");
        const rows = screen.getAllByRole("row");

        // assert they are present on all rows but header
        expect(editButtons.length).toEqual(rows.length - 1);

        // activate editing mode for first row
        userEvent.click(editButtons[0]);

        // assert input element is rendered in edit mode
        expect(await screen.findAllByRole("textbox")).toHaveLength(3);
    });

    test("Handles error with table fetch", () => {
        const restore = conditionallySuppressConsole(
            "not-found: Test",
            "The above error occurred"
        );
        mockUseValueSetsTable = jest.fn(() => {
            throw new RSNetworkError(
                new AxiosError("Test", "404", undefined, {}, {
                    status: 404,
                } as AxiosResponse)
            );
        });
        mockUseValueSetsMeta = jest.fn(
            () =>
                ({
                    valueSetMeta: fakeMeta,
                } as ValueSetsMetaResponse)
        );
        renderApp(<ValueSetsDetail />, {
            initialRouteEntries: ["/admin/value-sets/a-path"],
        });
        expect(
            screen.getByText(
                "Our apologies, there was an error loading this content."
            )
        ).toBeInTheDocument();
        restore();
    });
});

describe("ValueSetsDetailTable", () => {
    test("Handles fetch related errors", async () => {
        const restore = conditionallySuppressConsole("not-found: Test");
        const mockSetAlert = jest.fn();
        renderApp(
            <ValueSetsDetailTable
                valueSetName={"error"}
                setAlert={mockSetAlert}
                valueSetData={[]}
                error={mockError}
            />
        );
        expect(mockSetAlert).toHaveBeenCalled();
        expect(mockSetAlert).toHaveBeenCalledWith({
            type: "error",
            message: "unknown-error: test-error",
        });
        restore();
    });
    test("on row save, calls saveData and activateTable triggers with correct args", async () => {
        const fakeInputValue = "~~fakeInputValue";
        mockSaveData = jest.fn(() => {
            // to avoid unnecessary console error
            return Promise.resolve({ tableVersion: 2 });
        });

        mockActivateTable = jest.fn(() => {
            // to avoid unnecessary console error
            return Promise.resolve({ tableVersion: 2 });
        });
        const mockSetAlert = jest.fn();

        renderApp(
            <ValueSetsDetailTable
                valueSetName={"a-path"}
                setAlert={mockSetAlert}
                valueSetData={fakeRows}
            />
        );
        const editButtons = screen.getAllByText("Edit");
        const editButton = editButtons[0];
        expect(editButton).toBeInTheDocument();
        userEvent.click(editButton);

        const firstInput = await screen.findByLabelText<HTMLInputElement>(
            "display-0"
        );
        const initialValue = firstInput.value;
        const newFakeRows = [
            {
                ...fakeRows[0],
                display: `${initialValue}${fakeInputValue}`,
            },
            ...fakeRows.slice(1),
        ];
        userEvent.type(firstInput, fakeInputValue);

        await waitFor(() => {
            expect(firstInput).toHaveValue(`${initialValue}${fakeInputValue}`);
        });

        const saveButton = screen.getByText("Save");
        expect(saveButton).toBeInTheDocument();
        userEvent.click(saveButton);

        await waitFor(() => {
            expect(mockSaveData).toHaveBeenCalledWith({
                data: newFakeRows,
                tableName: "a-path",
            });
        });

        expect(mockActivateTable).toHaveBeenCalledWith({
            tableVersion: 2,
            tableName: "a-path",
        });
    });
});

// TODO: tests for header
