import { SpinnerCircular } from 'spinners-react'

interface Props {
    /* Will this spinner be a full page spinner? Marking this true will center the
       spinner on the page */
    fullPage?: boolean
}

const defaultStyles = {
    display: "flex",
    alignItems: "center",
    justifyContent: "left"
}

const fullPageStyles = {
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    height: "calc(100vh - 230px)"
}

function Spinner(props: Props) {
    return (
        <div style={props.fullPage ? fullPageStyles : defaultStyles} className="grid-container">
            <SpinnerCircular
                color="rgba(57, 88, 172, 1)"
                size="10%"
            />
        </div>
    )
}

export default Spinner
