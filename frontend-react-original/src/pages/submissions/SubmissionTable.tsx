import { FC, useCallback } from "react";
import { useController } from "rest-hooks";

import AdminFetchAlert from "../../components/alerts/AdminFetchAlert";
import DataDashboardTableFilters from "../../components/DataDashboard/DataDashboardTable/DataDashboardTableFilters/DataDashboardTableFilters";
import { withCatchAndSuspense } from "../../components/RSErrorBoundary/RSErrorBoundary";
import Spinner from "../../components/Spinner";
import { PaginationProps } from "../../components/Table/Pagination";
import Table, { ColumnConfig, TableConfig } from "../../components/Table/Table";
import { TableFilterDateLabel } from "../../components/Table/TableFilters";
import useSessionContext from "../../contexts/Session/useSessionContext";
import useFilterManager, {
    FilterManager,
    FilterManagerDefaults,
} from "../../hooks/filters/UseFilterManager/UseFilterManager";
import { Organizations } from "../../hooks/UseAdminSafeOrganizationName/UseAdminSafeOrganizationName";
import useAppInsightsContext from "../../hooks/UseAppInsightsContext/UseAppInsightsContext";
import usePagination from "../../hooks/UsePagination/UsePagination";
import SubmissionsResource from "../../resources/SubmissionsResource";
import { EventName } from "../../utils/AppInsights";
import { FeatureName } from "../../utils/FeatureName";

const extractCursor = (s: SubmissionsResource) => s.timestamp;

const filterManagerDefaults: FilterManagerDefaults = {
    sortDefaults: {
        column: "timestamp",
        order: "DESC",
    },
};

interface SubmissionTableContentProps {
    filterManager: FilterManager;
    paginationProps?: PaginationProps;
    submissions: SubmissionsResource[];
}

function transformDate(s: string) {
    return new Date(s).toLocaleString();
}

function transformSubmissions(submissionsResource: SubmissionsResource[]): SubmissionsResource[] {
    const items = submissionsResource.map(
        (eachSubmission): SubmissionsResource => ({
            ...eachSubmission,
            fileDisplayName:
                eachSubmission.externalName && eachSubmission.externalName !== ""
                    ? eachSubmission.externalName
                    : eachSubmission.fileName,
            pk: function (): string {
                throw new Error("Function not implemented.");
            },
            isSuccessSubmitted: function (): boolean {
                throw new Error("Function not implemented.");
            },
        }),
    );
    return items;
}

const SubmissionTableContent: FC<SubmissionTableContentProps> = ({ filterManager, paginationProps, submissions }) => {
    const appInsights = useAppInsightsContext();
    const analyticsEventName = `${FeatureName.SUBMISSIONS} | ${EventName.TABLE_FILTER}`;
    const columns: ColumnConfig[] = [
        {
            dataAttr: "id",
            columnHeader: "Report ID",
            feature: {
                link: true,
                linkBasePath: "/submissions/",
            },
        },
        {
            dataAttr: "timestamp",
            columnHeader: "Date/time submitted",
            sortable: true,
            transform: transformDate,
        },
        { dataAttr: "fileDisplayName", columnHeader: "File" },
        { dataAttr: "reportItemCount", columnHeader: "Records" },
        {
            dataAttr: "httpStatus",
            columnHeader: "Status",
            valueMap: new Map([[201, "Success"]]),
        },
    ];

    const submissionsConfig: TableConfig = {
        columns: columns,
        rows: transformSubmissions(submissions),
    };

    return (
        <>
            <DataDashboardTableFilters
                startDateLabel={TableFilterDateLabel.START_DATE}
                endDateLabel={TableFilterDateLabel.END_DATE}
                showDateHints={true}
                filterManager={filterManager}
                onFilterClick={({ from, to }: { from: string; to: string }) =>
                    appInsights?.trackEvent({
                        name: analyticsEventName,
                        properties: {
                            tableFilter: { startRange: from, endRange: to },
                        },
                    })
                }
            />
            <Table config={submissionsConfig} filterManager={filterManager} paginationProps={paginationProps} />
        </>
    );
};

function SubmissionTableWithNumberedPagination() {
    const { activeMembership } = useSessionContext();
    const isAdmin = activeMembership?.parsedName === Organizations.PRIMEADMINS;

    const filterManager = useFilterManager(filterManagerDefaults);
    const pageSize = filterManager.pageSettings.size;
    const sortOrder = filterManager.sortSettings.order;
    const rangeTo = filterManager.rangeSettings.to;
    const rangeFrom = filterManager.rangeSettings.from;

    const { fetch: controllerFetch } = useController();
    const fetchResults = useCallback(
        async (currentCursor: string, numResults: number) => {
            // HACK: return empty results if requesting as an admin
            if (isAdmin) {
                return await Promise.resolve<SubmissionsResource[]>([]);
            }

            try {
                return (await controllerFetch(SubmissionsResource.list(), {
                    organization: activeMembership?.parsedName,
                    cursor: currentCursor,
                    since: rangeFrom,
                    until: rangeTo,
                    pageSize: numResults,
                    sortdir: sortOrder,
                    showFailed: false,
                })) as unknown as SubmissionsResource[];
            } catch (_e: any) {
                return [] as SubmissionsResource[];
            }
        },
        [activeMembership?.parsedName, sortOrder, controllerFetch, rangeFrom, rangeTo, isAdmin],
    );

    // The start cursor is the high value when results are in descending order
    // and the low value when the results are in ascending order.
    const startCursor = sortOrder === "DESC" ? filterManager.rangeSettings.to : filterManager.rangeSettings.from;
    // The API treats the request range as the interval [from, to).
    // When we move the `endCursor` value in ascending requests, the cursor is
    // inclusive: the request will return results whose cursor values are >= the
    // cursor.
    // When we move the `cursor` value in descending requests, the cursor is
    // exclusive: the request will return results whose cursor values are < the
    // cursor.
    const isCursorInclusive = sortOrder === "ASC";
    const analyticsEventName = `${FeatureName.SUBMISSIONS} | ${EventName.TABLE_PAGINATION}`;

    const {
        currentPageResults: submissions,
        paginationProps,
        isLoading,
    } = usePagination<SubmissionsResource>({
        startCursor,
        isCursorInclusive,
        pageSize,
        fetchResults,
        extractCursor,
        analyticsEventName,
    });

    if (isAdmin) {
        return (
            <div className="grid-container">
                <AdminFetchAlert />
            </div>
        );
    }

    if (isLoading) {
        return <Spinner />;
    }

    if (paginationProps) {
        paginationProps.label = "Submissions pagination";
    }

    return (
        <SubmissionTableContent
            filterManager={filterManager}
            paginationProps={paginationProps}
            submissions={submissions}
        />
    );
}

const SubmissionTable = () => withCatchAndSuspense(<SubmissionTableWithNumberedPagination />);

export default SubmissionTable;
