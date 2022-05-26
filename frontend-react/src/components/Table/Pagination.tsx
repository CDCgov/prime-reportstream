export const OVERFLOW_INDICATOR = Symbol("...");
export type SlotItem = number | typeof OVERFLOW_INDICATOR;

export interface PaginationProps {
    slots: SlotItem[];
    setCurrentPage: (pageNum: number) => void;
    currentPageNum: number;
}
