import { fireEvent, screen, within } from "@testing-library/react";

import { renderWithBase } from "../../utils/CustomRenderUtils";

import Pagination, {
    PaginationProps,
    SlotItem,
    OVERFLOW_INDICATOR,
} from "./Pagination";

describe("Pagination", () => {
    test.each([
        {
            description: "on the first page of an unbounded set",
            slots: [1, 2, 3, 4, 5, 6, OVERFLOW_INDICATOR],
            currentPageNum: 1,
        },
        {
            description: "on the second page of an unbounded set",
            slots: [1, 2, 3, 4, 5, 6, OVERFLOW_INDICATOR],
            currentPageNum: 2,
        },
        {
            description: "on the first page of a bounded set",
            slots: [1, 2],
            currentPageNum: 1,
        },
        {
            description: "on the last page of a bounded set",
            slots: [1, 2],
            currentPageNum: 2,
        },
        {
            description: "when there is only one page",
            slots: [1],
            currentPageNum: 1,
        },
    ])(
        "Handles Previous and Next links $description",
        ({ slots, currentPageNum }) => {
            const isNext = currentPageNum < slots.length;
            const isPrev = currentPageNum > 1;
            const isOverflow = slots.includes(OVERFLOW_INDICATOR);
            const props: PaginationProps = {
                slots: slots as SlotItem[],
                currentPageNum,
                setSelectedPage: jest.fn(),
            };
            renderWithBase(<Pagination {...props} />);

            const list = screen.getByRole("list");
            const { getAllByRole, getByRole } = within(list);
            const items = getAllByRole("listitem");
            const currentItem = getAllByRole("button").find((ele) =>
                ele.hasAttribute("aria-current")
            )?.parentElement;

            if (isOverflow) {
                const overflow = getByRole("presentation");
                expect(overflow).toHaveTextContent(OVERFLOW_INDICATOR);
            }

            for (const [i, ele] of items.entries()) {
                let expectedText;

                if (i === 0 && isPrev) {
                    expectedText = "Previous";
                } else if (i === items.length - 1 && isNext) {
                    expectedText = "Next";
                } else {
                    expectedText = isPrev ? i.toString() : (i + 1).toString();
                }

                if (expectedText === currentPageNum.toString()) {
                    expect(ele).toStrictEqual(currentItem);
                }

                expect(ele).toHaveTextContent(expectedText);
            }
        }
    );

    test("Clicking on pagination items invokes the setSelectedPage callback", () => {
        const mockSetSelectedPage = jest.fn();
        const props: PaginationProps = {
            slots: [1, 2, 3],
            currentPageNum: 2,
            setSelectedPage: mockSetSelectedPage,
        };
        renderWithBase(<Pagination {...props} />);

        fireEvent.click(screen.getByText("Previous"));
        expect(mockSetSelectedPage).toHaveBeenLastCalledWith(1);

        fireEvent.click(screen.getByText("1"));
        expect(mockSetSelectedPage).toHaveBeenLastCalledWith(1);

        fireEvent.click(screen.getByText("2"));
        expect(mockSetSelectedPage).toHaveBeenLastCalledWith(2);

        fireEvent.click(screen.getByText("3"));
        expect(mockSetSelectedPage).toHaveBeenLastCalledWith(3);

        fireEvent.click(screen.getByText("Next"));
        expect(mockSetSelectedPage).toHaveBeenLastCalledWith(3);
    });

    test("Renders the expected markup when the current page is between two other pages", () => {
        const props: PaginationProps = {
            slots: [1, OVERFLOW_INDICATOR, 6, 7, 8, 9, OVERFLOW_INDICATOR],
            currentPageNum: 7,
            setSelectedPage: jest.fn(),
        };
        const { asFragment } = renderWithBase(<Pagination {...props} />);
        expect(asFragment()).toMatchSnapshot();
    });

    test("Renders the expected markup when the current page is the last page", () => {
        const props: PaginationProps = {
            slots: [1, 2],
            currentPageNum: 2,
            setSelectedPage: jest.fn(),
        };
        const { asFragment } = renderWithBase(<Pagination {...props} />);
        expect(asFragment()).toMatchSnapshot();
    });
});
