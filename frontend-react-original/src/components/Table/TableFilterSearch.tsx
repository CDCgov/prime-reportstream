import { Button, Search } from "@trussworks/react-uswds";
import { FormEvent } from "react";
import { FeatureName } from "../../utils/FeatureName";
import { appInsights } from "../../utils/TelemetryService/TelemetryService";

export interface TableFilterSearch {
    resultLength?: number;
    activeFilters: (string | undefined)[];
}

interface TableFilterSearchProps {
    resetHandler: (e: FormEvent<Element>) => void;
    searchReset: number;
    setSearchTerm: React.Dispatch<React.SetStateAction<string>>;
    resetFilterFields: React.Dispatch<React.FormEvent<HTMLFormElement>>;
}

function TableFilterSearch({
    resetHandler,
    searchReset,
    setSearchTerm,
    resetFilterFields,
}: TableFilterSearchProps) {
    const submitHandler = (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        resetFilterFields(e);
        const searchField = e.currentTarget.elements.namedItem(
            "search",
        ) as HTMLInputElement;
        setSearchTerm(searchField?.value);

        appInsights?.trackEvent({
            name: `${FeatureName.DAILY_DATA} | Search`,
        });
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
                    className="margin-right-205 height-5"
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
