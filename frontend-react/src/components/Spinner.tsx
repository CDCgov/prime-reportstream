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
        <div>
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
            <span className="text-center">{message}</span>
        </div>
    );
}

export default Spinner;
