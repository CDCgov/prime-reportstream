import { useState } from "react";

type ItemCount = 10 | 25 | 50 | 100;
interface PageSize {
    count: ItemCount;
    set: (count: ItemCount) => void;
    reset: () => void;
}

const usePageSize = (): PageSize => {
    const [pageSize, setPageSize] = useState<ItemCount>(10);

    const reset = () => setPageSize(10);

    return {
        count: pageSize,
        set: setPageSize,
        reset,
    };
};

export default usePageSize;
export type { ItemCount, PageSize };
