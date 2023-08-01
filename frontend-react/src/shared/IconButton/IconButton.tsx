import { Button } from "@trussworks/react-uswds";
import classnames from "classnames";

import styles from "./IconButton.module.scss";

export type ButtonProps = React.ComponentProps<typeof Button>;

export interface IconButtonProps extends ButtonProps {}

export const IconButton = ({
    children,
    className,
    ...props
}: ButtonProps & JSX.IntrinsicElements["button"]): React.ReactElement => {
    const classes = classnames(styles["usa-icon-button"], className);
    return (
        <Button {...props} className={classes}>
            {children}
        </Button>
    );
};
