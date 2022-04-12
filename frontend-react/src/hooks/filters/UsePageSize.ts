import { useState } from "react";

type ItemCount = 10 | 25 | 50 | 100;
interface PageSize {
    count: ItemCount;
    setCount: (count: ItemCount) => void;
    resetCount: () => void;
}

const usePageSize = (): PageSize => {
    const [pageSize, setPageSize] = useState<ItemCount>(10);

    const resetCount = () => setPageSize(10);

    return {
        count: pageSize,
        setCount: setPageSize,
        resetCount: resetCount,
    };
};

export default usePageSize;
export type { ItemCount, PageSize };
