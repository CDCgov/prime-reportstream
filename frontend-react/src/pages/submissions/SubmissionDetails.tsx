import { Suspense } from "react";
import { useParams } from "react-router-dom";
import { NetworkErrorBoundary, useResource } from "rest-hooks";

import { getStoredOrg } from "../../contexts/SessionStorageTools";
import Spinner from "../../components/Spinner";
import Title from "../../components/Title";
import ActionDetailsResource, {
    Destination,
} from "../../resources/ActionDetailsResource";
import { generateDateTitles } from "../../utils/DateTimeUtils";
import { ErrorPage } from "../error/ErrorPage";

/* Custom types */
type DetailItemProps = {
    item: string;
    content: any;
    subItem?: boolean;
};

type DestinationItemProps = {
    destinationObj: Destination;
};

type SubmissionDetailsProps = {
    actionId: string | undefined;
};

/*
    A component displaying a soft gray title and content in
    standard black text.

    @param item - the title of a property; e.g. Report ID
    @param content - the content of a property; e.g. 000000-0000-0000-000000
*/
export function DetailItem({ item, content, subItem }: DetailItemProps) {
    return (
        <div
            style={{
                display: "flex",
                flexDirection: "column",
                margin: subItem ? "16px 32px" : "8px 0px",
            }}
        >
            <span>{item}</span>
            <span>{content}</span>
        </div>
    );
}

/*
    A component displaying information about a single destination
    returned from the waters/report/{submissionId}/history details API

    @param destinationObj - a single object from the destinations array
    in the report history details API
*/
export function DestinationItem({ destinationObj }: DestinationItemProps) {
    const submissionDate = generateDateTitles(destinationObj.sending_at);
    const dataStream = `(${destinationObj.service})`;
    return (
        <div
            style={{
                display: "flex",
                flexDirection: "column",
            }}
        >
            <h2>
                {destinationObj.organization} {dataStream}
            </h2>
            <DetailItem
                item={"Transmission Date"}
                content={
                    submissionDate ? submissionDate.dateString : "Parsing error"
                }
                subItem
            />
            <DetailItem
                item={"Transmission Time"}
                content={
                    submissionDate ? submissionDate.timeString : "Parsing error"
                }
                subItem
            />
            <DetailItem
                item={"Records"}
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
function SubmissionDetailsContent() {
    const organization = getStoredOrg();
    const { actionId } = useParams<SubmissionDetailsProps>();
    const actionDetails: ActionDetailsResource = useResource(
        ActionDetailsResource.detail(),
        { actionId, organization }
    );
    const submissionDate = generateDateTitles(actionDetails.timestamp);

    /* Conditional title strings */
    const preTitle = `${
        actionDetails.sender
    } ${actionDetails.topic.toUpperCase()} Submissions`;
    const titleString: string = submissionDate
        ? `${submissionDate.dateString} ${submissionDate.timeString}`
        : "Date and Time parsing error";

    /* Only used when externalName is present */
    const titleWithFilename: string | undefined =
        actionDetails.externalName !== ""
            ? `${titleString} - ${actionDetails.externalName}`
            : undefined;

    if (!actionDetails) {
        return <ErrorPage type="page" />;
    } else {
        return (
            <div
                className="grid-container margin-bottom-10"
                data-testid="container"
            >
                <div className="grid-col-12">
                    <Title
                        preTitle={preTitle}
                        title={
                            titleWithFilename ? titleWithFilename : titleString
                        }
                    />
                    <DetailItem item={"Report ID"} content={actionDetails.id} />
                    {actionDetails.destinations.map((dst) => (
                        <DestinationItem
                            key={`${dst.organization_id}-${dst.sending_at}`}
                            destinationObj={dst}
                        />
                    ))}
                </div>
            </div>
        );
    }
}

/*
    For a component to use the Suspense and NEB fallbacks, it must be nested within
    the according tags, hence this wrapper.
*/
function SubmissionDetails() {
    return (
        <NetworkErrorBoundary
            fallbackComponent={() => <ErrorPage type="page" />}
        >
            <Suspense fallback={<Spinner size="fullpage" />}>
                <SubmissionDetailsContent />
            </Suspense>
        </NetworkErrorBoundary>
    );
}

export default SubmissionDetails;
