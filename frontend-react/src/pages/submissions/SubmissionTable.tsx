import { useContext } from "react";
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
import usePaginator from "../../utils/UsePaginator";

import { SubmissionFilterContext } from "./FilterContext";

function SubmissionTable() {
    // this component will refresh when global context changes (e.g. organization changes)
    const globalState = useContext(GlobalContext);
    const { filters, updateStartRange, updateEndRange, updateSortOrder } =
        useContext(SubmissionFilterContext);

    const submissions: SubmissionsResource[] = useResource(
        SubmissionsResource.list(),
        {
            organization: globalState.state.organization,
            cursor: filters.startRange,
            endCursor: filters.endRange,
            pageSize: filters.pageSize + 1, // Pulls +1 to check for next page
            sort: filters.sortOrder,
            showFailed: false, // No plans for this to be set to true
        }
    );

    /* Gives handlers for all pagination needs!
     *
     * - hasPrev/hasNext: boolean, indicating previous and next pages exist.
     * - currentIndex: number, the page number you're currently on.
     * - changeCursor: function(desiredCursorIndex), handles cursor navigation and updating.
     * - pageCount: function(), returns the current size of your cursor Map
     */
    const { hasPrev, hasNext, currentIndex, changeCursor, pageCount } =
        usePaginator(submissions, filters.pageSize, updateStartRange!!);

    const NextPrevButtonsComponent = () => {
        // on both the first and last page if there's only one page of data
        if (pageCount() <= 1) return <></>;
        return (
            <ButtonGroup type="segmented" className="float-right margin-top-5">
                {hasPrev && (
                    <Button
                        type="button"
                        onClick={() => changeCursor(currentIndex - 1)}
                    >
                        <span>
                            <IconNavigateBefore className="text-middle" />
                            Previous
                        </span>
                    </Button>
                )}
                {hasNext && (
                    <Button
                        type="button"
                        onClick={() => changeCursor(currentIndex + 1)}
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
                        {submissions.map((s, i) => {
                            if (i === filters.pageSize) return null
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
                {submissions?.length === 0 ? (
                    <p>There were no results found.</p>
                ) : null}
                <NextPrevButtonsComponent />
            </div>
        </div>
    );
}

export default SubmissionTable;
