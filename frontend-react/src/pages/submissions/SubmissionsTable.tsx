import { useContext, useEffect, useState } from "react";
import {
    Button,
    ButtonGroup,
    IconNavigateBefore,
    IconNavigateNext,
} from "@trussworks/react-uswds";
import { useResource } from "rest-hooks";
import { NavLink } from "react-router-dom";

import SubmissionsResource from "../../resources/SubmissionsResource";
import { GlobalContext } from "../../components/GlobalContextProvider";

const SUBMISSION_PAGE_LENGTH = 10;

function SubmissionsTable() {
    // this component will refresh when global context changes (e.g. organization changes)
    const globalState = useContext(GlobalContext);

    // state of pagination
    const [paginationCursor, setPaginationCursor] = useState("");
    const [paginationSort, setPaginationSort] = useState("DESC");

    const submissions: SubmissionsResource[] = useResource(
        SubmissionsResource.list(),
        {
            organization: globalState.state.organization,
            cursor: paginationCursor,
            pageSize: SUBMISSION_PAGE_LENGTH,
            sort: paginationSort,
        }
    );

    // we can tell if we're on the first page by saving the first result and then checking against it later
    const [firstPaginationCursor, setFirstPaginationCursor] = useState("");
    const [atLeastOneEntry, setAtLeastOneEntry] = useState(false);
    const [moreThanOnePage, setMoreThanOnePage] = useState(false);

    // after the first page loads, set up some state info about the data.
    useEffect(() => {
        if (
            !submissions?.length || // no data
            atLeastOneEntry
        ) {
            // already ran
            return; // use defaults or data determined from the first page.
        }
        setAtLeastOneEntry(true); // otherwise !submissions?.length would have returned
        setFirstPaginationCursor(submissions[0].createdAt || "");
        setMoreThanOnePage(submissions.length >= SUBMISSION_PAGE_LENGTH);
    }, [atLeastOneEntry, submissions]);

    const getSortedSubmissions = (): SubmissionsResource[] => {
        submissions?.sort((a, b) => SubmissionsResource.sortByCreatedAt(a, b));
        return submissions || [];
    };

    // this treats the FIRST entry in the list as the starting point. Used when doing a Prev
    const getCursorStart = (): string => {
        if (!submissions || !submissions.length) {
            return "";
        }
        return submissions[0]?.createdAt || "";
    };

    // this treats the last entry on the page as the starting point. Used when doing Next
    const getCursorEnd = (): string => {
        if (!submissions || !submissions.length) {
            return "";
        }
        return submissions[submissions.length - 1]?.createdAt || "";
    };

    const updatePaginationCursor = (next: boolean) => {
        if (!submissions) {
            return;
        }
        const cursor = next ? getCursorEnd() : getCursorStart();
        const sort = next ? "DESC" : "ASC";
        setPaginationCursor(cursor);
        setPaginationSort(sort);
    };

    const onFirstPage = () => {
        // do any elements match the current cursor?
        return submissions?.find((s) => s.createdAt === firstPaginationCursor);
    };

    // 2022-01-07: the team suggested to see if the number of items matches
    // the requested page size to tell if it is the last page,
    // and then leave a message if there are no results on the next page
    const onLastPage = () => {
        return submissions?.length !== SUBMISSION_PAGE_LENGTH;
    };

    const NextPrevButtonsComponent = () => {
        // on both the first and last page if there's only one page of data
        if (!moreThanOnePage) {
            return <></>;
        }
        return (
            <ButtonGroup type="segmented" className="float-right margin-top-5">
                {!onFirstPage() && (
                    <Button
                        type="button"
                        onClick={() => updatePaginationCursor(false)}
                    >
                        <span>
                            <IconNavigateBefore className="text-middle" />
                            Previous
                        </span>
                    </Button>
                )}
                {!onLastPage() && (
                    <Button
                        type="button"
                        onClick={() => updatePaginationCursor(true)}
                    >
                        <span>
                            Next
                            <IconNavigateNext className="text-middle" />
                        </span>
                    </Button>
                )}
            </ButtonGroup>
        );
    };

    return (
        <div className="grid-container margin-bottom-10">
            <div className="grid-col-12">
                <h2>Submissions</h2>
                <NextPrevButtonsComponent />
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
                        {getSortedSubmissions()
                            // failed submissions will not have an id. do not display them.
                            .filter((s) => s.isSuccessSubmitted())
                            .map((s) => {
                                return (
                                    <tr key={s.pk()}>
                                        <th scope="row">
                                            <NavLink
                                                to={`/submissions/${s.taskId}`}
                                            >
                                                {new Date(
                                                    s.createdAt
                                                ).toLocaleString()}
                                            </NavLink>
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
                {submissions?.length === 0 && !paginationCursor && (
                    <p>There were no results found.</p>
                )}
                {submissions?.length === 0 && paginationCursor && (
                    <p>No more results found.</p>
                )}
                <NextPrevButtonsComponent />
            </div>
        </div>
    );
}

export default SubmissionsTable;
