import React from "react";
import { Button } from "@trussworks/react-uswds";

export const OVERFLOW_INDICATOR = Symbol("...");
export type SlotItem = number | typeof OVERFLOW_INDICATOR;

export interface SlotContentProps {
    slotItem: SlotItem;
    currentPageNum: number;
    setCurrentPage: (pageNum: number) => void;
}

const SlotContent: React.FC<SlotContentProps> = ({
    currentPageNum,
    setCurrentPage,
    slotItem,
}) => {
    if (slotItem === OVERFLOW_INDICATOR) {
        return <span>OVERFLOW_INDICATOR</span>;
    }

    return (
        <Button
            type="button"
            onClick={() => setCurrentPage(slotItem as number)}
        >
            {slotItem}
            {currentPageNum === slotItem && "*"}
        </Button>
    );
};

export interface PaginationProps {
    slots: SlotItem[];
    setCurrentPage: (pageNum: number) => void;
    currentPageNum: number;
}

const Pagination: React.FC<PaginationProps> = ({
    currentPageNum,
    setCurrentPage,
    slots,
}) => {
    return (
        <ul>
            {slots.map((s, i) => (
                <li key={`${String(s)}${i}`}>
                    <SlotContent
                        currentPageNum={currentPageNum}
                        slotItem={s}
                        setCurrentPage={setCurrentPage}
                    />
                </li>
            ))}
        </ul>
    );
};

export default Pagination;
