import React from "react";
import { ComponentMeta, ComponentStory } from "@storybook/react";

import { Table } from "./Table";

export default {
    title: "components/Table",
    component: Table,
} as ComponentMeta<typeof Table>;

function DefaultTableContent() {
    return (
        <>
            <thead>
                <tr>
                    <th>Shape</th>
                    <th>Sides</th>
                    <th>Note</th>
                </tr>
            </thead>
            <tbody>
                <tr>
                    <td>Triangle</td>
                    <td>3</td>
                    <td>
                        Lorem ipsum dolor sit amet, consectetur adipiscing elit,
                        sed do eiusmod tempor incididunt ut labore et dolore
                        magna aliqua.
                    </td>
                </tr>
                <tr>
                    <td>Square</td>
                    <td>4</td>
                    <td>
                        Sed ut perspiciatis unde omnis iste natus error sit
                        voluptatem accusantium doloremque laudantium, totam rem
                        aperiam, eaque ipsa quae ab illo inventore veritatis et
                        quasi architecto beatae vitae dicta sunt explicabo.
                    </td>
                </tr>
                <tr>
                    <td>Pentagon</td>
                    <td>5</td>
                    <td>
                        Ut enim ad minima veniam, quis nostrum exercitationem
                        ullam corporis suscipit laboriosam, nisi ut aliquid ex
                        ea commodi consequatur?
                    </td>
                </tr>
            </tbody>
        </>
    );
}

const defaultHeaderContent = ["Shape", "Sides", "Notes"];
const defaultRowContent = [
    [
        "Triangle",
        "3",
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
    ],
    [
        "Square",
        "4",
        "Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo.",
    ],
    [
        "Pentagon",
        "5",
        "Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit laboriosam, nisi ut aliquid ex ea commodi consequatur?",
    ],
];

export const Borderless: ComponentStory<typeof Table> = () => {
    return (
        <Table
            borderless
            columnHeaders={defaultHeaderContent}
            rowData={defaultRowContent}
        />
    );
};

export const Bordered: ComponentStory<typeof Table> = () => {
    return (
        <Table
            columnHeaders={defaultHeaderContent}
            rowData={defaultRowContent}
        />
    );
};

export const Striped: ComponentStory<typeof Table> = () => {
    return (
        <Table
            striped
            columnHeaders={defaultHeaderContent}
            rowData={defaultRowContent}
        />
    );
};

export const Scrollable: ComponentStory<typeof Table> = () => {
    return (
        <Table
            scrollable
            columnHeaders={defaultHeaderContent}
            rowData={defaultRowContent}
        />
    );
};

export const Sortable: ComponentStory<typeof Table> = () => {
    return (
        <Table
            sortable
            columnHeaders={defaultHeaderContent}
            rowData={defaultRowContent}
        />
    );
};
