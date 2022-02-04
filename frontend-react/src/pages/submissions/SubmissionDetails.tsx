import { Button } from '@trussworks/react-uswds';
import { Suspense } from 'react';
import { useParams } from 'react-router-dom';
import { NetworkErrorBoundary, useResource } from 'rest-hooks';
import { getStoredOrg } from '../../components/GlobalContextProvider';
import Spinner from '../../components/Spinner';
import Title from '../../components/Title';
import ActionDetailsResource from '../../resources/ActionDetailsResource';
import { Destination } from '../../types/SubmissionDetailsTypes'
import { ErrorPage } from '../error/ErrorPage';

/* Custom types */
type DetailItemProps = {
    item: string,
    content: any,
    subItem?: boolean;
}

type DestinationItemProps = {
    destinationObj: Destination
}

type SubmissionDetailsProps = {
    actionId: string | undefined
}

type SubmissionDate = {
    dateString: string;
    timeString: string;
}

/* 
    This function serves as a cleaner (read: more contained) way of parsing out
    necessary date and time string formats for this page.

    @param dateTimeString - the value representing when a report was sent, returned
    by the API         
*/
// TODO: Refactor, maybe??
const generateSubmissionDate = (dateTimeString: string): SubmissionDate => {
    const monthNames = ["Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    ];
    const dateTimeISO = new Date(dateTimeString)

    /* Parse time into parts */
    const minutes = dateTimeISO.getMinutes()
    let hours = dateTimeISO.getHours()
    let meridian = "am"

    /* 12-hour and meridian conversion */
    if (hours > 12) {
        hours -= 12
        meridian = "pm"
    }

    /* Create strings from parsed values */
    const time = `${hours}:${minutes} ${meridian.toUpperCase()}`
    const date = `${dateTimeISO.getDate()} ${monthNames[dateTimeISO.getMonth()]} ${dateTimeISO.getFullYear()}`

    return {
        dateString: date,
        timeString: time
    }
}

/* 
    A component displaying a soft gray title and content in
    standard black text. 

    @param item - the title of a property; e.g. Report ID
    @param content - the content of a property; e.g. 000000-0000-0000-000000
*/
function DetailItem({ item, content, subItem }: DetailItemProps) {
    return (
        <div style={{
            'display': 'flex',
            'flexDirection': 'column',
            'margin': subItem ? '16px 32px' : '8px 0px'
        }}>
            <span>{item}</span>
            <span>{content}</span>
        </div>
    );
}

/* 
    A component displaying information about a single destination
    returned from the history/submissions details API

    @param destinationObj - a single object from the destinations array
    in the history/submissions details API
*/
function DestinationItem({ destinationObj }: DestinationItemProps) {
    const submissionDate = generateSubmissionDate(destinationObj.sending_at) || "unsent"
    return (
        <div style={{
            'display': 'flex',
            'flexDirection': 'column',
        }}>
            <h2>{destinationObj.organization}</h2>
            <DetailItem
                item={'Transmission Date'}
                content={submissionDate.dateString}
                subItem
            />
            <DetailItem
                item={'Transmission Time'}
                content={submissionDate.timeString}
                subItem
            />
            <DetailItem
                item={'Records'}
                content={destinationObj.itemCount}
                subItem
            />
        </div>
    );
}

/* 
    The page component showcasing details about a submission to the
    sender

    @param actionId - the id tracking the ActionLog object containing
    the information to display. Used to call the API.
*/
function SubmissionDetails() {
    const organization = getStoredOrg();
    const { actionId } = useParams<SubmissionDetailsProps>()
    const actionDetails: ActionDetailsResource = useResource(
        ActionDetailsResource.detail(),
        { actionId, organization }
    );
    const submissionDate = generateSubmissionDate(actionDetails.submittedAt)

    return (
        <div className="grid-container margin-bottom-10">
            <div className="grid-col-12">
                {/* TODO: why does the spinner take the whole dang page?! */}
                <NetworkErrorBoundary
                    fallbackComponent={() => <ErrorPage type="page" />}
                >
                    <Suspense fallback={<Title title='Loading info...' />}>
                        <Title
                            preTitle={
                                `${actionDetails.submitter} ${actionDetails.topic.toUpperCase()} Submissions`
                            }
                            title={`${submissionDate.dateString} ${submissionDate.timeString}`} />
                    </Suspense>
                    <Suspense fallback={<Spinner />}>
                        <DetailItem
                            item={'Report ID'}
                            content={actionDetails.id}
                        />
                        {
                            actionDetails.destinations.map(dst => (
                                <DestinationItem
                                    key={`${dst.organization_id}-${dst.sending_at}`}
                                    destinationObj={dst}
                                />
                            ))
                        }
                    </Suspense>
                </NetworkErrorBoundary>
            </div>
        </div>
    );
}

export default SubmissionDetails;
