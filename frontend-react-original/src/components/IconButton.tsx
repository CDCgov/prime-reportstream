import { Button } from "@trussworks/react-uswds";
import classnames from "classnames";
import { ReactElement, ReactNode } from "react";

// Can't seem to import these directly from react-uswds? Copied here instead.
export interface ButtonProps {
    type: "button" | "submit" | "reset";
    children: ReactNode;
    secondary?: boolean;
    base?: boolean;
    /**
     * @deprecated since 1.15.0, use accentStyle
     */
    accent?: boolean;
    accentStyle?: "cool" | "warm";
    outline?: boolean;
    inverse?: boolean;
    size?: "big";
    /**
     * @deprecated since 1.6.0, use size
     */
    big?: boolean;
    /**
     * @deprecated since 1.6.0, use size
     */
    small?: boolean;
    /**
     * @deprecated since 1.9.0
     */
    icon?: boolean;
    unstyled?: boolean;
}

export type IconButtonProps = ButtonProps;

export const IconButton = ({
    children,
    className,
    ...props
}: ButtonProps & JSX.IntrinsicElements["button"]): ReactElement => {
    const classes = classnames("usa-icon-button", className);
    return (
        <Button {...props} className={classes}>
            {children}
        </Button>
    );
};
