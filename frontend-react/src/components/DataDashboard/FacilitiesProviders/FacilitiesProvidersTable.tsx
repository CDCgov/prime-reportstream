import React, { Dispatch, SetStateAction } from "react";

import { EventName, trackAppInsightEvent } from "../../../utils/Analytics";
import useFilterManager, {
    FilterManagerDefaults,
} from "../../../hooks/filters/UseFilterManager";
import Table, { TableConfig } from "../../Table/Table";
import TableFilters from "../../Table/TableFilters";
import { transformDate } from "../../../utils/DateTimeUtils";
import ReceiverServices from "../ReceiverServices/ReceiverServices";
import { RSReceiver } from "../../../config/endpoints/settings";
import { useOrganizationReceiversFeed } from "../../../hooks/UseOrganizationReceiversFeed";
import Spinner from "../../Spinner";
import { NoServicesBanner } from "../../alerts/NoServicesAlert";
import { FeatureName } from "../../../AppRouter";
import {
    AggregatorType,
    transformFacilityType,
} from "../../../utils/DataDashboardUtils";

const filterManagerDefaults: FilterManagerDefaults = {
    sortDefaults: {
        column: "reportDate",
        order: "DESC",
    },
};

interface ReceiverServicesProps {
    receivers: RSReceiver[];
    activeService: RSReceiver | undefined;
    setActiveService: Dispatch<SetStateAction<RSReceiver | undefined>>;
}

function FacilitiesProvidersTableWithPagination({
    receivers,
    activeService,
    setActiveService,
}: ReceiverServicesProps) {
    const featureEvent = `${FeatureName.FACILITIES_PROVIDERS} | ${EventName.TABLE_FILTER}`;

    const data = [
        {
            aggregatorId: "12w3e4r",
            name: "Sally Doctor",
            location: "San Diego, CA",
            aggregatorType: "provider",
            reportDate: "2022-09-28T22:21:33.801667",
        },
        {
            aggregatorId: "12w3e4r",
            name: "AFC Urgent Care",
            location: "San Antonio, TX",
            aggregatorType: "facility",
            reportDate: "2022-09-28T22:21:33.801667",
        },
        {
            aggregatorId: "12w3e4r",
            name: "SimpleReport",
            location: "Fairfield, CO",
            aggregatorType: "submitter",
            reportDate: "2022-09-28T22:21:33.801667",
        },
    ];

    const handleSetActive = (name: string) => {
        setActiveService(receivers.find((item) => item.name === name));
    };

    const filterManager = useFilterManager(filterManagerDefaults);

    // TODO: update linkBasePath with AggregatorType
    const tableConfig: TableConfig = {
        columns: [
            {
                dataAttr: "name",
                columnHeader: "Name",
                feature: {
                    link: true,
                    linkAttr: "aggregatorId",
                    linkBasePath: `/data-dashboard/facility-provider-submitter-details/${AggregatorType.FACILITY}/`,
                },
            },
            {
                dataAttr: "location",
                columnHeader: "Location",
            },
            {
                dataAttr: "facilityType",
                columnHeader: "Facility type",
                transform: transformFacilityType,
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
                <div className="display-flex flex-row">
                    <ReceiverServices
                        receivers={receivers}
                        activeService={activeService}
                        handleSetActive={handleSetActive}
                    />
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
                </div>
                <Table config={tableConfig} />
            </section>
        </div>
    );
}

export default function FacilitiesProvidersTable() {
    const { loadingServices, services, activeService, setActiveService } =
        useOrganizationReceiversFeed();

    if (loadingServices) return <Spinner />;

    if (!loadingServices && !activeService)
        return (
            <div className="usa-section margin-bottom-10">
                <NoServicesBanner
                    featureName="Active Services"
                    organization=""
                    serviceType={"receiver"}
                />
            </div>
        );

    return (
        <>
            {activeService && (
                <FacilitiesProvidersTableWithPagination
                    receivers={services}
                    activeService={activeService}
                    setActiveService={setActiveService}
                />
            )}
        </>
    );
}
