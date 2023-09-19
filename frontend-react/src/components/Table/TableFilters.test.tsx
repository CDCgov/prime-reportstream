import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import { renderApp } from "../../utils/CustomRenderUtils";
import { mockCursorManager } from "../../hooks/filters/mocks/MockCursorManager";
import { mockFilterManager } from "../../hooks/filters/mocks/MockFilterManager";

import TableFilters, {
    TableFilterDateLabel,
    isValidDateString,
} from "./TableFilters";

describe("Rendering", () => {
    beforeEach(() => {
        renderApp(
            <TableFilters
                startDateLabel={TableFilterDateLabel.START_DATE}
                endDateLabel={TableFilterDateLabel.END_DATE}
                filterManager={mockFilterManager}
                cursorManager={mockCursorManager}
            />,
        );
    });

    test("renders without error", async () => {
        const container = await screen.findByTestId("filter-container");
        expect(container).toBeInTheDocument();
    });

    test("pickers render", async () => {
        /* Trussworks USWDS library uses the placeholder text in two different
         *  HTML elements, so we have to use getAllBy...() rather than getBy...()
         *  and assert that they are non-null.
         * */
        const datePickers = await screen.findAllByTestId(
            "date-picker-internal-input",
        );
        expect(datePickers).toHaveLength(2);
    });
});

describe("when validating values", () => {
    const VALID_FROM = "01/01/2023";
    const VALID_TO = "02/02/2023";
    const INVALID_DATE = "99/99/9999";

    let startDateNode: HTMLElement;
    let endDateNode: HTMLElement;
    let filterButtonNode: HTMLElement;

    beforeEach(() => {
        renderApp(
            <TableFilters
                startDateLabel={TableFilterDateLabel.START_DATE}
                endDateLabel={TableFilterDateLabel.END_DATE}
                filterManager={mockFilterManager}
                cursorManager={mockCursorManager}
            />,
        );

        [startDateNode, endDateNode] = screen.getAllByTestId(
            "date-picker-external-input",
        );
        filterButtonNode = screen.getByText("Filter");
    });

    describe("by default", () => {
        test("enables the Filter button with the fallback values", () => {
            expect(filterButtonNode).toHaveProperty("disabled", false);
        });
    });

    describe("when both values are valid", () => {
        beforeEach(async () => {
            await userEvent.type(startDateNode, VALID_FROM);
            await userEvent.type(endDateNode, VALID_TO);
        });

        test("enables the Filter button", () => {
            expect(filterButtonNode).toHaveProperty("disabled", true);
        });
    });

    describe("when both values are invalid", () => {
        beforeEach(async () => {
            await userEvent.type(startDateNode, INVALID_DATE);
            await userEvent.type(endDateNode, INVALID_DATE);
        });

        test("disables the Filter button", () => {
            expect(filterButtonNode).toHaveProperty("disabled", true);
        });
    });

    describe("when the from range is invalid", () => {
        beforeEach(async () => {
            await userEvent.type(startDateNode, INVALID_DATE);
            await userEvent.type(endDateNode, VALID_TO);
        });

        test("disables the Filter button", () => {
            expect(filterButtonNode).toHaveProperty("disabled", true);
        });
    });

    describe("when the to range is invalid", () => {
        beforeEach(async () => {
            await userEvent.type(startDateNode, VALID_FROM);
            await userEvent.type(endDateNode, INVALID_DATE);
        });

        test("disables the Filter button", () => {
            expect(filterButtonNode).toHaveProperty("disabled", true);
        });
    });

    describe("when the from value is greater than the to value", () => {
        beforeEach(async () => {
            await userEvent.type(startDateNode, VALID_TO);
            await userEvent.type(endDateNode, VALID_FROM);
        });

        test("disables the Filter button", () => {
            expect(filterButtonNode).toHaveProperty("disabled", true);
        });
    });
});

describe("isValidDateString", () => {
    test("returns true only when the date string is well-formed and a valid date", () => {
        expect(isValidDateString("1/1/23")).toEqual(true);
        expect(isValidDateString("1/1/2023")).toEqual(true);
        expect(isValidDateString("01/1/2023")).toEqual(true);
        expect(isValidDateString("01/01/2023")).toEqual(true);
        expect(isValidDateString("01/01/2023")).toEqual(true);

        expect(isValidDateString("")).toEqual(false);
        expect(isValidDateString("99/99")).toEqual(false);
        expect(isValidDateString("99/99/9999")).toEqual(false);
        expect(isValidDateString("01/99/2023")).toEqual(false);
        expect(isValidDateString("abc")).toEqual(false);
    });
});
