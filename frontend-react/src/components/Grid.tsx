import classNames from "classnames";

export interface GridRowProps {
    children: React.ReactNode;
    className?: string;
}

export function GridRow({ children, className }: GridRowProps) {
    return <div className={classNames("grid-row", className)}>{children}</div>;
}
