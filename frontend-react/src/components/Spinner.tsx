import { Oval } from "react-loader-spinner";
import { ReactNode } from "react";

interface SpinnerProps {
    size?: "default" | "fullpage" | "insidebutton";
    display?: boolean; // allows easier dynamic show/hide during load
    message?: ReactNode;
}

function Spinner({
    size = "default",
    display = true,
    message = "",
}: SpinnerProps) {
    // map prop to css className
    const sizeClassName = {
        default: "grid-container rs-spinner-default",
        fullpage: "grid-container rs-spinner-default rs-spinner-fullpage",
        insidebutton: "rs-spinner-default rs-spinner-tiny",
    }[size];
    return (
        <div className="padding-y-4 text-center">
            <div className="grid-row">
                <div
                    hidden={!display}
                    className={sizeClassName}
                    data-testid="rs-spinner"
                >
                    <Oval
                        height={60}
                        width={60}
                        color="#2378c3"
                        secondaryColor="#aacdec"
                        strokeWidth={8}
                        strokeWidthSecondary={8}
                        ariaLabel="loading-indicator"
                    />
                </div>
            </div>
            {message && (
                <div className="grid-row">
                    <div
                        className="margin-x-auto tablet:grid-col-5 line-height-sans-6"
                        data-testid="spinner-message"
                    >
                        <span>{message}</span>
                    </div>
                </div>
            )}
        </div>
    );
}

export default Spinner;
