import classnames from "classnames";
import { Button, Icon } from "@trussworks/react-uswds";

export const OVERFLOW_INDICATOR = "…";
export type SlotItem = number | typeof OVERFLOW_INDICATOR;

const PaginationOverflow: React.FC = () => (
    <li
        className="usa-pagination__item usa-pagination__overflow"
        role="listitem presentation"
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
            {/* Using `unstyled` and custom classes is a method used by Trussworks,
            but they do not export a `PaginationButton` component, so we have to rewrite
            it here.
            See: https://github.com/trussworks/react-uswds/blob/main/src/components/Pagination/Pagination.tsx */}
            <Button
                type="button"
                unstyled
                {...(isCurrentPage && { "aria-current": "page" })}
                aria-label={`${isLastPage ? "last page, " : ""}Page ${pageNum}`}
                className={classnames("usa-pagination__button", {
                    "usa-current": isCurrentPage,
                })}
                onClick={() => setSelectedPage(pageNum)}
            >
                {pageNum}
            </Button>
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
                {direction === "previous" && <Icon.NavigateBefore />}
                <span className="usa-pagination__link-text">{label}</span>
                {direction === "next" && <Icon.NavigateNext />}
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
