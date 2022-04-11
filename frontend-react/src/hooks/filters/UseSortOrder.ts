import { useState } from "react";

type SortOrder = "ASC" | "DESC";
type SortSetter = (column: string) => void;
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

    const set = (column: string) => {
        setColumn(column);
        order === "ASC" ? setOrder("DESC") : setOrder("ASC");
    };

    const reset = () => {
        setColumn(init?.column || "");
        setOrder(init?.order || "DESC");
    };

    return {
        column,
        order,
        set,
        reset,
    };
};

export default useSortOrder;
export type { SortOrder, SortSettings, SortSetter, Sort };
