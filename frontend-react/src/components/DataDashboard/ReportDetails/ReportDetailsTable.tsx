import React from "react";

import { useReportsFacilities } from "../../../hooks/network/History/DeliveryHooks";
import Table, { TableConfig } from "../../../components/Table/Table";
import { EventName, trackAppInsightEvent } from "../../../utils/Analytics";
import TableFilters from "../../Table/TableFilters";
import useFilterManager, {
    FilterManagerDefaults,
} from "../../../hooks/filters/UseFilterManager";
import { FeatureName } from "../../../AppRouter";

import styles from "./ReportDetailsTable.module.scss";

const filterManagerDefaults: FilterManagerDefaults = {
    sortDefaults: {
        column: "collectionDate",
        order: "DESC",
    },
};

interface ReportDetailsTableProps {
    reportId: string;
}

function ReportDetailsTable(props: ReportDetailsTableProps) {
    const { reportId }: ReportDetailsTableProps = props;
    const { reportFacilities } = useReportsFacilities(reportId);
    const featureEvent = `${FeatureName.REPORT_DETAILS} | ${EventName.TABLE_FILTER}`;

    const filterManager = useFilterManager(filterManagerDefaults);

    const tableConfig: TableConfig = {
        columns: [
            { dataAttr: "facility", columnHeader: "Facility" },
            { dataAttr: "location", columnHeader: "Location" },
            { dataAttr: "CLIA", columnHeader: "CLIA" },
            { dataAttr: "total", columnHeader: "Total tests" },
            { dataAttr: "positive", columnHeader: "Total positive" },
        ],
        rows: reportFacilities!!,
    };

    return (
        <div className={styles.ReportDetailsTable}>
            <section id="facilities">
                <h2>Facilities & Providers included in this report</h2>
                <TableFilters
                    startDateLabel="From: (mm/dd/yyy)"
                    endDateLabel="To: (mm/dd/yyyy)"
                    filterManager={filterManager}
                    onFilterClick={({
                        from,
                        to,
                    }: {
                        from: string;
                        to: string;
                    }) =>
                        trackAppInsightEvent(featureEvent, {
                            tableFilter: { startRange: from, endRange: to },
                        })
                    }
                />
                <Table config={tableConfig} />
            </section>
        </div>
    );
}

export default ReportDetailsTable;
