import React from "react";

import { QualityFilter } from "../../config/endpoints/messageTracker";

type QualityFiltersDisplayProps = {
    qualityFilters: QualityFilter[] | undefined;
};

export const QualityFilters = ({
    qualityFilters,
}: QualityFiltersDisplayProps) => {
    return (
        <>
            <section className="margin-bottom-5">
                <h3>Quality Filters:</h3>
                <table
                    className="usa-table usa-table--borderless usa-table--striped width-full"
                    data-testid="qualityFilter-table"
                >
                    <tbody className="font-body-xs">
                        {qualityFilters?.map((q, i) => {
                            return (
                                <tr key={"quality_filter_" + i}>
                                    <td>{q.detail.message}</td>
                                </tr>
                            );
                        })}
                    </tbody>
                </table>
            </section>
        </>
    );
};
