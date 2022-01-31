import { useContext, useState } from "react";
import moment from "moment";
import { Button, IconNavigateBefore, IconNavigateNext, } from "@trussworks/react-uswds";
import { useResource } from "rest-hooks";
// import { useCache } from "@rest-hooks/core";
import SubmissionsResource from "../../resources/SubmissionsResource";
import { GlobalContext } from "../../components/GlobalContextProvider";


const SUBMISSION_PAGE_LENGTH = 20;

function SubmissionsTable() {
    // this component will refresh when global context changes (e.g. organization changes)
    const globalState = useContext(GlobalContext);

    // state of pagination
    const [paginationCursor, setPaginationCursor] = useState("");
    const [paginationSort, setPaginationSort] = useState("DESC");
    const [paginationPageSize] = useState(SUBMISSION_PAGE_LENGTH);

    const submissions: SubmissionsResource[] = useResource(
        SubmissionsResource.list(),
        {
            organization: globalState.state.organization,
            cursor: paginationCursor,
            pageSize: paginationPageSize,
            sort: paginationSort,
        }
    );

    const sortedSubmissions = (): SubmissionsResource[] => {
        if (paginationSort === "ASC") {
            // sort by createdAt DESC
            // because when the paginationSort is ASC, the results from the server are reversed
            submissions?.sort(
                (a, b) =>
                    b.createdAt.getTime() - a.createdAt.getTime()
            );
        }
        return submissions || [];
    };

    const getCursor = (): string => {
        if (!submissions || !submissions.length) {
            return "";
        }
        return submissions[submissions.length - 1]?.createdAt?.toString() || "";
    }
    const updatePaginationCursor = (next: boolean) => {
        if (!submissions) {
            return;
        }
        let cursor = submissions[0]?.createdAt?.toString() || "";
        let sort = "ASC";
        if (next) {
            cursor = getCursor();
            sort = "DESC";
        }
        setPaginationCursor(cursor);
        setPaginationSort(sort);
    };

    // we can tell if we're on the first page by saving the first result and then checking against it later
    const [firstPaginationCursor] = useState(getCursor());

    const onFirstPage = () => {
        return submissions?.find(
            (s) => s.createdAt?.toString() === firstPaginationCursor
        );
    };

    // 2022-01-07: the team suggested to see if the number of items matches
    // the requested page size to tell if it is the last page,
    // and then leave a message if there are no results on the next page
    const onLastPage = () => {
        return submissions?.length !== paginationPageSize;
    };

    return (
        <div className="grid-container usa-section margin-bottom-10">
            <div className="grid-col-12">
                <h2>Submissions</h2>
                <table
                    className="usa-table usa-table--borderless prime-table"
                    aria-label="Submission history from the last 30 days"
                >
                    <thead>
                    <tr>
                        <th scope="col">Date/time submitted</th>
                        <th scope="col">File</th>
                        <th scope="col">Records</th>
                        <th scope="col">Report ID</th>
                    </tr>
                    </thead>
                    <tbody id="tBody" className="font-mono-2xs">
                    {sortedSubmissions().filter(
                        // failed will not have an id. do not display them.
                        (s) => s.isSuccessSubmitted()
                    ).map((s) => {
                        return (
                            <tr key={s.pk()}>
                                <th scope="row">
                                    {moment
                                        .utc(s.createdAt)
                                        .local()
                                        .format("YYYY-MM-DD HH:mm")}
                                </th>
                                {/* File name */}
                                <th scope="row">{s.externalName}</th>
                                <th scope="row">{s.reportItemCount}</th>
                                <th scope="row">{s.id}</th>
                            </tr>
                        );
                    })}
                    </tbody>
                </table>
                {( submissions?.length === 0 && !paginationCursor ) && (
                    <p>There were no results found.</p>
                )}
                {( submissions?.length === 0 && paginationCursor ) && (
                    <p>No more results found.</p>
                )}
                {( submissions?.length !== 0 || paginationCursor ) && (
                    <span className="float-right margin-top-5">
                        {!onFirstPage() && (
                            <Button
                                className="text-no-underline margin-right-4"
                                type="button"
                                unstyled
                                onClick={() => updatePaginationCursor(false)}
                            >
                                <span>
                                    <IconNavigateBefore className="text-middle"/>
                                    Previous
                                </span>
                            </Button>
                        )}
                        {!onLastPage() && (
                            <Button
                                className="text-no-underline"
                                type="button"
                                unstyled
                                onClick={() => updatePaginationCursor(true)}
                            >
                                <span>
                                    Next
                                    <IconNavigateNext className="text-middle"/>
                                </span>
                            </Button>
                        )}
                    </span>
                )}
            </div>
        </div>
    );
}

export default SubmissionsTable;
