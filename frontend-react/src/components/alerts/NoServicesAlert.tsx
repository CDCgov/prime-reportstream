import React from "react";

import { USExtLink } from "../USLink";

export const NoServicesBanner = () => {
    return (
        <>
            <section className="rs-text-align-center">
                <img
                    className="margin-bottom-6"
                    src="/assets/folder-search.svg"
                    alt="Map of states using ReportStream"
                />
                <h3>No available data</h3>
                <p>
                    If you need more help finding what you're looking for,{" "}
                    <USExtLink href="https://app.smartsheetgov.com/b/form/da894779659b45768079200609b3a599">
                        contact us
                    </USExtLink>
                    .
                </p>
            </section>
        </>
    );
};
