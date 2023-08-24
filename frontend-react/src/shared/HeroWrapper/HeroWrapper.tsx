import classNames from "classnames";

import styles from "./HeroWrapper.module.scss";

export interface HeroWrapperProps
    extends React.PropsWithChildren<React.HTMLAttributes<HTMLDivElement>> {
    isAlternate?: boolean;
}

export function HeroWrapper({ isAlternate, ...props }: HeroWrapperProps) {
    const classnames = classNames(
        styles["rs-hero-wrapper"],
        isAlternate && styles["rs-hero-wrapper--alternate"],
        props.className,
    );
    return <div {...props} className={classnames} />;
}

export default HeroWrapper;
