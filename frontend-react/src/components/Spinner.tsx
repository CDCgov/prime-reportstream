import { Circles } from "react-loader-spinner";

interface SpinnerProps {
    /* Will this spinner be a full page spinner? Marking this true will center the
       spinner on the page */
    fullPage?: boolean;
}

const defaultStyles: React.CSSProperties = {
    display: "flex",
    alignItems: "center",
    justifyContent: "left",
};

const fullPageStyles: React.CSSProperties = {
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    height: "calc(100vh - 230px)",
};

function Spinner(props: SpinnerProps) {
    return (
        <div
            style={props.fullPage ? fullPageStyles : defaultStyles}
            className="grid-container"
        >
            <Circles ariaLabel="loading-indicator" />
        </div>
    );
}

export default Spinner;
