import React from "react";

// TODO: move this interface into the resources directory
interface ResponseWarning {
    field: string | undefined;
    description: string | undefined;
    type: string | undefined;
    trackingIds: string[] | undefined;
}

type WarningsDisplayProps = {
    title: string;
    warnings: ResponseWarning[];
};

export const WarningsDisplay = ({ title, warnings }: WarningsDisplayProps) => {
    return (
        <>
            <section className="margin-bottom-5">
                <h3>{title}</h3>
                <table
                    className="usa-table usa-table--borderless usa-table--striped width-full"
                    data-testid="warning-table"
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
                        {warnings.map((w, i) => {
                            return (
                                <WarningRow
                                    warning={w}
                                    index={i}
                                    key={`warning${i}`}
                                />
                            );
                        })}
                    </tbody>
                </table>
            </section>
        </>
    );
};

interface WarningRowProps {
    warning: ResponseWarning;
    index: number;
}

const WarningRow = ({ warning, index }: WarningRowProps) => {
    const { field, description, type, trackingIds } = warning;
    return (
        <tr key={"warning_" + index}>
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
