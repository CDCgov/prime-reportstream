import { fireEvent, screen, waitFor, within } from "@testing-library/react";

import { MOCK_MESSAGE_SENDER_DATA } from "../../__mocks__/MessageTrackerMockServer";
import { useMessageSearch } from "../../hooks/network/MessageTracker/MessageTrackerHooks";

import { MessageTracker } from "./MessageTracker";

const mockUseMessageSearchInitial = {
    mutateAsync: () => Promise.resolve(MOCK_MESSAGE_SENDER_DATA),
    isLoading: false,
    error: null,
};

vi.mock("../../hooks/network/MessageTracker/MessageTrackerHooks", async () => ({
    ...(await vi.importActual<
        typeof import("../../hooks/network/MessageTracker/MessageTrackerHooks")
    >("../../hooks/network/MessageTracker/MessageTrackerHooks")),
    useMessageSearch: vi.fn(),
}));

const mockUseMessageSearch = vi.mocked(useMessageSearch);

describe("MessageTracker component", () => {
    function setup() {
        mockUseMessageSearch.mockImplementation(
            () => mockUseMessageSearchInitial as any,
        );
        render(<MessageTracker />);
    }

    test("should be able to edit search field", () => {
        setup();
        const searchField = screen.getByTestId("textInput");

        expect(searchField).toBeInTheDocument();

        fireEvent.change(searchField, { target: { value: "123" } });

        expect(searchField).toHaveValue("123");
    });

    test("should be able to clear search field", async () => {
        setup();
        const searchField = screen.getByTestId("textInput");
        expect(searchField).toBeInTheDocument();

        const clearButton = await screen.findByText("Clear");
        expect(clearButton).toBeInTheDocument();

        fireEvent.click(clearButton);

        expect(searchField).toHaveValue("");
    });

    test("renders proper search results", async () => {
        setup();
        const searchField = screen.getByTestId("textInput");
        expect(searchField).toBeInTheDocument();

        const submitButton = await screen.findByText("Search");
        expect(submitButton).toBeInTheDocument();

        const clearButton = await screen.findByText("Clear");
        expect(clearButton).toBeInTheDocument();

        // set input field then click submit
        const textInput = await screen.findByTestId("textInput");
        expect(textInput).toBeInTheDocument();

        fireEvent.change(textInput, { target: { value: "123" } });
        fireEvent.click(submitButton);

        const table = await screen.findByRole("table");
        expect(table).toBeInTheDocument();

        const rows = await screen.findAllByRole("row");
        expect(rows).toHaveLength(4); // 2 warnings + header

        const firstCells = await within(rows[1]).findAllByRole("cell");
        expect(firstCells[0]).toHaveTextContent("12-234567");

        const secondCells = await within(rows[2]).findAllByRole("cell");
        expect(secondCells[0]).toHaveTextContent("12-234567");

        const thirdCells = await within(rows[3]).findAllByRole("cell");
        expect(thirdCells[0]).toHaveTextContent("12-234567");
    });

    test("trims search value leading/trailing whitespace", async () => {
        setup();
        const searchField = screen.getByTestId("textInput");
        expect(searchField).toBeInTheDocument();

        const submitButton = await screen.findByText("Search");
        expect(submitButton).toBeInTheDocument();

        const textInput = await screen.findByTestId("textInput");
        expect(textInput).toBeInTheDocument();

        fireEvent.change(textInput, { target: { value: "    abc 123    " } });
        fireEvent.click(submitButton);
        await waitFor(() => expect(textInput).toHaveValue("abc 123"));
    });
});
