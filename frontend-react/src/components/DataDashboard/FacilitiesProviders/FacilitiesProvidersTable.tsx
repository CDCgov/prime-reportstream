import React, { Dispatch, SetStateAction } from "react";

import { EventName, trackAppInsightEvent } from "../../../utils/Analytics";
import useFilterManager, {
    FilterManagerDefaults,
} from "../../../hooks/filters/UseFilterManager";
import TableFilters from "../../Table/TableFilters";
import ReceiverServices from "../ReceiverServices/ReceiverServices";
import { RSReceiver } from "../../../config/endpoints/settings";
import { useOrganizationReceiversFeed } from "../../../hooks/UseOrganizationReceiversFeed";
import Spinner from "../../Spinner";
import { NoServicesBanner } from "../../alerts/NoServicesAlert";
import { FeatureName } from "../../../AppRouter";
import { Table } from "../../../shared/Table/Table";
import { FacilityResource } from "../../../config/endpoints/dataDashboard";
import { USLink } from "../../USLink";
import { formatDateWithoutSeconds } from "../../../utils/DateTimeUtils";
import {
    transformFacilityTypeClass,
    transformFacilityTypeLabel,
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

    // TODO: implement API once ready
    const data: FacilityResource[] = [
        {
            facilityId: "12w3e4r5",
            name: "Sally Doctor",
            location: "San Diego, CA",
            facilityType: "provider",
            reportDate: "2022-09-28T22:21:33.801667",
        },
        {
            facilityId: "12w3e4r6",
            name: "AFC Urgent Care",
            location: "San Antonio, TX",
            facilityType: "facility",
            reportDate: "2022-09-28T22:21:33.801667",
        },
        {
            facilityId: "12w3e4r7",
            name: "SimpleReport",
            location: "Fairfield, CO",
            facilityType: "submitter",
            reportDate: "2022-09-28T22:21:33.801667",
        },
    ];

    const handleSetActive = (name: string) => {
        setActiveService(receivers.find((item) => item.name === name));
    };

    const filterManager = useFilterManager(filterManagerDefaults);

    const formattedTableData = () => {
        return data
            .filter((eachFacility) => eachFacility)
            .map((eachFacility) => [
                {
                    columnKey: "name",
                    columnHeader: "Name",
                    content: (
                        <USLink
                            href={`/data-dashboard/${eachFacility.facilityType}/${eachFacility.facilityId}`}
                            className="flex-align-self-end height-5"
                        >
                            {eachFacility.name}
                        </USLink>
                    ),
                },
                {
                    columnKey: "location",
                    columnHeader: "Location",
                    content: eachFacility.location || "",
                },
                {
                    columnKey: "facilityType",
                    columnHeader: "Facility type",
                    content: eachFacility.facilityType ? (
                        <span
                            className={transformFacilityTypeClass(
                                eachFacility.facilityType
                            )}
                        >
                            {transformFacilityTypeLabel(
                                eachFacility.facilityType
                            )}
                        </span>
                    ) : (
                        ""
                    ),
                },
                {
                    columnKey: "reportDate",
                    columnHeader: "Most recent report date",
                    content: formatDateWithoutSeconds(eachFacility.reportDate),
                },
            ]);
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
                <Table
                    striped
                    borderless
                    sticky
                    rowData={formattedTableData()}
                />
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
