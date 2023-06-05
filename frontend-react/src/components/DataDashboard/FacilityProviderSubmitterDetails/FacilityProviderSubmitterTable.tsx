import React from "react";

import Table, { TableConfig } from "../../../components/Table/Table";
import { EventName, trackAppInsightEvent } from "../../../utils/Analytics";
import TableFilters from "../../Table/TableFilters";
import useFilterManager, {
    FilterManagerDefaults,
} from "../../../hooks/filters/UseFilterManager";
import { FeatureName } from "../../../AppRouter";
import { transformDate } from "../../../utils/DateTimeUtils";

import styles from "./FacilityProviderSubmitterTable.module.scss";

const filterManagerDefaults: FilterManagerDefaults = {
    sortDefaults: {
        column: "collectionDate",
        order: "DESC",
    },
};

interface FacilityProviderSubmitterTableProps {
    senderTypeId: string;
    senderTypeName: string;
}

function FacilityProviderSubmitterTable(
    props: FacilityProviderSubmitterTableProps
) {
    const featureEvent = `${FeatureName.REPORT_DETAILS} | ${EventName.TABLE_FILTER}`;
    // const { senderTypeId }: FacilityProviderSubmitterTableProps = props;
    const data = [
        {
            reportId: "fd34d590-eb8f-412f-9562-0975f2c413e3",
            total: "5",
            batchReadyAt: "2022-09-28T22:21:33.801667",
            expires: "2022-10-28T22:21:33.801667",
        },
        {
            reportId: "fd34d590-eb8f-412f-9562-0975f2c413e3",
            total: "1",
            batchReadyAt: "2022-09-28T22:21:33.801667",
            expires: "2022-11-28T22:21:33.801667",
        },
        {
            reportId: "fd34d590-eb8f-412f-9562-0975f2c413e3",
            total: "2",
            batchReadyAt: "2022-09-28T22:21:33.801667",
            expires: "2023-09-28T22:21:33.801667",
        },
    ];

    const filterManager = useFilterManager(filterManagerDefaults);

    const tableConfig: TableConfig = {
        columns: [
            {
                dataAttr: "reportId",
                columnHeader: "Report ID",
            },
            {
                dataAttr: "batchReadyAt",
                columnHeader: "Date sent to you",
                transform: transformDate,
            },
            {
                dataAttr: "expires",
                columnHeader: "Available until",
                transform: transformDate,
            },
            { dataAttr: "total", columnHeader: "Test results" },
        ],
        rows: data!!,
    };

    return (
        <div className={styles.FacilityProviderSubmitterTable}>
            <section id="facilities">
                <h2>Your available reports including {props.senderTypeName}</h2>
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

export default FacilityProviderSubmitterTable;
