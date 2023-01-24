import { screen, within } from "@testing-library/react";
import { AxiosError, AxiosResponse } from "axios";

import { renderWithFullAppContext } from "../../../utils/CustomRenderUtils";
import { ValueSet } from "../../../config/api/lookupTables";
import { RSNetworkError } from "../../../utils/RSNetworkError";

import ValueSetsIndex from "./ValueSetsIndex";

const fakeRows = [
    {
        name: "any name",
        system: "your very own system",
    },
    {
        name: "engelbert anyname",
        system: "a very different system",
    },
];

const fakeMeta = {
    createdAt: "Tuesday",
    createdBy: "you",
};

let mockUseValueSetsTable = jest.fn();
let mockUseValueSetsMeta = jest.fn();

jest.mock("../../../hooks/api/LookupTables/UseValueSetsTable", () => {
    return {
        useValueSetsTable: () => mockUseValueSetsTable(),
    };
});

jest.mock("../../../hooks/api/LookupTables/UseValueSetsMeta", () => {
    return {
        useValueSetsMeta: () => mockUseValueSetsMeta(),
    };
});

describe("ValueSetsIndex tests", () => {
    test("Renders with no errors", () => {
        mockUseValueSetsTable = jest.fn(() => ({
            data: [] as ValueSet[],
        }));

        mockUseValueSetsMeta = jest.fn(() => ({
            data: {},
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
            data: fakeRows,
        }));

        mockUseValueSetsMeta = jest.fn(() => ({
            data: fakeMeta,
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
    test("Error in query will render error UI instead of table", () => {
        mockUseValueSetsMeta = jest.fn(() => ({
            data: fakeMeta,
        }));
        mockUseValueSetsTable = jest.fn(() => {
            throw new RSNetworkError(
                new AxiosError("Test", "404", undefined, {}, {
                    status: 404,
                } as AxiosResponse)
            );
        });
        /* Outputs a large error stack...should we consider hiding error stacks in page tests since we
         * test them via the ErrorBoundary test? */
        renderWithFullAppContext(<ValueSetsIndex />);
        expect(
            screen.getByText(
                "Our apologies, there was an error loading this content."
            )
        ).toBeInTheDocument();
    });
});
