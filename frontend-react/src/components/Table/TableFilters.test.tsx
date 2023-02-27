import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import { renderApp } from "../../utils/CustomRenderUtils";
import { mockCursorManager } from "../../hooks/filters/mocks/MockCursorManager";
import { mockFilterManager } from "../../hooks/filters/mocks/MockFilterManager";

import TableFilters, { isValidDateString } from "./TableFilters";

const VALID_FROM = "01/01/2023";
const VALID_TO = "02/02/2023";
const INVALID_DATE = "99/99/9999";

let startDateNode: HTMLInputElement;
let endDateNode: HTMLInputElement;
let filterButtonNode: HTMLElement;

beforeEach(() => {
    renderApp(
        <TableFilters
            filterManager={mockFilterManager}
            cursorManager={mockCursorManager}
        />
    );

    [startDateNode, endDateNode] = screen.getAllByTestId<HTMLInputElement>(
        "date-picker-external-input"
    );
    filterButtonNode = screen.getByText("Filter");
});

describe("Rendering", () => {
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
            "date-picker-internal-input"
        );
        expect(datePickers).toHaveLength(2);
    });
});

describe("when validating values", () => {
    describe("by default", () => {
        test("enables the Filter button with the fallback values", () => {
            expect(filterButtonNode).toBeEnabled();
        });
    });

    describe("when both values are valid", () => {
        test("enables the Filter button", async () => {
            // Overwrite default value
            userEvent.type(startDateNode, VALID_FROM, {
                initialSelectionStart: 0,
                initialSelectionEnd: startDateNode.value.length,
            });
            await waitFor(() => expect(startDateNode).toHaveValue(VALID_FROM));
            userEvent.type(endDateNode, VALID_TO, {
                initialSelectionStart: 0,
                initialSelectionEnd: endDateNode.value.length,
            });
            await waitFor(() => expect(endDateNode).toHaveValue(VALID_TO));
            expect(filterButtonNode).toBeEnabled();
        });
    });

    describe("when both values are invalid", () => {
        beforeEach(async () => {
            userEvent.type(startDateNode, INVALID_DATE, {
                initialSelectionStart: 0,
                initialSelectionEnd: startDateNode.value.length,
            });
            await waitFor(() =>
                expect(startDateNode).toHaveValue(INVALID_DATE)
            );
            userEvent.type(endDateNode, INVALID_DATE, {
                initialSelectionStart: 0,
                initialSelectionEnd: startDateNode.value.length,
            });
            await waitFor(() => expect(endDateNode).toHaveValue(INVALID_DATE));
        });

        test("disables the Filter button", () => {
            expect(filterButtonNode).toBeDisabled();
        });
    });

    describe("when the from range is invalid", () => {
        beforeEach(async () => {
            userEvent.type(startDateNode, INVALID_DATE, {
                initialSelectionStart: 0,
                initialSelectionEnd: startDateNode.value.length,
            });
            await waitFor(() =>
                expect(startDateNode).toHaveValue(INVALID_DATE)
            );
            userEvent.type(endDateNode, VALID_TO, {
                initialSelectionStart: 0,
                initialSelectionEnd: startDateNode.value.length,
            });
            await waitFor(() => expect(endDateNode).toHaveValue(VALID_TO));
        });

        test("disables the Filter button", () => {
            expect(filterButtonNode).toBeDisabled();
        });
    });

    describe("when the to range is invalid", () => {
        beforeEach(async () => {
            userEvent.type(startDateNode, VALID_FROM, {
                initialSelectionStart: 0,
                initialSelectionEnd: startDateNode.value.length,
            });
            await waitFor(() => expect(startDateNode).toHaveValue(VALID_FROM));
            userEvent.type(endDateNode, INVALID_DATE, {
                initialSelectionStart: 0,
                initialSelectionEnd: startDateNode.value.length,
            });
            await waitFor(() => expect(endDateNode).toHaveValue(INVALID_DATE));
        });

        test("disables the Filter button", () => {
            expect(filterButtonNode).toBeDisabled();
        });
    });

    describe("when the from value is greater than the to value", () => {
        beforeEach(async () => {
            userEvent.type(startDateNode, VALID_TO, {
                initialSelectionStart: 0,
                initialSelectionEnd: startDateNode.value.length,
            });
            await waitFor(() => expect(startDateNode).toHaveValue(VALID_TO));
            userEvent.type(endDateNode, VALID_FROM, {
                initialSelectionStart: 0,
                initialSelectionEnd: startDateNode.value.length,
            });
            await waitFor(() => expect(endDateNode).toHaveValue(VALID_FROM));
        });

        test("disables the Filter button", () => {
            expect(filterButtonNode).toBeDisabled();
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
