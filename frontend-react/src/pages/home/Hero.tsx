import { USSmartLink } from "../../components/USLink";

export default function Hero() {
    return (
        <header className="usa-section--light padding-y-4 tablet:padding-y-6 tablet:margin-bottom-4 margin-top-neg-5">
            <div className="grid-container">
                <div className="font-sans-lg">
                    <h1 className="font-sans-xl tablet:font-sans-2xl margin-top-2">
                        ReportStream
                    </h1>
                    <p>
                        Your single connection to simplify data transfer and
                        improve public health
                    </p>
                    <p>
                        Connect with us to{" "}
                        <USSmartLink href="https://app.smartsheetgov.com/b/form/8c71931f25e64e42bf1fef32900bdecd">
                            send data
                        </USSmartLink>{" "}
                        or{" "}
                        <USSmartLink href="mailto:reportstream@cdc.gov">
                            receive data
                        </USSmartLink>
                        .
                    </p>
                </div>
            </div>
        </header>
    );
}
