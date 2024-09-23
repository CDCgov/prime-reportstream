import { Button, Pagination } from "@trussworks/react-uswds";

import { getReportAndDownload } from "./ReportsUtils";
import AdminFetchAlert from "../../../components/alerts/AdminFetchAlert";
import { NoServicesBanner } from "../../../components/alerts/NoServicesAlert";
import Spinner from "../../../components/Spinner";
import TableFilters, { TableFilterDateLabel } from "../../../components/Table/TableFilters";
import { USLink } from "../../../components/USLink";
import { RSReceiver } from "../../../config/endpoints/settings";
import useSessionContext from "../../../contexts/Session/useSessionContext";
import useDeliveriesHistory, {
    DeliveriesDataAttr,
} from "../../../hooks/api/deliveries/UseDeliveriesHistory/UseDeliveriesHistory";
import useOrganizationReceivers from "../../../hooks/api/organizations/UseOrganizationReceivers/UseOrganizationReceivers";
import { PageSettingsActionType } from "../../../hooks/filters/UsePages/UsePages";
import { SortSettingsActionType } from "../../../hooks/filters/UseSortOrder/UseSortOrder";
import useAppInsightsContext from "../../../hooks/UseAppInsightsContext/UseAppInsightsContext";
import Table from "../../../shared/Table/Table";
import { EventName } from "../../../utils/AppInsights";
import { formatDateWithoutSeconds, isDateExpired } from "../../../utils/DateTimeUtils";
import { FeatureName } from "../../../utils/FeatureName";

const DeliveriesFilterAndTable = ({
    services,
    isOrgReceiversLoading,
}: {
    services: RSReceiver[];
    isOrgReceiversLoading: boolean;
}) => {
    const {
        data: results,
        filterManager,
        searchTerm,
        setSearchTerm,
        setService,
        dataUpdatedAt: deliveriesHistoryDataUpdatedAt,
    } = useDeliveriesHistory();
    const { authState, activeMembership } = useSessionContext();
    const { appInsights } = useAppInsightsContext();
    const featureEvent = `${FeatureName.DAILY_DATA} | ${EventName.TABLE_FILTER}`;
    const currentPageNum = filterManager.pageSettings.currentPage;
    const handleFetchAndDownload = (id: string) => {
        getReportAndDownload(id, authState.accessToken?.accessToken ?? "", activeMembership?.parsedName ?? "");
    };

    if (isOrgReceiversLoading || !results) return <Spinner />;

    const receiverDropdown = [
        ...new Set(
            services.map((data) => {
                return data.name;
            }),
        ),
    ].map((receiver) => {
        return { value: receiver, label: receiver };
    });

    const onColumnCustomSort = (columnID: string) => {
        filterManager?.updateSort({
            type: SortSettingsActionType.CHANGE_COL,
            payload: {
                column: columnID,
            },
        });
        filterManager?.updateSort({
            type: SortSettingsActionType.SWAP_ORDER,
        });
    };

    const data = results?.data.map((dataRow) => [
        {
            columnKey: DeliveriesDataAttr.REPORT_ID,
            columnHeader: "Report ID",
            content: <USLink href={`/report-details/${dataRow.reportId}`}>{dataRow.reportId}</USLink>,
        },
        {
            columnKey: DeliveriesDataAttr.CREATED_AT,
            columnHeader: "Time received",
            content: <p className="font-mono-2xs">{formatDateWithoutSeconds(dataRow.createdAt)}</p>,
            columnCustomSort: () => onColumnCustomSort(DeliveriesDataAttr.CREATED_AT),
            columnCustomSortSettings: filterManager.sortSettings,
        },
        {
            columnKey: DeliveriesDataAttr.EXPIRES_AT,
            columnHeader: "File available until",
            content: <p className="font-mono-2xs">{formatDateWithoutSeconds(dataRow.expiresAt)}</p>,
            columnCustomSort: () => onColumnCustomSort(DeliveriesDataAttr.EXPIRES_AT),
            columnCustomSortSettings: filterManager.sortSettings,
        },
        {
            columnKey: DeliveriesDataAttr.ITEM_COUNT,
            columnHeader: "Items",
            content: <p className="font-mono-2xs">{dataRow.reportItemCount}</p>,
        },
        {
            columnKey: DeliveriesDataAttr.FILE_NAME,
            columnHeader: "Filename",
            content: isDateExpired(dataRow.expiresAt) ? (
                <p>{decodeURIComponent(dataRow.fileName)}</p>
            ) : (
                <Button
                    className="font-mono-2xs line-height-alt-4"
                    type="button"
                    unstyled
                    onClick={() => handleFetchAndDownload(dataRow.reportId)}
                >
                    {decodeURIComponent(dataRow.fileName)}
                </Button>
            ),
        },
        {
            columnKey: DeliveriesDataAttr.RECEIVER,
            columnHeader: "Receiver",
            content: dataRow.receiver,
        },
    ]);

    return (
        <>
            <TableFilters
                receivers={receiverDropdown}
                startDateLabel={TableFilterDateLabel.START_DATE}
                endDateLabel={TableFilterDateLabel.END_DATE}
                showDateHints={true}
                filterManager={filterManager}
                setSearchTerm={setSearchTerm}
                searchTerm={searchTerm}
                setService={setService}
                onFilterClick={({ from, to }: { from: string; to: string }) =>
                    appInsights?.trackEvent({
                        name: featureEvent,
                        properties: {
                            tableFilter: {
                                startRange: from,
                                endRange: to,
                            },
                        },
                    })
                }
                resultLength={results?.meta.totalFilteredCount}
                deliveriesHistoryDataUpdatedAt={deliveriesHistoryDataUpdatedAt}
            />
            {services.length === 0 ? (
                <div className="usa-section margin-bottom-5">
                    <NoServicesBanner />
                </div>
            ) : (
                <>
                    <Table apiSortable borderless striped rowData={data} />
                    {data?.length !== 0 && (
                        <Pagination
                            currentPage={currentPageNum}
                            pathname=""
                            onClickPageNumber={(e) => {
                                const pageNumValue = parseInt((e.target as HTMLElement).innerText);
                                filterManager.updatePage({
                                    type: PageSettingsActionType.SET_PAGE,
                                    payload: { page: pageNumValue },
                                });
                            }}
                            onClickNext={() => {
                                filterManager.updatePage({
                                    type: PageSettingsActionType.SET_PAGE,
                                    payload: { page: currentPageNum + 1 },
                                });
                            }}
                            onClickPrevious={() => {
                                filterManager.updatePage({
                                    type: PageSettingsActionType.SET_PAGE,
                                    payload: { page: currentPageNum - 1 },
                                });
                            }}
                            totalPages={results?.meta.totalPages}
                        />
                    )}
                </>
            )}
        </>
    );
};

export function DailyData() {
    const { isLoading, isDisabled, activeReceivers } = useOrganizationReceivers();
    if (isDisabled) {
        return <AdminFetchAlert />;
    }
    return <DeliveriesFilterAndTable isOrgReceiversLoading={isLoading} services={activeReceivers} />;
}

export default DailyData;
