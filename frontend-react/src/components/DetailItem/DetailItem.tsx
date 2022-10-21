import React from "react";

type DetailItemProps = {
    item: string;
    content: any;
};

/*
    A component displaying a soft gray title and content in
    standard black text.

    @param item - the title of a property; e.g. Report ID
    @param content - the content of a property; e.g. 000000-0000-0000-000000
*/
export function DetailItem({ item, content }: DetailItemProps) {
    return (
        <div className="display-flex flex-column margin-bottom-2">
            <span className="text-base">{item}</span>
            <span>{content}</span>
        </div>
    );
}
