import classNames from "classnames";
import { Accordion as OrigAccordion } from "@trussworks/react-uswds";

import styles from "./Accordion.module.scss";

export interface AccordionProps
    extends React.ComponentProps<typeof OrigAccordion> {
    isAlternate?: boolean;
}

export function Accordion({
    isAlternate,
    className,
    ...props
}: AccordionProps) {
    const classnames = classNames(
        isAlternate && styles["usa-accordion--alternate"],
        className,
    );
    return <OrigAccordion className={classnames} {...props} />;
}

export default Accordion;
