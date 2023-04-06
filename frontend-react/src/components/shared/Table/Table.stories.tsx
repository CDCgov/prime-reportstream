import React from "react";
import { ComponentMeta, ComponentStory } from "@storybook/react";

import { Table } from "./Table";

export default {
    title: "components/Table",
    component: Table,
} as ComponentMeta<typeof Table>;

export const Default: ComponentStory<typeof Table> = (args) => (
    <Table>
        <thead></thead>
        <tbody></tbody>
    </Table>
);
