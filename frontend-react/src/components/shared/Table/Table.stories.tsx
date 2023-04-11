import React from "react";
import { ComponentMeta, ComponentStory } from "@storybook/react";

import { Table } from "./Table";

export default {
    title: "components/Table",
    component: Table,
} as ComponentMeta<typeof Table>;

const defaultData = [
    [
        {
            columnKey: "Shape",
            content: "Triangle",
        },
        {
            columnKey: "Sides",
            content: "3",
        },
        {
            columnKey: "Note",
            content:
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
        },
    ],
    [
        {
            columnKey: "Shape",
            content: "Square",
        },
        {
            columnKey: "Sides",
            content: "4",
        },
        {
            columnKey: "Note",
            content:
                "Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo.",
        },
    ],
    [
        {
            columnKey: "Shape",
            content: "Pentagon",
        },
        {
            columnKey: "Sides",
            content: "5",
        },
        {
            columnKey: "Note",
            content:
                "Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit laboriosam, nisi ut aliquid ex ea commodi consequatur?",
        },
    ],
];

export const Borderless: ComponentStory<typeof Table> = () => {
    return <Table borderless rowData={defaultData} />;
};

export const Bordered: ComponentStory<typeof Table> = () => {
    return <Table rowData={defaultData} />;
};

export const Striped: ComponentStory<typeof Table> = () => {
    return <Table striped rowData={defaultData} />;
};

export const Scrollable: ComponentStory<typeof Table> = () => {
    return <Table scrollable rowData={defaultData} />;
};

export const Sortable: ComponentStory<typeof Table> = () => {
    return <Table sortable rowData={defaultData} />;
};
