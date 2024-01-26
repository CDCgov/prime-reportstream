import classNames from "classnames";
import { Card as OrigCard } from "@trussworks/react-uswds";
import { ComponentProps } from "react";

import styles from "./Card.module.scss";

export interface CardProps extends ComponentProps<typeof OrigCard> {
    isAlternate?: boolean;
}

export function Card({ isAlternate, className, ...props }: CardProps) {
    const classnames = classNames(
        isAlternate && styles["usa-card--alternate"],
        className,
    );
    return <OrigCard className={classnames} {...props} />;
}

export default Card;
