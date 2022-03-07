import React, { createContext, PropsWithChildren, useState } from "react";

interface FilterState {
    startRange?: string;
    endRange?: string;
}

interface ISubmissionFilterContext {
    filters: FilterState;
    updateStartRange?: (val: string) => void;
    updateEndRange?: (val: string) => void;
}

/* This is a definition of the context shape, NOT the payload delivered */
export const SubmissionFilterContext = createContext<ISubmissionFilterContext>({
    filters: {
        startRange: "",
        endRange: "",
    },
});

/*
 * This component handles a pseudo-global state for the Submission
 * components; primarily linking SubmissionsTable and
 * SubmissionFilters. This is much friendlier than callback functions
 * and piping props!
 */
const SubmissionContext: React.FC<any> = (props: PropsWithChildren<any>) => {
    const [startRange, setStartRange] = useState<string>();
    const [endRange, setEndRange] = useState<string>();

    const updateStartRange = (val: string) => setStartRange(val);
    const updateEndRange = (val: string) => setEndRange(val);

    /* This is the payload we deliver through our context provider */
    const contextPayload: ISubmissionFilterContext = {
        filters: {
            startRange: startRange,
            endRange: endRange,
        },
        updateStartRange: updateStartRange,
        updateEndRange: updateEndRange,
    };

    return (
        <SubmissionFilterContext.Provider value={contextPayload}>
            {props.children}
        </SubmissionFilterContext.Provider>
    );
};

export default SubmissionContext;
