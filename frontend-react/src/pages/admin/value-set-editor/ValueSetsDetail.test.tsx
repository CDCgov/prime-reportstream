import { screen, act } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { AxiosError, AxiosResponse } from "axios";

import { renderApp } from "../../../utils/CustomRenderUtils";
import { RSNetworkError } from "../../../utils/RSNetworkError";
import {
    ValueSetsMetaResponse,
    ValueSetsTableResponse,
} from "../../../hooks/UseValueSets";
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

jest.mock("../../../hooks/UseValueSets", () => {
    return {
        ...jest.requireActual("../../../hooks/UseValueSets"),
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
    ...jest.requireActual("react-router-dom"),
    useParams: () => ({ valueSetName: "a-path" }),
}));

describe("ValueSetsDetail", () => {
    test("Renders with no errors", () => {
        mockUseValueSetsTable = jest.fn(
            () =>
                ({
                    valueSetArray: fakeRows,
                }) as ValueSetsTableResponse<any>,
        );
        mockUseValueSetsMeta = jest.fn(
            () =>
                ({
                    valueSetMeta: fakeMeta,
                }) as ValueSetsMetaResponse,
        );
        // only render with query provider
        renderApp(<ValueSetsDetail />);
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
                }) as ValueSetsTableResponse<any>,
        );
        mockUseValueSetsMeta = jest.fn(
            () =>
                ({
                    valueSetMeta: fakeMeta,
                }) as ValueSetsMetaResponse,
        );
        renderApp(<ValueSetsDetail />);
        const editButtons = screen.getAllByText("Edit");
        const rows = screen.getAllByRole("row");

        // assert they are present on all rows but header
        expect(editButtons.length).toEqual(rows.length - 1);

        // activate editing mode for first row
        await userEvent.click(editButtons[0]);

        // assert input element is rendered in edit mode
        const input = screen.getAllByRole("textbox");
        expect(input.length).toEqual(3);
    });

    test("Handles error with table fetch", () => {
        const restore = conditionallySuppressConsole("not-found: Test");
        mockUseValueSetsTable = jest.fn(() => {
            throw new RSNetworkError(
                new AxiosError("Test", "404", undefined, {}, {
                    status: 404,
                } as AxiosResponse),
            );
        });
        mockUseValueSetsMeta = jest.fn(
            () =>
                ({
                    valueSetMeta: fakeMeta,
                }) as ValueSetsMetaResponse,
        );
        /* Outputs a large error stack...should we consider hiding error stacks in page tests since we
         * test them via the ErrorBoundary test? */
        renderApp(<ValueSetsDetail />);
        expect(
            screen.getByText(
                "Our apologies, there was an error loading this content.",
            ),
        ).toBeInTheDocument();
        restore();
    });
});

describe("ValueSetsDetailTable", () => {
    test("Handles fetch related errors", () => {
        const restore = conditionallySuppressConsole("not-found: Test");
        const mockSetAlert = jest.fn();
        renderApp(
            <ValueSetsDetailTable
                valueSetName={"error"}
                setAlert={mockSetAlert}
                valueSetData={[]}
                error={mockError}
            />,
        );
        expect(mockSetAlert).toHaveBeenCalled();
        expect(mockSetAlert).toHaveBeenCalledWith({
            type: "error",
            message: "unknown-error: test-error",
        });
        restore();
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

        renderApp(
            <ValueSetsDetailTable
                valueSetName={"a-path"}
                setAlert={mockSetAlert}
                valueSetData={fakeRows}
            />,
        );
        const editButtons = screen.getAllByText("Edit");
        const editButton = editButtons[0];
        expect(editButton).toBeInTheDocument();
        await userEvent.click(editButton);

        const inputs = screen.getAllByRole("textbox") as HTMLInputElement[];
        const firstInput = inputs[0];
        const initialValue = firstInput.value;
        await userEvent.click(firstInput);
        await userEvent.keyboard("~~fakeInputValue~~");

        const saveButton = screen.getByText("Save");
        expect(saveButton).toBeInTheDocument();
        // eslint-disable-next-line testing-library/no-unnecessary-act
        await act(async () => {
            await userEvent.click(saveButton);
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
