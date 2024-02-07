import styles from "./ReportDetailsTable.module.scss";
import Table, { TableConfig } from "../../../components/Table/Table";
import {
    EventName,
    useAppInsightsContext,
} from "../../../contexts/AppInsights";
import useFilterManager, {
    FilterManagerDefaults,
} from "../../../hooks/filters/UseFilterManager";
import { useReportsFacilities } from "../../../hooks/network/History/DeliveryHooks";
import { FeatureName } from "../../../utils/FeatureName";
import TableFilters from "../../Table/TableFilters";

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
    const { appInsights } = useAppInsightsContext();
    const { reportId }: ReportDetailsTableProps = props;
    const { data: reportFacilities } = useReportsFacilities(reportId);
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
        rows: reportFacilities!,
    };

    return (
        <div className={styles.ReportDetailsTable}>
            <section id="facilities">
                <h2>Facilities & Providers included in this report</h2>
                <TableFilters
                    startDateLabel="From: (mm/dd/yyyy)"
                    endDateLabel="To: (mm/dd/yyyy)"
                    filterManager={filterManager}
                    onFilterClick={({
                        from,
                        to,
                    }: {
                        from: string;
                        to: string;
                    }) =>
                        appInsights?.trackEvent({
                            name: featureEvent,
                            properties: {
                                tableFilter: { startRange: from, endRange: to },
                            },
                        })
                    }
                />
                <Table config={tableConfig} />
            </section>
        </div>
    );
}

export default ReportDetailsTable;
