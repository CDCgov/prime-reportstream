import { screen, within } from "@testing-library/react";

import { renderWithFullAppContext } from "../../../utils/CustomRenderUtils";
import { ValueSet } from "../../../config/endpoints/lookupTables";

import ValueSetsIndex from "./ValueSetsIndex";

const fakeRows = [
    {
        name: "any name",
        system: "your very own system",
        createdAt: "Tuesday",
        createdBy: "you",
    },
    {
        name: "engelbert anyname",
        system: "a very different system",
        createdAt: "Wednesday",
        createdBy: "me",
    },
];

let mockUseValueSetsTable = jest.fn();

jest.mock("../../../hooks/UseValueSets", () => {
    return {
        useValueSetsTable: () => mockUseValueSetsTable(),
    };
});

describe("ValueSetsIndex tests", () => {
    test("Renders with no errors", () => {
        mockUseValueSetsTable = jest.fn(() => ({
            valueSetArray: [] as ValueSet[],
            error: null,
        }));
        renderWithFullAppContext(<ValueSetsIndex />);
        const headers = screen.getAllByRole("columnheader");
        const title = screen.getByText("ReportStream Value Sets");
        const rows = screen.getAllByRole("row");

        expect(headers.length).toEqual(4);
        expect(title).toBeInTheDocument();
        expect(rows.length).toBe(1); // +1 for header
    });

    test("Renders rows with data returned from hook", () => {
        mockUseValueSetsTable = jest.fn(() => ({
            valueSetArray: fakeRows,
            error: null,
        }));
        renderWithFullAppContext(<ValueSetsIndex />);
        const rows = screen.getAllByRole("row");
        expect(rows.length).toBe(3); // +1 for header

        const firstContentRow = rows[1];
        expect(
            within(firstContentRow).getByText("any name")
        ).toBeInTheDocument();
        expect(
            within(firstContentRow).getByText("your very own system")
        ).toBeInTheDocument();
        expect(
            within(firstContentRow).getByText("Tuesday")
        ).toBeInTheDocument();
        expect(within(firstContentRow).getByText("you")).toBeInTheDocument();
    });
});
