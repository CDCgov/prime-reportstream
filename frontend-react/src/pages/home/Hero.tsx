import React from "react";

import site from "../../content/site.json";
import { USExtLink } from "../../components/USLink";

export default function Hero() {
    return (
        <header className="bg-primary-darker text-white padding-y-9 margin-top-neg-5">
            <div className="grid-container">
                <div className="grid-row">
                    <h1 className="font-sans-3xl margin-top-2">
                        Your single connection to simplify data transfer and
                        improve public health
                    </h1>
                    <p className="font-sans-lg">
                        ReportStream is CDC’s free, interoperable platform for
                        streamlining public health reporting. We navigate
                        unique, complex requirements and work to make sure your
                        data gets where it needs to be.
                    </p>
                </div>
                <div className="grid-row padding-top-8 margin-bottom-2">
                    <USExtLink
                        href={site.forms.connectWithRS.url}
                        className="usa-button"
                    >
                        Connect now
                    </USExtLink>
                </div>
            </div>
        </header>
    );
}
