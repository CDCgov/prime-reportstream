import { Accordion as OrigAccordion } from "@trussworks/react-uswds";
import { useId } from "react";
import { useLocation } from "react-router";

export interface AccordionProps
    extends React.ComponentProps<typeof OrigAccordion> {}

/**
 * Enhanced Accordion proxy.
 *
 * Will automatically expand item matching hash name in url.
 * Creates a unique accordion key from a unique id + location key
 * in order to force remounting when a new hash is selected (
 * original accordion does not honor changes to item expand
 * property).
 */
export function Accordion({ items, ...props }: AccordionProps) {
    const location = useLocation();
    const id = useId();
    const hashId = location.hash.replace("#", "");
    const finalItems = items.map((i) => ({
        ...i,
        expanded: i.id === hashId,
    }));
    return (
        <OrigAccordion
            key={`${id}-${location.key}`}
            {...props}
            items={finalItems}
        />
    );
}

export default Accordion;
