import { Accordion as OrigAccordion } from "@trussworks/react-uswds";
import { useId } from "react";
import { useLocation } from "react-router";

/**
 * Ensure our accordion urls being added to history is only ever
 * page + 1 by utilizing a property in history state.
 *
 * Ex: User goes to /support page and interacts with multiple accordion items.
 * Top of history will be: /support, /support#lastAccordionItemOpened. User can
 * press back to go to /support and then navigate elsewhere to naturally remove
 * the hash link from history. This example doesn't take into account if the user
 * clicks any hash links on the page, in which case the history will include all
 * links clicked (default browser behavior) + last accordion item opened.
 */
function appendHashToURL(hash: string) {
    const currentURL = window.location.href;
    const newURL = currentURL.split("#")[0] + hash; // Remove any existing anchor and append the new one
    if (window.history.state.isAccordion) {
        window.history.replaceState(window.history.state, "", newURL);
    } else {
        window.history.pushState({ isAccordion: true }, "", newURL);
    }
}

export interface AccordionProps
    extends React.ComponentProps<typeof OrigAccordion> {}

/**
 * Enhanced Accordion proxy.
 *
 * Will automatically wrap titles with provided id for item,
 * while replacing the id used for the content (so that url
 * hash scrolling will go to title).
 *
 * Will automatically expand item matching hash name in url.
 * Creates a unique accordion key from a unique id + location key
 * in order to force remounting when a new hash is selected (
 * original accordion does not honor changes to item expand
 * property).
 *
 * Will also update history will the last accordion item
 * id interacted with.
 */
export function Accordion({ items, ...props }: AccordionProps) {
    const location = useLocation();
    const id = useId();
    const hashId = location.hash.replace("#", "");
    const finalItems = items.map((i) => ({
        ...i,
        id: `${i.id}--content`,
        title: <span id={i.id}>{i.title}</span>,
        expanded: i.id === hashId,
    }));
    return (
        <div
            onClick={(event) => {
                // TODO: Figure out how to get event.target to return proper type
                const target = event.target as HTMLElement;
                // Depending on DOM tree structure inside button, target will differ
                // depending on area clicked. Check if the target itself has an id,
                // otherwise try to find a descendant with one.
                const id =
                    target.getAttribute("id") ??
                    target.querySelector("[id]")?.id;

                if (id) {
                    appendHashToURL(`#${id}`);
                }
            }}
        >
            <OrigAccordion
                key={`${id}-${location.key}`}
                {...props}
                items={finalItems}
            />
        </div>
    );
}

export default Accordion;
