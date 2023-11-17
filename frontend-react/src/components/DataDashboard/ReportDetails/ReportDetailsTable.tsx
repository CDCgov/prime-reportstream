import { useCallback } from "react";

import { useReportsFacilities } from "../../../hooks/network/History/DeliveryHooks";
import Table, { TableConfig } from "../../../components/Table/Table";
import TableFilters from "../../Table/TableFilters";
import useFilterManager, {
    FilterManager,
    FilterManagerDefaults,
} from "../../../hooks/filters/UseFilterManager";
import { FeatureName } from "../../../utils/FeatureName";
import {
    EventName,
    useAppInsightsContext,
} from "../../../contexts/AppInsights";
import { RSFacility } from "../../../config/endpoints/deliveries";
import Spinner from "../../Spinner";

import styles from "./ReportDetailsTable.module.scss";

const filterManagerDefaults: FilterManagerDefaults = {
    sortDefaults: {
        column: "collectionDate",
        order: "DESC",
    },
};

interface ReportDetailsTableSharedProps {}

interface ReportDetailsTableBaseProps extends ReportDetailsTableSharedProps {
    onFilterClick: (from: string, to: string) => void;
    filterManager: FilterManager;
    facilities: RSFacility[];
}

function ReportDetailsTableBase({
    facilities,
    onFilterClick,
    filterManager,
}: ReportDetailsTableBaseProps) {
    const tableConfig: TableConfig = {
        columns: [
            { dataAttr: "facility", columnHeader: "Facility" },
            { dataAttr: "location", columnHeader: "Location" },
            { dataAttr: "CLIA", columnHeader: "CLIA" },
            { dataAttr: "total", columnHeader: "Total tests" },
            { dataAttr: "positive", columnHeader: "Total positive" },
        ],
        rows: facilities,
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
                    }) => onFilterClick(from, to)}
                />
                <Table config={tableConfig} />
            </section>
        </div>
    );
}

export interface ReportDetailsTableProps extends ReportDetailsTableSharedProps {
    reportId: string;
}

export function ReportDetailsTable({
    reportId,
    ...props
}: ReportDetailsTableProps) {
    const featureEvent = `${FeatureName.REPORT_DETAILS} | ${EventName.TABLE_FILTER}`;
    const { appInsights } = useAppInsightsContext();
    const { data: facilities, isLoading } = useReportsFacilities(reportId);
    const filterManager = useFilterManager(filterManagerDefaults);
    const filterClickHandler = useCallback(
        (from: string, to: string) => {
            appInsights?.trackEvent({
                name: featureEvent,
                properties: {
                    tableFilter: { startRange: from, endRange: to },
                },
            });
        },
        [appInsights, featureEvent],
    );

    if (isLoading || !facilities) return <Spinner />;

    return (
        <ReportDetailsTableBase
            {...props}
            onFilterClick={filterClickHandler}
            facilities={facilities}
            filterManager={filterManager}
        />
    );
}

export default ReportDetailsTable;
