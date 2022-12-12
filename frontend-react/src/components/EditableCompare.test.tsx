import { fireEvent, render, screen } from "@testing-library/react";
import { Ref, useRef } from "react";
import { renderHook } from "@testing-library/react-hooks";

import { EditableCompare, EditableCompareRef } from "./EditableCompare";

describe("EditableCompare", () => {
    const { result } = renderHook<undefined, Ref<EditableCompareRef>>(() => {
        return useRef<EditableCompareRef>(null);
    });
    const diffEditorRef = result.current;
    const leftJson = JSON.stringify(
        {
            createdBy: "local@test.com",
            description: "Abbott",
        },
        null,
        2
    );

    test("diff with incorrect JSON format", () => {
        const rightJson = JSON.stringify(
            {
                createdAt: "2022-09-22",
                createdBy: "local@test.com",
                description: "Abbott",
                filter: [1, 2],
            },
            null,
            2
        );
        render(
            <EditableCompare
                ref={diffEditorRef}
                original={leftJson}
                modified={rightJson}
                jsonDiffMode={true}
            />
        );

        const leftCompare = screen.getByTestId("left-compare-text");
        const leftMarkText = screen.getByTestId("left-mark-text");

        expect(leftCompare.innerHTML).toEqual(`{
  "createdBy": "local@test.com",
  "description": "Abbott"
}`);
        expect(leftMarkText.innerHTML).toEqual(`{
  "createdBy": "local@test.com",
  "description": "Abbott"
}`);

        const rightCompare = screen.getByTestId("right-compare-text");
        const rightMarkText = screen.getByTestId("right-mark-text");
        expect(rightCompare.innerHTML).toEqual(`{
  "createdAt": "2022-09-22",
  "createdBy": "local@test.com",
  "description": "Abbott",
  "filter": [
    1,
    2
  ]
}`);
        expect(rightMarkText.innerHTML).toEqual(`{
  <mark>"createdAt": "2022-09-22"</mark>,
  "createdBy": "local@test.com",
  "description": "Abbott",
  <mark>"filter": [
    <mark>1</mark>,
    <mark>2</mark>
  ]</mark>
}<br>`);
    });

    test("highlight enables/disables accordingly", () => {
        const rightJson = JSON.stringify(
            {
                countyName: "",
                createdBy: "local@test.com",
                description: "Abbott",
                stateCode: "",
            },
            null,
            2
        );
        render(
            <EditableCompare
                ref={diffEditorRef}
                original={leftJson}
                modified={rightJson}
                jsonDiffMode={true}
            />
        );

        const leftCompare = screen.getByTestId("left-compare-text");
        const leftMarkText = screen.getByTestId("left-mark-text");

        expect(leftCompare.innerHTML).toEqual(`{
  "createdBy": "local@test.com",
  "description": "Abbott"
}`);
        expect(leftMarkText.innerHTML).toEqual(`{
  "createdBy": "local@test.com",
  "description": "Abbott"
}`);

        const rightCompare = screen.getByTestId("right-compare-text");
        const rightMarkText = screen.getByTestId("right-mark-text");
        expect(rightCompare.innerHTML).toEqual(`{
  "countyName": "",
  "createdBy": "local@test.com",
  "description": "Abbott",
  "stateCode": ""
}`);
        expect(rightMarkText.innerHTML).toEqual(`{
  <mark>"countyName": ""</mark>,
  "createdBy": "local@test.com",
  "description": "Abbott",
  <mark>"stateCode": ""</mark>
}<br>`);

        fireEvent.change(rightCompare, { target: { value: leftJson } });
        expect(rightCompare.innerHTML).toEqual(`{
  "createdBy": "local@test.com",
  "description": "Abbott"
}`);
        expect(rightCompare.innerHTML).toEqual(`{
  "createdBy": "local@test.com",
  "description": "Abbott"
}`);
    });
});
