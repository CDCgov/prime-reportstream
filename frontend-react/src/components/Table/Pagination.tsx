import classnames from "classnames";
import {
    Button,
    IconNavigateBefore,
    IconNavigateNext,
} from "@trussworks/react-uswds";

export const OVERFLOW_INDICATOR = Symbol("…");
export type SlotItem = number | typeof OVERFLOW_INDICATOR;

const PaginationOverflow: React.FC = () => (
    <li
        className="usa-pagination__item usa-pagination__overflow"
        role="presentation"
    >
        <span>…</span>
    </li>
);

interface PaginationPageNumberProps {
    pageNum: number;
    setSelectedPage: (pageNum: number) => void;
    isCurrentPage: boolean;
    isLastPage: boolean;
}

const PaginationPageNumber: React.FC<PaginationPageNumberProps> = ({
    pageNum,
    setSelectedPage,
    isCurrentPage,
    isLastPage,
}) => {
    return (
        <li className="usa-pagination__item usa-pagination__page-no">
            <button
                {...(isCurrentPage && { "aria-current": "page" })}
                aria-label={`${isLastPage ? "last page, " : ""}Page ${pageNum}`}
                className={classnames(
                    ["usa-pagination__button", "rs-pagination-no-button"],
                    { "usa-current": isCurrentPage }
                )}
                onClick={() => setSelectedPage(pageNum)}
            >
                {pageNum}
            </button>
        </li>
    );
};

interface PaginationArrowProps {
    pageNum: number;
    setSelectedPage: (pageNum: number) => void;
    direction: "previous" | "next";
}

const PaginationArrow: React.FC<PaginationArrowProps> = ({
    pageNum,
    setSelectedPage,
    direction,
}) => {
    const isNext = direction === "next";
    const isPrevious = direction === "previous";
    const label = isPrevious ? "Previous" : "Next";
    const buttonClassName = classnames("rs-pagination-arrow-button", {
        "usa-pagination__previous-page": isPrevious,
        "usa-pagination__next-page": isNext,
    });
    return (
        <li className="usa-pagination__item usa-pagination__arrow">
            <Button
                aria-label={`${label} page`}
                className={buttonClassName}
                onClick={() => setSelectedPage(pageNum)}
                type="button"
                unstyled
            >
                {direction === "previous" && <IconNavigateBefore />}
                <span className="usa-pagination__link-text">{label}</span>
                {direction === "next" && <IconNavigateNext />}
            </Button>
        </li>
    );
};

export interface PaginationProps {
    slots: SlotItem[];
    setSelectedPage: (pageNum: number) => void;
    currentPageNum: number;
    label?: string;
}

const Pagination: React.FC<PaginationProps> = ({
    slots,
    setSelectedPage,
    currentPageNum,
    label = "Pagination",
}) => {
    const previousPageNum =
        currentPageNum - 1 > 0 ? currentPageNum - 1 : undefined;
    const lastSlot = slots[slots.length - 1];
    const nextPageNum =
        lastSlot === OVERFLOW_INDICATOR || lastSlot > currentPageNum
            ? currentPageNum + 1
            : undefined;

    return (
        <nav aria-label={label} className="usa-pagination">
            <ul className="usa-pagination__list">
                {previousPageNum && (
                    <PaginationArrow
                        pageNum={previousPageNum}
                        setSelectedPage={setSelectedPage}
                        direction="previous"
                    />
                )}
                {slots.map((s, i) => {
                    const key = String(s) + i;
                    if (s === OVERFLOW_INDICATOR) {
                        return <PaginationOverflow key={key} />;
                    }
                    return (
                        <PaginationPageNumber
                            key={key}
                            pageNum={s}
                            setSelectedPage={setSelectedPage}
                            isCurrentPage={s === currentPageNum}
                            isLastPage={i === slots.length - 1}
                        />
                    );
                })}
                {nextPageNum && (
                    <PaginationArrow
                        pageNum={nextPageNum}
                        setSelectedPage={setSelectedPage}
                        direction="next"
                    />
                )}
            </ul>
        </nav>
    );
};

export default Pagination;
