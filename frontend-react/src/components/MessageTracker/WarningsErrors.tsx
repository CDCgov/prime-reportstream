import React from "react";

// TODO: move this interface into the resources directory
interface Warnings {
    field: string | undefined;
    description: string | undefined;
    type: string | undefined;
    trackingIds: string[] | undefined;
}

type WarningsErrorsDisplayProps = {
    title: string;
    data: Warnings[];
};

export const WarningsErrors = ({ title, data }: WarningsErrorsDisplayProps) => {
    return (
        <>
            <section className="margin-bottom-5">
                <h3>{title}</h3>
                <table
                    className="usa-table usa-table--borderless usa-table--striped width-full"
                    data-testid="warning-error-table"
                >
                    <thead>
                        <tr>
                            <th>Field</th>
                            <th>Description</th>
                            <th>Type</th>
                            <th>Tracking ID</th>
                        </tr>
                    </thead>
                    <tbody className="font-body-xs">
                        {data.map((d, i) => {
                            return (
                                <Row
                                    data={d}
                                    index={i}
                                    key={`warning_error${i}`}
                                />
                            );
                        })}
                    </tbody>
                </table>
            </section>
        </>
    );
};

interface RowProps {
    data: Warnings;
    index: number;
}

const Row = ({ data, index }: RowProps) => {
    const { field, description, type, trackingIds } = data;
    return (
        <tr key={"warning_error_" + index}>
            <td>{field}</td>
            <td>{description}</td>
            <td>{type}</td>
            <td>
                {trackingIds?.length && trackingIds.length > 0 && (
                    <span>{trackingIds.join(", ")}</span>
                )}
            </td>
        </tr>
    );
};
