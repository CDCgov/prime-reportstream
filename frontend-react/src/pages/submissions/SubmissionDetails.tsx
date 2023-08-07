import React, { Suspense } from "react";
import { useLocation, useParams } from "react-router-dom";
import { NetworkErrorBoundary, useResource } from "rest-hooks";
import { GridContainer } from "@trussworks/react-uswds";

import { getStoredOrg } from "../../utils/SessionStorageTools";
import Spinner from "../../components/Spinner";
import Title from "../../components/Title";
import ActionDetailsResource, {
    Destination,
} from "../../resources/ActionDetailsResource";
import { generateDateTitles } from "../../utils/DateTimeUtils";
import { ErrorPage } from "../error/ErrorPage";
import Crumbs, { CrumbConfig } from "../../components/Crumbs";
import { MemberType } from "../../hooks/UseOktaMemberships";
import { AuthElement } from "../../components/AuthElement";
import { DetailItem } from "../../components/DetailItem/DetailItem";
import { FeatureName } from "../../AppRouter";

/* Custom types */
type DestinationItemProps = {
    destinationObj: Destination;
};

type SubmissionDetailsProps = {
    actionId: string | undefined;
};

/*
    A component displaying information about a single destination
    returned from the waters/report/{submissionId}/history details API

    @param destinationObj - a single object from the destinations array
    in the report history details API
*/
export function DestinationItem({ destinationObj }: DestinationItemProps) {
    const submissionDate = generateDateTitles(destinationObj.sending_at);
    const dataStream = destinationObj.service.toUpperCase();
    return (
        <div className="display-flex flex-column">
            <h2>{destinationObj.organization}</h2>
            <DetailItem item={"Data Stream"} content={dataStream} />
            <DetailItem
                item={"Transmission Date"}
                content={
                    destinationObj.itemCount > 0
                        ? submissionDate
                            ? submissionDate.dateString
                            : "Parsing error"
                        : "Not transmitting - all data filtered"
                }
            />
            <DetailItem
                item={"Transmission Time"}
                content={
                    destinationObj.itemCount > 0
                        ? submissionDate
                            ? submissionDate.timeString
                            : "Parsing error"
                        : "Not transmitting - all data filtered"
                }
            />
            <DetailItem item={"Records"} content={destinationObj.itemCount} />
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
        { actionId, organization },
    );
    const submissionDate = generateDateTitles(actionDetails.timestamp);

    /* Conditional title strings */
    const preTitle = `${
        actionDetails.sender
    } ${actionDetails.topic.toUpperCase()} ${FeatureName.SUBMISSIONS}`;
    const titleString: string = submissionDate
        ? `${submissionDate.dateString} ${submissionDate.timeString}`
        : "Date and Time parsing error";

    /* Only used when externalName is present */
    const titleWithFilename: string | undefined =
        actionDetails.externalName !== null
            ? `${titleString} - ${actionDetails.externalName}`
            : undefined;

    if (!actionDetails) {
        return <ErrorPage type="page" />;
    } else {
        return (
            <div className="margin-bottom-10" data-testid="container">
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
    const { actionId } = useParams<SubmissionDetailsProps>();
    const crumbs: CrumbConfig[] = [
        { label: "Submissions", path: "/submissions" },
        { label: `Details: ${actionId}` },
    ];
    const location = useLocation();
    return (
        <GridContainer>
            <Crumbs
                crumbList={crumbs}
                previousPage={(location.state as any)?.previousPage}
            />
            <NetworkErrorBoundary
                fallbackComponent={() => <ErrorPage type="page" />}
            >
                <Suspense fallback={<Spinner size="fullpage" />}>
                    <SubmissionDetailsContent />
                </Suspense>
            </NetworkErrorBoundary>
        </GridContainer>
    );
}

export default SubmissionDetails;

export const SubmissionDetailsWithAuth = () => (
    <AuthElement
        element={<SubmissionDetails />}
        requiredUserType={MemberType.SENDER}
    />
);
