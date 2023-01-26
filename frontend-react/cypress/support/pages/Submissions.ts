import { DateRangeFilter } from "../pageComponents/DateRangeFilter";
import { PaginatedTable } from "../pageComponents/PaginatedTable";

import { BasePage } from "./Base";

export class SubmissionsPage extends BasePage {
    path = "/submissions";

    elementsMap = {
        heading: "h1",
    };

    dateRangeFilter = new DateRangeFilter();
    paginatedTable = new PaginatedTable(10);
}

export default new SubmissionsPage();
