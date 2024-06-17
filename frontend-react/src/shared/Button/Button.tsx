import { Button as OrigButton } from "@trussworks/react-uswds";
import classnames from "classnames";
import {
    ComponentProps,
    PropsWithChildren,
    ReactElement,
    ReactNode,
} from "react";

export type USWDSButtonProps = ComponentProps<typeof OrigButton>;

export interface ButtonProps
    extends PropsWithChildren<Omit<USWDSButtonProps, "children">> {
    icon?: ReactNode;
}

export const Button = ({
    children,
    className,
    icon,
    ...props
}: ButtonProps): ReactElement => {
    const classes = classnames(icon && "usa-icon-button", className);
    return (
        <OrigButton {...props} className={classes}>
            {children}
            {icon && <> {icon}</>}
        </OrigButton>
    );
};
