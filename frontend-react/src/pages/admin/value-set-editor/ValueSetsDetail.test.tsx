import { screen, act } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import { renderWithQueryProvider } from "../../../utils/CustomRenderUtils";

import ValueSetsDetail, { ValueSetsDetailTable } from "./ValueSetsDetail";

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

const mockError = new Error();

let mockSaveData = jest.fn();
let mockActivateTable = jest.fn();
let mockUseValueSetsTable = jest.fn();
let mockUseValueSetsMeta = jest.fn();

jest.mock("../../../hooks/UseValueSets", () => {
    return {
        useValueSetsTable: (valueSetName: string) =>
            mockUseValueSetsTable(valueSetName),
        useValueSetUpdate: () => ({
            saveData: mockSaveData,
        }),
        useValueSetActivation: () => ({
            activateTable: mockActivateTable,
        }),
        useValueSetsMeta: () => mockUseValueSetsMeta(),
    };
});

jest.mock("react-router-dom", () => ({
    useParams: () => ({ valueSetName: "a-path" }),
}));

describe("ValueSetsDetail", () => {
    beforeEach(() => {
        mockUseValueSetsTable = jest.fn(() => ({
            valueSetArray: fakeRows,
            error: null,
        }));
        mockUseValueSetsMeta = jest.fn(() => ({
            valueSetMeta: fakeMeta,
            error: null,
        }));
    });
    test("Renders with no errors", () => {
        // only render with query provider
        renderWithQueryProvider(<ValueSetsDetail />);
        const headers = screen.getAllByRole("columnheader");
        const title = screen.getByText("ReportStream Core Values");
        const datasetActionButton = screen.getByText("Add item");
        const rows = screen.getAllByRole("row");

        expect(headers.length).toEqual(4);
        expect(title).toBeInTheDocument();
        expect(datasetActionButton).toBeInTheDocument();
        expect(rows.length).toBe(3); // +1 for header
    });

    test("Rows are editable", () => {
        renderWithQueryProvider(<ValueSetsDetail />);
        const editButtons = screen.getAllByText("Edit");
        const rows = screen.getAllByRole("row");

        // assert they are present on all rows but header
        expect(editButtons.length).toEqual(rows.length - 1);

        // activate editing mode for first row
        userEvent.click(editButtons[0]);

        // assert input element is rendered in edit mode
        const input = screen.getAllByRole("textbox");
        expect(input.length).toEqual(3);
    });
});

describe("ValueSetsDetailTable", () => {
    test("Handles fetch related errors", () => {
        const mockSetAlert = jest.fn();
        renderWithQueryProvider(
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
            message: "Error",
        });
    });

    test("on row save, calls saveData and activateTable triggers with correct args", async () => {
        mockSaveData = jest.fn(() => {
            // to avoid unnecessary console error
            return Promise.resolve({ tableVersion: 2 });
        });

        mockActivateTable = jest.fn(() => {
            // to avoid unnecessary console error
            return Promise.resolve({ tableVersion: 2 });
        });
        const mockSetAlert = jest.fn();
        const fakeRowsCopy = [...fakeRows];

        renderWithQueryProvider(
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

        const inputs = screen.getAllByRole("textbox") as HTMLInputElement[];
        const firstInput = inputs[0];
        const initialValue = firstInput.value;
        userEvent.click(firstInput);
        userEvent.keyboard("~~fakeInputValue~~");

        const saveButton = screen.getByText("Save");
        expect(saveButton).toBeInTheDocument();
        // eslint-disable-next-line testing-library/no-unnecessary-act
        await act(async () => {
            userEvent.click(saveButton);
        });
        fakeRowsCopy.shift();

        expect(mockSaveData).toHaveBeenCalled();
        expect(mockSaveData).toHaveBeenCalledWith({
            data: [
                {
                    ...fakeRows[0],
                    display: `${initialValue}~~fakeInputValue~~`,
                },
                ...fakeRowsCopy,
            ],
            tableName: "a-path",
        });
        expect(mockActivateTable).toHaveBeenCalled();
        expect(mockActivateTable).toHaveBeenCalledWith({
            tableVersion: 2,
            tableName: "a-path",
        });
    });
});

// TODO: tests for header
