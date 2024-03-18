import { Search } from "@trussworks/react-uswds";

export interface TableFilterSearch {
    resultLength?: number;
    activeFilters: (string | undefined)[];
}

interface TableFilterSearchProps {
    filterStatus: TableFilterData;
}

function TableFilterSearch({ filterStatus }: TableFilterSearchProps) {
    return (
        <div className="margin-bottom-4 padding-left-4">
            <label
                id="search-field-label"
                data-testid="label"
                className="usa-label"
                htmlFor="search-field"
            >
                Search by Report ID or Filename
            </label>
            <div className="usa-hint margin-bottom-105">
                Enter full Report ID or Filename, including file extension when
                applicable.
            </div>
            <Search />
        </div>
    );
}

export default TableFilterSearch;
