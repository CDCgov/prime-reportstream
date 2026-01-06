import { ReactElement } from "react";

import Pagination from "./Pagination";

export default {
    title: "Components/Pagination",
    component: Pagination,
};

export const PaginationCmp = (): ReactElement => (
    <Pagination
        slots={[1, 2, 3]}
        setSelectedPage={() => void 0}
        currentPageNum={1}
    />
);
