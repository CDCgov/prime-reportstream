import { useContext } from "react";
import {
    Button,
    ButtonGroup,
    IconNavigateBefore,
    IconNavigateNext,
} from "@trussworks/react-uswds";
import { NavLink } from "react-router-dom";

import { PaginationController } from "../../utils/UsePaginator";

import { SubmissionFilterContext } from "./FilterContext";

/* Handles pagination button logic and display */
function PaginationButtons({ paginator }: { paginator: PaginationController }) {
    return (
        <ButtonGroup type="segmented" className="float-right margin-top-5">
            {paginator.hasPrev && (
                <Button
                    type="button"
                    onClick={() =>
                        paginator.changeCursor(paginator.currentIndex - 1)
                    }
                >
                    <span>
                        <IconNavigateBefore className="text-middle" />
                        Previous
                    </span>
                </Button>
            )}
            {paginator.hasNext && (
                <Button
                    type="button"
                    onClick={() =>
                        paginator.changeCursor(paginator.currentIndex + 1)
                    }
                >
                    <span>
                        Next
                        <IconNavigateNext className="text-middle" />
                    </span>
                </Button>
            )}
        </ButtonGroup>
    );
}

function SubmissionTable() {
    /* The one-stop-shop for all things filters, pagination, and API responses! */
    const { filters, paginator, contents } = useContext(
        SubmissionFilterContext
    );

    /* Handles pagination button logic and display */

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
                        {contents.map((s, i) => {
                            // Do not render the additional item pulled for measuring
                            // whether the next page exists.
                            if (i === filters.pageSize) return null;
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
                {contents?.length === 0 ? (
                    <p>There were no results found.</p>
                ) : null}
                {paginator ? <PaginationButtons paginator={paginator} /> : null}
            </div>
        </div>
    );
}

export default SubmissionTable;
