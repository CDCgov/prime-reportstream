import { screen, within } from "@testing-library/react";
import { AxiosError, AxiosResponse } from "axios";

import { renderApp } from "../../../utils/CustomRenderUtils";
import { ValueSet } from "../../../config/endpoints/lookupTables";
import { RSNetworkError } from "../../../utils/RSNetworkError";
import { conditionallySuppressConsole } from "../../../utils/TestUtils";
import {
    UseValueSetsMetaResult,
    UseValueSetsTableResult,
} from "../../../hooks/UseValueSets";

import ValueSetsIndexPage from "./ValueSetsIndex";

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

let mockUseValueSetsTable = vi.fn();
let mockUseValueSetsMeta = vi.fn();

vi.mock("../../../hooks/UseValueSets", async () => {
    return {
        useValueSetsTable: () => mockUseValueSetsTable(),
        useValueSetsMeta: () => mockUseValueSetsMeta(),
    };
});

describe("ValueSetsIndex tests", () => {
    test("Renders with no errors", () => {
        mockUseValueSetsTable = vi.fn(
            () =>
                ({
                    data: [] as ValueSet[],
                }) as UseValueSetsTableResult,
        );

        mockUseValueSetsMeta = vi.fn(
            () =>
                ({
                    data: {},
                }) as UseValueSetsMetaResult,
        );
        renderApp(<ValueSetsIndexPage />);
        const headers = screen.getAllByRole("columnheader");
        const title = screen.getByText("ReportStream Value Sets");
        const rows = screen.getAllByRole("row");

        expect(headers.length).toEqual(4);
        expect(title).toBeInTheDocument();
        expect(rows.length).toBe(1); // +1 for header
    });

    test("Renders rows with data returned from hook", () => {
        mockUseValueSetsTable = vi.fn(
            () =>
                ({
                    data: fakeRows,
                }) as UseValueSetsTableResult,
        );

        mockUseValueSetsMeta = vi.fn(
            () =>
                ({
                    data: fakeMeta,
                }) as UseValueSetsMetaResult,
        );

        renderApp(<ValueSetsIndexPage />);
        const rows = screen.getAllByRole("row");
        expect(rows.length).toBe(3); // +1 for header

        const firstContentRow = rows[1];
        expect(
            within(firstContentRow).getByText("any name"),
        ).toBeInTheDocument();
        expect(
            within(firstContentRow).getByText("your very own system"),
        ).toBeInTheDocument();
        expect(
            within(firstContentRow).getByText("Tuesday"),
        ).toBeInTheDocument();
        expect(within(firstContentRow).getByText("you")).toBeInTheDocument();
    });
    test("Error in query will render error UI instead of table", () => {
        const restore = conditionallySuppressConsole("not-found: Test");
        mockUseValueSetsMeta = vi.fn(
            () =>
                ({
                    data: fakeMeta,
                }) as UseValueSetsMetaResult,
        );
        mockUseValueSetsTable = vi.fn(() => {
            throw new RSNetworkError(
                new AxiosError("Test", "404", undefined, {}, {
                    status: 404,
                } as AxiosResponse),
            );
        });
        /* Outputs a large error stack...should we consider hiding error stacks in page tests since we
         * test them via the ErrorBoundary test? */
        renderApp(<ValueSetsIndexPage />);
        expect(
            screen.getByText(
                "Our apologies, there was an error loading this content.",
            ),
        ).toBeInTheDocument();
        restore();
    });
});
