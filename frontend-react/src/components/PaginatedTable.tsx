import React from "react";

interface TestObj {
    fieldA: string;
    fieldB: number;
    transform?: Map<string, Function>
}

interface PaginatedTableProps<T> {
    objects: T[]
}

const PaginatedTable: React.FC<PaginatedTableProps<TestObj>> = (props) => {

    function transformData() {
        // TODO: Instructions for transformData()
        // 1. Iterate through Map.
        //
        // 2. For each item in the transform ma[, use the key to
        //    access the value in the TestObj.
        //
        // 3. Perform transformation function on the value and
        //    overwrite the old value with the transformed value
        //    at the TestObj layer
        //
        // ex: { a: 1, b: 2, transform: Map([['b', addOne]])} ->
        //     { a: 1, b: 3, transform: Map([['b', addOne]])}
        props.objects.forEach((obj) => {
            console.log(obj.fieldA);
            if (obj.transform) {
                for (let entry of obj.transform.entries())
                {
                    console.log(entry)
                }
            }
        });
    }

    const extractHeaders = () => {
        // TODO: Instructions for extractHeaders()
        // I'll add these in later
        // Return transformed (capitalized first letter) headers
    }

    // Pull field names and stylize them
    // FIRST... translate field B with += 1
    // Pull values and populate them
    return (
        <div className="grid-container margin-bottom-10">
            <div className="grid-col-12">
                <table
                    className="usa-table usa-table--borderless prime-table"
                    aria-label="Submission history from the last 30 days"
                >
                    <thead>
                    <tr>
                        {/* Render headers in <th/> */}
                    </tr>
                    </thead>
                    <tbody id="tBody" className="font-mono-2xs">
                        {/* Render transformed objects in <tr/> */}
                    </tbody>
                </table>
            </div>
        </div>
    )
}

export default PaginatedTable
