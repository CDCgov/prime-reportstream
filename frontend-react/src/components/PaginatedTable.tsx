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

    // function here() {
    //     props.objects.forEach((obj) => {
    //         console.log(obj.fieldA);
    //         for (let entry of obj.transform.entries())
    //         {
    //             console.log(entry)
    //         }
    //     });
    // }
    //
    // const extractHeaders = () => {
    //     // Return transformed (capitalized first letter) headers
    // }
    //
    // const transformData = () => {
    //     // Mutate data using the provided instructions (found in transform map)
    // }

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
