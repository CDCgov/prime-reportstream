import React from 'react'
import { SpinnerCircular } from 'spinners-react'

function Spinner() {
    return (
        <div id="spinner" className="grid-container">
            <SpinnerCircular
                color="rgba(57, 88, 172, 1)"
                size="10%"
            />
        </div>
    )
}

export default Spinner
