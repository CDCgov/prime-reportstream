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

import { FilterState, SubmissionFilterContext } from "./FilterContext";

function SubmissionTable() {
    // this component will refresh when global context changes (e.g. organization changes)
    const globalState = useContext(GlobalContext);
    const { filters, updateStartRange, updateEndRange, updateSortOrder } =
        useContext(SubmissionFilterContext);

    /*    Pagination behavior
     * 1. Load page. Start range filter is used to dictate first cursor
     * 2. On response, store cursor + nextCursor in Map<number, string> with 1-indexing to mimic page numbers
     * 3. When updateStartRange is called, conditionally update Map with new cursor at new index
     * 4. When paging back, use the Map to retrieve the previous cursor and set it
     * 5. When paging forward, use the Map to retrieve the next cursor and set it
     * 6. When calling a page, the cursor should be set to currentIndex cursor and endCursor should be set
     *    to currentIndex + 1 cursor
     */

    // Initialized cursors with first cursor as blank so page 1 always has the newest items
    // displayed.
    const [cursors, updateCursors] = useState<Map<number, string>>(
        new Map([[1, ""]])
    );
    const [currentIndex, updateCurrentIndex] = useState<number>(1);
    const [hasNext, setHasNext] = useState(false);
    const [hasPrev, setHasPrev] = useState(false);

    /* NOT meant to show total page count, only page count "so far" */
    const pageCount = () => {
        return cursors.size;
    };
    const changeCursor = (cursorIndex: number) => {
        let cursor =
            cursors.get(cursorIndex) || cursors.get(currentIndex) || null;
        if (cursorIndex === 1 || !cursor) {
            /* Cursor 1 will always have the value of "" so that page 1 always has
             * the absolute latest results pulled. However, this will return null
             * if called cursors.get(1), so this check manually sets the cursor to
             * an empty string.
             *
             * For this, we also must reset the Map so previously tracked
             * cursors are lost and new cursors can be stored.
             */
            cursor = "";
            updateCursors(new Map([[1, cursor]]));
            updateCurrentIndex(cursorIndex);
        } else {
            if (cursor === cursors.get(cursorIndex))
                updateCurrentIndex(cursorIndex);
        }

        updateStartRange!!(cursor);
        // updateEndRange!!(endCursor)
    };

    // we can tell if we're on the first page by saving the first result and then checking against it later
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

    // after the first page loads, set up some state info about the data.
    useEffect(() => {
        const cursorExists = (c: string) => {
            return Array.from(cursors.values()).includes(c);
        };
        const addCursors = (nextCursor: string) => {
            updateCursors(cursors.set(currentIndex + 1, nextCursor));
        };
        const updatePrevNextBools = () => {
            setHasNext(currentIndex < cursors.size);
            setHasPrev(currentIndex > 1);
        };

        const lastTimestamp =
            submissions[filters.pageSize - 1]?.createdAt || null;
        if (lastTimestamp && !cursorExists(lastTimestamp))
            addCursors(lastTimestamp);
        updatePrevNextBools();

        debugger;
    }, [
        currentIndex,
        updateCurrentIndex,
        cursors,
        submissions,
        filters.pageSize,
    ]);

    const getSortedSubmissions = (): SubmissionsResource[] => {
        submissions?.sort((a, b) => SubmissionsResource.sortByCreatedAt(a, b));
        return submissions || [];
    };

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
                {submissions?.length === 0 ? (
                    <p>There were no results found.</p>
                ) : null}
                <NextPrevButtonsComponent />
            </div>
        </div>
    );
}

export default SubmissionTable;
