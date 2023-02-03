import { DateRangeFilter } from "../pageComponents/DateRangeFilter";
import { PaginatedTable } from "../pageComponents/PaginatedTable";

import { BasePage } from "./Base";

export class DailyDataPage extends BasePage {
    path = "/daily-data";

    elementsMap = {
        heading: "h1",
        receiversSelect: "#services-dropdown",
        receiversSelectOptions: "#services-dropdown option",
    };

    dateRangeFilter = new DateRangeFilter();
    paginatedTable = new PaginatedTable(10);
}

export default new DailyDataPage();
