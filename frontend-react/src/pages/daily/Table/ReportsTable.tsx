import { Dispatch, SetStateAction, useEffect, useState } from "react";

import Table, { TableConfig } from "../../../components/Table/Table";
import useFilterManager, {
    FilterManagerDefaults,
} from "../../../hooks/filters/UseFilterManager";
import { useSessionContext } from "../../../contexts/SessionContext";
import { useReceiversList } from "../../../hooks/network/Organizations/ReceiversHooks";
import { RSReceiver } from "../../../network/api/Organizations/Receivers";
import { useReportsList } from "../../../hooks/network/History/DeliveryHooks";
import { showError } from "../../../components/AlertNotifications";
import Spinner from "../../../components/Spinner";

import { getReportAndDownload } from "./ReportsUtils";
import ServicesDropdown from "./ServicesDropdown";

/** @todo: page size default set to 10 once paginated */
const filterManagerDefaults: FilterManagerDefaults = {
    sortDefaults: {
        column: "sent",
        locally: true,
    },
    pageDefaults: {
        size: 100,
    },
};

interface ReceiverFeeds {
    loadingServices: boolean;
    services: RSReceiver[];
    activeService: RSReceiver | undefined;
    setActiveService: Dispatch<SetStateAction<RSReceiver | undefined>>;
}
/** Fetches a list of receivers for your active organization, and provides a controller to switch
 * between them */
export const useReceiverFeeds = (): ReceiverFeeds => {
    const { activeMembership } = useSessionContext();
    const {
        data: receivers,
        loading,
        trigger: getReceiversList,
    } = useReceiversList(activeMembership?.parsedName);
    const [active, setActive] = useState<RSReceiver | undefined>();
    useEffect(() => {
        // IF activeMembership?.parsedName is not undefined
        if (
            activeMembership?.parsedName !== undefined &&
            receivers === undefined
        ) {
            // Trigger useReceiversList()
            getReceiversList();
        }
        // Ignoring getReceiverList() as dep
    }, [activeMembership?.parsedName, receivers]); //eslint-disable-line

    useEffect(() => {
        if (receivers?.length) {
            setActive(receivers[0]);
        }
    }, [receivers]);

    return {
        loadingServices: loading,
        services: receivers,
        activeService: active,
        setActiveService: setActive,
    };
};

/*
    This is the main exported component from this file. It provides container styling,
    table headers, and applies the <TableData> component to the table that is created in this
    component.
*/
function ReportsTable() {
    const { oktaToken, activeMembership } = useSessionContext();
    const { loadingServices, services, activeService, setActiveService } =
        useReceiverFeeds();
    // TODO: Doesn't update parameters because of the config memo dependency array
    const {
        data: deliveries,
        loading,
        error,
        trigger: getReportsList,
    } = useReportsList(activeMembership?.parsedName, activeService?.name);
    const filterManager = useFilterManager(filterManagerDefaults);

    useEffect(
        () => {
            // IF parsedName and activeService.name are FOR SURE valid values
            // AND we don't have any deliveries yet (i.e. first fetch *has not* triggered)
            if (
                deliveries === undefined &&
                activeMembership?.parsedName !== undefined &&
                activeService?.name !== undefined
            ) {
                // Trigger useReportsList()
                getReportsList();
            }
        },
        // Ignoring getReportsList as dep
        [activeService, deliveries, activeMembership?.parsedName] //eslint-disable-line react-hooks/exhaustive-deps
    );

    useEffect(() => {
        if (error !== "") {
            showError(error);
        }
    }, [error]);

    const handleSetActive = (name: string) => {
        setActiveService(services.find((item) => item.name === name));
    };

    const handleFetchAndDownload = (id: string) => {
        getReportAndDownload(
            id,
            oktaToken?.accessToken || "",
            activeMembership?.parsedName || ""
        );
    };

    const resultsTableConfig: TableConfig = {
        columns: [
            {
                dataAttr: "reportId",
                columnHeader: "Report ID",
                feature: {
                    link: true,
                    linkBasePath: "/report-details?reportId=",
                },
            },
            {
                dataAttr: "batchReadyAt",
                columnHeader: "Available",
                sortable: true,
                localSort: true,
                transform: (s: string) => {
                    return new Date(s).toLocaleString();
                },
            },
            {
                dataAttr: "expires",
                columnHeader: "Expires",
                sortable: true,
                localSort: true,
                transform: (s: string) => {
                    return new Date(s).toLocaleString();
                },
            },
            {
                dataAttr: "reportItemCount",
                columnHeader: "Items",
            },
            {
                dataAttr: "fileType",
                columnHeader: "File",
                feature: {
                    action: handleFetchAndDownload,
                    param: "reportId",
                },
            },
        ],
        rows: deliveries || [],
    };
    if (loading || loadingServices) return <Spinner />;
    return (
        <>
            <div className="grid-container grid-col-12">
                {services && services?.length > 1 ? (
                    <ServicesDropdown
                        services={services}
                        active={activeService?.name || ""}
                        chosenCallback={handleSetActive}
                    />
                ) : (
                    <p>
                        Default service:{" "}
                        <strong>
                            {(services?.length &&
                                services[0].name.toUpperCase()) ||
                                ""}
                        </strong>
                    </p>
                )}
            </div>
            <div className="grid-col-12">
                <Table
                    config={resultsTableConfig}
                    filterManager={filterManager}
                />
            </div>
            <div className="grid-container margin-bottom-10">
                <div className="grid-col-12">
                    {deliveries?.length === 0 ? <p>No results</p> : null}
                </div>
            </div>
        </>
    );
}

export default ReportsTable;
