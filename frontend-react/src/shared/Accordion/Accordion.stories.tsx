// AutoUpdateFileChromatic
import React from "react";
import { AccordionItemProps } from "@trussworks/react-uswds/lib/components/Accordion/Accordion";

import Accordion from "./Accordion";

export default {
    title: "components/Accordion",
    component: Accordion,
};

const testItems: AccordionItemProps[] = [
    {
        title: "First Amendment",
        content: (
            <p>
                Congress shall make no law respecting an establishment of
                religion, or prohibiting the free exercise thereof; or abridging
                the freedom of speech, or of the press; or the right of the
                people peaceably to assemble, and to petition the Government for
                a redress of grievances.
            </p>
        ),
        expanded: false,
        id: "123",
        headingLevel: "h4",
    },
    {
        title: "Second Amendment",
        content: (
            <>
                <p>
                    A well regulated Militia, being necessary to the security of
                    a free State, the right of the people to keep and bear Arms,
                    shall not be infringed.
                </p>{" "}
                <ul>
                    <li>This is a list item</li>
                    <li>Another list item</li>
                </ul>
            </>
        ),
        expanded: false,
        id: "abc",
        headingLevel: "h4",
    },
    {
        title: "Third Amendment",
        content: (
            <p>
                No Soldier shall, in time of peace be quartered in any house,
                without the consent of the Owner, nor in time of war, but in a
                manner to be prescribed by law.
            </p>
        ),
        expanded: false,
        id: "def",
        headingLevel: "h4",
    },
    {
        title: "Fourth Amendment",
        content: (
            <p>
                The right of the people to be secure in their persons, houses,
                papers, and effects, against unreasonable searches and seizures,
                shall not be violated, and no Warrants shall issue, but upon
                probable cause, supported by Oath or affirmation, and
                particularly describing the place to be searched, and the
                persons or things to be seized.
            </p>
        ),
        expanded: false,
        id: "456",
        headingLevel: "h4",
    },
];

export const Default = {
    args: {
        items: testItems,
    },
};
export const Alternate = {
    args: {
        ...Default.args,
        isAlternate: true,
    },
};
