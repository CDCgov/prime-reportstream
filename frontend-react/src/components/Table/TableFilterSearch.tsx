import { Button, Search } from "@trussworks/react-uswds";
import { FormEvent } from "react";

export interface TableFilterSearch {
    resultLength?: number;
    activeFilters: (string | undefined)[];
}

interface TableFilterSearchProps {
    filterReset: number;
    resetHandler: (e: FormEvent<Element>) => void;
    searchReset: number;
    setFilterReset: React.Dispatch<React.SetStateAction<number>>;
    setSearchTerm: React.Dispatch<React.SetStateAction<string>>;
}

function TableFilterSearch({
    filterReset,
    resetHandler,
    searchReset,
    setFilterReset,
    setSearchTerm,
}: TableFilterSearchProps) {
    const submitHandler = (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setFilterReset(filterReset + 1);
        const searchField = e.currentTarget.elements.namedItem(
            "search",
        ) as HTMLInputElement;
        setSearchTerm(searchField?.value);
    };
    return (
        <div className="margin-bottom-4 padding-left-4">
            <label
                id="search-field-label"
                data-testid="label"
                className="usa-label"
                htmlFor="search-field"
            >
                Search by Report ID or Filename
            </label>
            <div className="usa-hint margin-bottom-105">
                Enter full Report ID or Filename, including file extension when
                applicable.
            </div>
            <div className="display-flex file-search-container">
                <Search
                    key={searchReset}
                    onSubmit={submitHandler}
                    className="margin-right-205"
                />
                <Button
                    onClick={resetHandler}
                    type={"reset"}
                    name="clear-button"
                    unstyled
                >
                    Reset
                </Button>
            </div>
        </div>
    );
}

export default TableFilterSearch;
