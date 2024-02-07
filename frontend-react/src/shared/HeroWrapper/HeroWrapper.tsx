import classNames from "classnames";
import { PropsWithChildren, HTMLAttributes } from "react";

import styles from "./HeroWrapper.module.scss";

export interface HeroWrapperProps
    extends PropsWithChildren<HTMLAttributes<HTMLDivElement>> {
    isAlternate?: boolean;
}

function HeroWrapper({ isAlternate, ...props }: HeroWrapperProps) {
    const classnames = classNames(
        styles["rs-hero-wrapper"],
        isAlternate && styles["rs-hero-wrapper--alternate"],
        props.className,
    );
    return <div {...props} className={classnames} />;
}

export default HeroWrapper;
