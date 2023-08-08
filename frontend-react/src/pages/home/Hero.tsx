import { Link } from "@trussworks/react-uswds";
import React from "react";

export default function Hero() {
    return (
        <header className="bg-primary-darker text-white padding-y-9 margin-top-neg-5">
            <div className="grid-container width-desktop-lg padding-left-4 padding-right-4">
                <div className="grid-row maxw-tablet-lg">
                    <h1 className="font-sans-xl tablet:font-sans-2xl margin-top-2">
                        Your single connection to simplify data transfer and
                        improve public health
                    </h1>
                    <p className="usa-intro maxw-tablet">
                        ReportStream is CDC’s free, interoperable platform for
                        streamlining public health reporting. We navigate
                        unique, complex requirements and work to make sure your
                        data gets where it needs to be.
                    </p>
                </div>
                <div className="grid-row padding-top-8">
                    <Link href="" className="usa-button">
                        Connect now
                    </Link>
                </div>
            </div>
        </header>
    );
}
