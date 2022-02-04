import { useOktaAuth } from '@okta/okta-react';
import { useController } from 'rest-hooks';
import { getStoredOrg } from '../../components/GlobalContextProvider';
import Title from '../../components/Title';
import { Destination } from '../../types/SubmissionDetailsTypes'

/* Custom types */
type DetailItemProps = {
    item: string,
    content: any
}

type DestinationItemProps = {
    destinationObj: Destination
}

type SubmissionDetailsProps = {
    actionId: number
}

/* 
    A component displaying a soft gray title and content in
    standard black text. 

    @param item - the title of a property; e.g. Submission ID
    @param content - the content of a property; e.g. 000000-0000-0000-000000
*/
function DetailItem({ item, content }: DetailItemProps) {
    return (
        <>
            <p>{item}</p>
            <p>{content}</p>
        </>
    );
}

/* 
    A component displaying information about a single destination
    returned from the history/submissions details API

    @param destinationObj - a single object from the destinations array
    in the history/submissions details API
*/
function DestinationItem({ destinationObj }: DestinationItemProps) {
    return (
        <>
        </>
    );
}

/* 
    The page component showcasing details about a submission to the
    sender

    @param actionId - the id tracking the ActionLog object containing
    the information to display. Used to call the API.
*/
function SubmissionDetails({ actionId }: SubmissionDetailsProps) {
    const { authState } = useOktaAuth();
    const organization = getStoredOrg();
    const { fetch: fetchController } = useController();

    return (
        <>
            <Title preTitle='Pre-title test' title='Title test' />
        </>
    );
}

export default { SubmissionDetails };
