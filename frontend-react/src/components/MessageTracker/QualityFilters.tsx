import React from "react";

import { Destination } from "../../resources/ActionDetailsResource";

//TODO: do we want to create a new interface here or use the existing Destination?
type QualityFiltersDisplayProps = {
    qualityFilters: Destination[] | undefined;
};

export const QualityFilters = ({
    qualityFilters,
}: QualityFiltersDisplayProps) => {
    return (
        <>
            <section className="margin-bottom-5">
                <h3>Jurisdictions:</h3>
                {qualityFilters?.map((d) => (
                    <React.Fragment key={d.organization_id}>
                        <table
                            className="usa-table usa-table--borderless usa-table--striped width-full"
                            data-testid="qualityFilter-table"
                        >
                            <thead>
                                <tr className="text-baseline">
                                    <th>
                                        {d.organization} <br />
                                        <span className="font-sans-3xs text-normal">
                                            {" "}
                                            ({d.filteredReportItems.length})
                                            record(s) filtered out
                                        </span>
                                    </th>
                                    <th>Field</th>
                                    <th>Tracking element</th>
                                </tr>
                            </thead>
                            <tbody className="font-body-xs">
                                {d.filteredReportItems.map((f, i) => {
                                    return (
                                        <tr key={i}>
                                            <td> {f.message}</td>
                                            <td> {f.filterName}</td>
                                            <td>
                                                {" "}
                                                {f.filteredTrackingElement}
                                            </td>
                                        </tr>
                                    );
                                })}
                            </tbody>
                        </table>
                    </React.Fragment>
                ))}
            </section>
        </>
    );
};
