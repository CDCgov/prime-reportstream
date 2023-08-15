// AutoUpdateFileChromatic
import React from "react";
import { StoryFn, Meta } from "@storybook/react";

import { Table } from "./Table";

export default {
    title: "Components/Table",
    component: Table,
} as Meta<typeof Table>;

const defaultData = [
    [
        {
            columnKey: "Shape",
            columnHeader: "Shape",
            content: "Triangle",
        },
        {
            columnKey: "Sides",
            columnHeader: "Sides",
            content: "3",
        },
        {
            columnKey: "NoteSection",
            columnHeader: "Note Section",
            content:
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
        },
    ],
    [
        {
            columnKey: "Shape",
            columnHeader: "Shape",
            content: "Square",
        },
        {
            columnKey: "Sides",
            columnHeader: "Sides",
            content: "4",
        },
        {
            columnKey: "NoteSection",
            columnHeader: "Note Section",
            content:
                "Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo.",
        },
    ],
    [
        {
            columnKey: "Shape",
            columnHeader: "Shape",
            content: "Pentagon",
        },
        {
            columnKey: "Sides",
            columnHeader: "Sides",
            content: "5",
        },
        {
            columnKey: "NoteSection",
            columnHeader: "Note Section",
            content:
                "Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit laboriosam, nisi ut aliquid ex ea commodi consequatur?",
        },
    ],
    [
        {
            columnKey: "Shape",
            columnHeader: "Shape",
            content: "Hexagon",
        },
        {
            columnKey: "Sides",
            columnHeader: "Sides",
            content: "6",
        },
        {
            columnKey: "NoteSection",
            columnHeader: "Note Section",
            content: "Nisi ut aliquid ex ea commodi consequatur!!",
        },
    ],
];

export const Borderless: StoryFn<typeof Table> = () => {
    return <Table borderless rowData={defaultData} />;
};

export const Bordered: StoryFn<typeof Table> = () => {
    return <Table rowData={defaultData} />;
};

export const Striped: StoryFn<typeof Table> = () => {
    return <Table striped rowData={defaultData} />;
};

export const Scrollable: StoryFn<typeof Table> = () => {
    return <Table scrollable rowData={defaultData} />;
};

export const Sortable: StoryFn<typeof Table> = () => {
    return <Table sortable rowData={defaultData} />;
};
