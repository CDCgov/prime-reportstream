export interface TableFilterData {
    resultLength?: number;
    activeFilters: (string | undefined)[];
}

interface TableFilterStatusProps {
    filterStatus: TableFilterData;
    showDefaultStatus: boolean;
}

function TableFilterStatus({
    filterStatus,
    showDefaultStatus,
}: TableFilterStatusProps) {
    return (
        <div data-testid="filter-status" className="margin-left-2">
            {showDefaultStatus ? (
                <p>Showing all data ({filterStatus.resultLength ?? 0})</p>
            ) : (
                <>
                    {" "}
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
                                    <span className="text-bold">{filter}</span>
                                    {index < array.length - 1 && (
                                        <span>, </span>
                                    )}
                                </span>
                            ))}
                    </p>
                </>
            )}
        </div>
    );
}

export default TableFilterStatus;
