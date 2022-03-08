import React, { useEffect } from "react";

/* 
    This is the base interface that all object arrays passed to
    PaginatedTable must extend. You can add n-many fields with
    an optional transform Map that'll let you tell the table
    how to transform data before displaying.
*/
interface TableRow {
    [key: string]: any;
    fieldA: string;
    fieldB: number;
    transform?: Map<string, Function>;
}

interface PaginatedTableProps<T extends TableRow> {
    objects: Array<T>;
    headers: Array<string>;
}

/*
    Provided an array of objects of typeof TestObj, this table will
    render capitalized headers and optionally-transformed data.
*/
const PaginatedTable = (props: PaginatedTableProps<TableRow>) => {

    useEffect(() => {
        transformData()
    }, [props.objects])
    

    const transformData = (): void => {
        // ex: { a: 1, b: 2, transform: Map([['b', addOne]])} ->
        //     { a: 1, b: 3, transform: Map([['b', addOne]])}
        props.objects.forEach((obj) => {
            if (obj.transform) {
                obj.transform.forEach((transform, key)=>{
                    obj[key] = transform(obj[key])
                })                
            }
        });
    }

    const transformHeaders = (): string[] => {
        const newHeaders: Array<string> = []
        props.objects.forEach(obj => {
            Object.entries(obj).forEach(key =>
                newHeaders.push(key[0].toUpperCase() + key.slice(1))
            )
        })
        return newHeaders
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
