import { useCallback, useState } from "react";

type SortOrder = "ASC" | "DESC";
type SortSetter = (column: string, currentOrder: SortOrder) => void;
interface SortSettings {
    column: string;
    order: SortOrder;
}
interface Sort extends SortSettings {
    setSort: SortSetter;
    resetSort: () => void;
}

const useSortOrder = (init?: Partial<SortSettings>): Sort => {
    /* TODO: Refactor this into a useReducer call that stores the sort
     *   state as an object. The reducer should handle setting column,
     *   swapping sort order. */
    const [column, setColumn] = useState(init?.column || "");
    const [order, setOrder] = useState<SortOrder>(init?.order || "DESC");

    const set = useCallback((column: string, currentOrder: SortOrder) => {
        setColumn(column);
        currentOrder === "ASC" ? setOrder("DESC") : setOrder("ASC");
    }, []);

    const reset = useCallback(() => {
        setColumn(init?.column || "");
        setOrder(init?.order || "DESC");
    }, [init]);

    return {
        column,
        order,
        setSort: set,
        resetSort: reset,
    };
};

export default useSortOrder;
export type { SortOrder, SortSettings, SortSetter, Sort };
