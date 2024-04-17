export interface TableFilterData {
    resultLength?: number;
    activeFilters: (string | undefined)[];
}

interface TableFilterStatusProps {
    filterStatus: TableFilterData;
}

function TableFilterStatus({ filterStatus }: TableFilterStatusProps) {
    return (
        <div data-testid="filter-status" className="margin-left-2">
            <p className="display-inline">
                Showing
                {` (${filterStatus.resultLength ?? 0}) ${filterStatus.resultLength === 1 ? "result" : "results"} `}
                for:{" "}
            </p>

            <p className="display-inline">
                {filterStatus.activeFilters
                    .filter((filter) => filter)
                    .map((filter, index, array) => (
                        <span key={index}>
                            <span
                                data-testid="filter-status-results"
                                className="text-bold"
                            >
                                {filter}
                            </span>
                            {index < array.length - 1 && <span>, </span>}
                        </span>
                    ))}
            </p>
        </div>
    );
}

export default TableFilterStatus;
