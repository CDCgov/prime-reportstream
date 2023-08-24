import classNames from "classnames";

import styles from "./CardWrapper.module.scss";

export interface CardWrapperProps
    extends React.PropsWithChildren<React.HTMLAttributes<HTMLDivElement>> {
    isAlternate?: boolean;
}

export function CardWrapper({ isAlternate, ...props }: CardWrapperProps) {
    const classnames = classNames(
        isAlternate && styles["rs-hero-wrapper--alternate"],
        props.className,
    );
    return <div {...props} className={classnames} />;
}

export default CardWrapper;
