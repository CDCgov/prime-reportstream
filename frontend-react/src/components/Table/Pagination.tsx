import React from "react";
import { Button } from "@trussworks/react-uswds";

export const OVERFLOW_INDICATOR = Symbol("...");
export type SlotItem = number | typeof OVERFLOW_INDICATOR;

export interface SlotContentProps {
    slotItem: SlotItem;
    setCurrentPage: (pageNum: number) => void;
}

const SlotContent: React.FC<SlotContentProps> = ({
    slotItem,
    setCurrentPage,
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
        </Button>
    );
};

export interface PaginationProps {
    slots: SlotItem[];
    setCurrentPage: (pageNum: number) => void;
    currentPageNum: number;
}

const Pagination: React.FC<PaginationProps> = ({ slots, setCurrentPage }) => {
    return (
        <ul>
            {slots.map((s) => (
                <li>
                    <SlotContent slotItem={s} setCurrentPage={setCurrentPage} />
                </li>
            ))}
        </ul>
    );
};

export default Pagination;
