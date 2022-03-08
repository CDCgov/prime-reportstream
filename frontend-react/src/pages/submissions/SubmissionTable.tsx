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

import { SubmissionFilterContext } from "./SubmissionContext";

function SubmissionTable() {
    // this component will refresh when global context changes (e.g. organization changes)
    const globalState = useContext(GlobalContext);
    const { filters, updateStartRange, updateSortOrder } = useContext(
        SubmissionFilterContext
    );

    // we can tell if we're on the first page by saving the first result and then checking against it later
    const [firstPaginationCursor, setFirstPaginationCursor] = useState("");
    const [atLeastOneEntry, setAtLeastOneEntry] = useState(false);
    const [moreThanOnePage, setMoreThanOnePage] = useState(false);

    const submissions: SubmissionsResource[] = useResource(
        SubmissionsResource.list(),
        {
            organization: globalState.state.organization,
            cursor: filters.startRange,
            endCursor: filters.endRange,
            pageSize: filters.pageSize,
            sort: filters.sortOrder,
            showFailed: false, // No plans for this to be set to true
        }
    );

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
        setMoreThanOnePage(submissions.length >= filters.pageSize);
    }, [atLeastOneEntry, filters.pageSize, submissions]);

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
        updateStartRange!!(cursor);
        updateSortOrder!!(sort);
    };

    const onFirstPage = () => {
        // do any elements match the current cursor?
        return submissions?.find((s) => s.createdAt === firstPaginationCursor);
    };

    // 2022-01-07: the team suggested to see if the number of items matches
    // the requested page size to tell if it is the last page,
    // and then leave a message if there are no results on the next page
    const onLastPage = () => {
        return submissions?.length !== filters.pageSize;
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
                <table
                    className="usa-table usa-table--borderless prime-table"
                    aria-label="Submission history from the last 30 days"
                >
                    <thead>
                        <tr>
                            <th scope="col">Report ID</th>
                            <th scope="col">Date/time submitted</th>
                            <th scope="col">File</th>
                            <th scope="col">Records</th>
                            <th scope="col">Status</th>
                        </tr>
                    </thead>
                    <tbody id="tBody" className="font-mono-2xs">
                        {getSortedSubmissions().map((s) => {
                            return (
                                <tr key={s.pk()}>
                                    <th scope="row">
                                        <NavLink
                                            to={`/submissions/${s.taskId}`}
                                        >
                                            {s.id}
                                        </NavLink>
                                    </th>
                                    {/* File name */}
                                    <th scope="row">
                                        {new Date(s.createdAt).toLocaleString()}
                                    </th>
                                    <th scope="row">{s.externalName}</th>
                                    <th scope="row">{s.reportItemCount}</th>
                                    <th scope="row">{"Success"}</th>
                                </tr>
                            );
                        })}
                    </tbody>
                </table>
                {submissions?.length === 0 && !atLeastOneEntry && (
                    <p>There were no results found.</p>
                )}
                {submissions?.length === 0 && atLeastOneEntry && (
                    <p>No more results found.</p>
                )}
                <NextPrevButtonsComponent />
            </div>
        </div>
    );
}

export default SubmissionTable;
