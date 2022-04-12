import { useCallback, useState } from "react";

type SortOrder = "ASC" | "DESC";
type SortSetter = (column: string, currentOrder: SortOrder) => void;
interface SortSettings {
    column: string;
    order: SortOrder;
}
interface Sort extends SortSettings {
    set: SortSetter;
    reset: () => void;
}

const useSortOrder = (init?: Partial<SortSettings>): Sort => {
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
        set,
        reset,
    };
};

export default useSortOrder;
export type { SortOrder, SortSettings, SortSetter, Sort };
