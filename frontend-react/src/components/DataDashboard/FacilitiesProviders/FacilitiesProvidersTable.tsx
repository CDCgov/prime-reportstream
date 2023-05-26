import React from "react";

import { FeatureName } from "../../../AppRouter";
import { EventName, trackAppInsightEvent } from "../../../utils/Analytics";
import useFilterManager, {
    FilterManagerDefaults,
} from "../../../hooks/filters/UseFilterManager";
import Table, { TableConfig } from "../../Table/Table";
import TableFilters from "../../Table/TableFilters";
import { transformDate } from "../../../utils/DateTimeUtils";

const filterManagerDefaults: FilterManagerDefaults = {
    sortDefaults: {
        column: "reportDate",
        order: "DESC",
    },
};

export default function FacilitiesProvidersTable() {
    const data = [
        {
            name: "Sally Doctor",
            location: "San Diego, CA",
            facilityType: "Ordering Provider",
            reportDate: "2022-09-28T22:21:33.801667",
        },
        {
            name: "AFC Urgent Care",
            location: "San Antonio, TX",
            facilityType: "Performing Facility",
            reportDate: "2022-09-28T22:21:33.801667",
        },
        {
            name: "SimpleReport",
            location: "Fairfield, CO",
            facilityType: "Submitter",
            reportDate: "2022-09-28T22:21:33.801667",
        },
    ];

    const featureEvent = `${FeatureName.FACILITIES_PROVIDERS} | ${EventName.TABLE_FILTER}`;

    const filterManager = useFilterManager(filterManagerDefaults);

    const tableConfig: TableConfig = {
        columns: [
            {
                dataAttr: "name",
                columnHeader: "Name",
            },
            {
                dataAttr: "location",
                columnHeader: "Location",
            },
            {
                dataAttr: "facilityType",
                columnHeader: "Facility type",
            },
            {
                dataAttr: "reportDate",
                columnHeader: "Most recent report date",
                transform: transformDate,
            },
        ],
        rows: data!!,
    };

    return (
        <div>
            <section id="facilities-providers">
                <div className="text-bold font-sans-md">
                    Showing all results ({data.length})
                </div>
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
