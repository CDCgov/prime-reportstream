import { BasePageComponent } from "./Base";

export class PaginatedTable extends BasePageComponent {
    constructor(pageSize = 10) {
        super();
        this.pageSize = pageSize;
    }

    pageSize = 0;

    elementsMap = {
        // TODO: need more specific selectors
        table: ".usa-table",
        tableRows: ".usa-table :not(thead) tr",
        pagination: ".usa-pagination",
        paginationNumberButton: ".usa-pagination__page-no",
    };

    getInitiallyDisplayedRows(itemCount: number) {
        return itemCount < this.pageSize ? itemCount : this.pageSize;
    }
}

export default new PaginatedTable();
