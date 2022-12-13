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

    test("onload - diff with incorrect JSON format", () => {
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

    test("onChange - highlight enables/disables accordingly", () => {
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
    });

    test("onChange - dont refresh if originalText or modifiedText are empty", () => {
        const emptyString = "";
        const rightJson = JSON.stringify(
            {
                countyName: "",
            },
            null,
            2
        );
        render(
            <EditableCompare
                ref={diffEditorRef}
                original={emptyString}
                modified={rightJson}
                jsonDiffMode={true}
            />
        );

        const rightCompare = screen.getByTestId("right-compare-text");

        fireEvent.change(rightCompare, { target: { value: emptyString } });
        expect(rightCompare.innerHTML).toEqual(`{
  "countyName": ""
}`);
    });

    test("onBlur - show left highlight if modifiedText has been replaced", () => {
        const rightJson = JSON.stringify(
            {
                createdBy: "local@test.com",
                description: "Abbott",
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

        const rightCompare = screen.getByTestId("right-compare-text");
        const rightMarkText = screen.getByTestId("right-mark-text");
        expect(rightCompare.innerHTML).toEqual(`{
  "createdBy": "local@test.com",
  "description": "Abbott"
}`);
        expect(rightMarkText.innerHTML).toEqual(`{
  "createdBy": "local@test.com",
  "description": "Abbott"
}<br>`);

        fireEvent.change(rightCompare, {
            target: {
                value: `{}`,
            },
        });
        fireEvent.blur(rightCompare);

        expect(leftMarkText.innerHTML).toEqual(`{
  <mark>"createdBy": "local@test.com"</mark>,
  <mark>"description": "Abbott"</mark>
}`);
        expect(rightMarkText.innerHTML).toEqual(`{}<br>`);
    });

    test("onBlur - show error if modifiedText is not valid JSON", () => {
        const rightJson = JSON.stringify(
            {
                createdBy: "local@test.com",
                description: "Abbott",
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

        const rightCompare = screen.getByTestId("right-compare-text");
        const rightMarkText = screen.getByTestId("right-mark-text");
        expect(rightCompare.innerHTML).toEqual(`{
  "createdBy": "local@test.com",
  "description": "Abbott"
}`);

        fireEvent.change(rightCompare, {
            target: {
                value: `This is not JSON`,
            },
        });
        fireEvent.blur(rightCompare);

        expect(leftMarkText.innerHTML).toEqual(`{
  "createdBy": "local@test.com",
  "description": "Abbott"
}`);
        expect(rightMarkText.innerHTML).toEqual(`<s>This</s> is not JSON<br>`);
    });
});
