import { ButtonGroup, Button } from '@trussworks/react-uswds'
import { useState } from 'react'

type Props = {
    /* REQUIRED
    A list of senders gathered by calling getListOfSenders() */
    senders: string[]

    /* REQUIRED
    A function passed in by the parent prop to sync chosen state
    This can be seen in-use by <TableReports>. The chosen state in sync'd
    and TableReports filters by the chosen sender */
    chosenCallback: Function
}

/* 
    These are the buttons used to swap between various senders of data
    to see only reports sent by individual senders populated on their
    list
*/
function TableButtonGroup(props: Props) {
    const senders: string[] = props.senders
    const [chosen, setChosen] = useState(senders[0])

    /* This sets both the <TableButtonGroup> AND <TableReports> chosen state variable */
    const handleClick = (id) => {
        setChosen(id)
        props.chosenCallback(id)
    }

    return (
        <ButtonGroup type="segmented">
            {
                senders.map((val) => {
                    return <Button
                        key={val}
                        id={val}
                        onClick={() => handleClick(val)}
                        type="button"
                        outline={val !== chosen}>
                        {
                            /* Accounting for the fact we have not been POSTing items with
                            a sendingOrg property yet */
                            val === "" ? "No Sender ID" : val
                        }
                    </Button>
                })
            }
        </ButtonGroup>
    )
}

export default TableButtonGroup
