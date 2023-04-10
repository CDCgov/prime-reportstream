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
                        Lorem ipsum dolor sit amet, consectetur adipiscing elit,
                        sed do eiusmod tempor incididunt ut labore et dolore
                        magna aliqua.
                    </td>
                </tr>
                <tr>
                    <td>Pentagon</td>
                    <td>5</td>
                    <td>
                        Lorem ipsum dolor sit amet, consectetur adipiscing elit,
                        sed do eiusmod tempor incididunt ut labore et dolore
                        magna aliqua.
                    </td>
                </tr>
            </tbody>
        </>
    );
}

export const Borderless: ComponentStory<typeof Table> = (args) => {
    return (
        <Table borderless>
            <DefaultTableContent />
        </Table>
    );
};

export const Bordered: ComponentStory<typeof Table> = (args) => {
    return (
        <Table>
            <DefaultTableContent />
        </Table>
    );
};

export const Striped: ComponentStory<typeof Table> = (args) => {
    return (
        <Table striped>
            <DefaultTableContent />
        </Table>
    );
};

export const Scrollable: ComponentStory<typeof Table> = (args) => {
    return (
        <Table scrollable>
            <DefaultTableContent />
        </Table>
    );
};
