import React, { useState } from "react";
import {
    Button,
    Form,
    FormGroup,
    Label,
    FileInput,
} from "@trussworks/react-uswds";
import { useResource } from "rest-hooks";
import { useOktaAuth } from "@okta/okta-react";
import moment from "moment";
import { library } from "@fortawesome/fontawesome-svg-core";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faSync } from "@fortawesome/free-solid-svg-icons";

import { senderClient } from "../webreceiver-utils";
import AuthResource from "../resources/AuthResource";
import SenderOrganizationResource from "../resources/SenderOrganizationResource";

library.add(faSync);

export const Upload = () => {
    const { authState } = useOktaAuth();
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [fileContent, setFileContent] = useState("");
    const [consolidatedWarnings, setConsolidatedWarnings] = useState([]);
    const [consolidatedErrors, setConsolidatedErrors] = useState([]);
    const [destinations, setDestinations] = useState("");
    const [reportId, setReportId] = useState(null);
    const [successTimestamp, setSuccessTimestamp] = useState("");
    const [buttonText, setButtonText] = useState("Upload");
    const [headerMessage, setHeaderMessage] = useState(
        "Upload your COVID-19 results"
    );
    const [errorMessageText, setErrorMessageText] = useState(
        `Please resolve the errors below and upload your edited file. Your file has not been accepted.`
    );

    const client = senderClient(authState);
    const organization = useResource(SenderOrganizationResource.detail(), {
        name: client,
    });

    const userName = {
        firstName: authState?.accessToken?.claims.given_name || "",
        lastName: authState?.accessToken?.claims.family_name || "",
    };

    const uploadReport = async function postData(fileBody: string) {
        let textBody;
        let response;
        try {
            response = await fetch(`${AuthResource.getBaseUrl()}/api/waters`, {
                method: "POST",
                headers: {
                    "Content-Type": "text/csv",
                    client: client,
                    "authentication-type": "okta",
                    Authorization: `Bearer ${authState?.accessToken?.accessToken}`,
                },
                body: fileBody,
            });

            textBody = await response.text();

            // if this JSON.parse fails, the body was most likely an error string from the server
            return JSON.parse(textBody);
        } catch (error) {
            return {
                ok: false,
                status: response ? response.status : 500,
                errors: [
                    {
                        details: textBody ? textBody : error,
                    },
                ],
            };
        }
    };

    const handleChange = async (event: React.ChangeEvent<HTMLInputElement>) => {
        try {
            if (!event?.currentTarget?.files?.length) {
                // no files selected
                return;
            }
            const file = event.currentTarget.files.item(0);
            if (!file) {
                // shouldn't happen but keeps linter happy
                return;
            }

            // load the "contents" of the file. Hope it fits in memory!
            const filecontent = await file.text();
            setFileContent(filecontent);
        } catch (err) {
            // todo: have central error reporting mechanism.
            console.error(err);
        }
    };

    const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
        event.preventDefault();

        // reset the state on subsequent uploads
        setIsSubmitting(true);
        setReportId(null);
        setSuccessTimestamp("");
        setConsolidatedWarnings([]);
        setConsolidatedErrors([]);
        setDestinations("");

        if (fileContent.length) {
            let response;
            try {
                response = await uploadReport(fileContent);

                if (response.destinations && response.destinations.length) {
                    // NOTE: `{ readonly [key: string]: string }` means a key:value object
                    setDestinations(
                        response.destinations
                            .map((d: { readonly [key: string]: string }) => d["organization"])
                            .join(", ")
                    );
                }

                if (response.id) {
                    setReportId(response.id);
                    setSuccessTimestamp(response.timestamp);
                    event.currentTarget.reset();
                }

                if (response.errors && response.errors.length) {
                    // if there is a response status, then there was most likely a server-side error as the json was not parsed
                    if (response.status) {
                        setErrorMessageText(
                            "There was a server error. Your file has not been accepted."
                        );
                    } else {
                        setErrorMessageText(
                            "Please resolve the errors below and upload your edited file. Your file has not been accepted."
                        );
                    }
                }

                if (
                    response.consolidatedWarnings &&
                    response.consolidatedWarnings.length
                ) {
                    setConsolidatedWarnings(response.consolidatedWarnings);
                }

                if (
                    response.consolidatedErrors &&
                    response.consolidatedErrors.length
                ) {
                    setConsolidatedErrors(response.consolidatedErrors);
                }

                setHeaderMessage("Your COVID-19 Results");
                setButtonText("Upload another file");
            } catch (error) {
                if (response && response.consolidatedErrors) {
                    setConsolidatedErrors(response.errors);
                }
                setButtonText("Upload another file");
            }
            setIsSubmitting(false);
        }
    };

    const formattedSuccessDate = (format:string) => {
        const timestampDate = new Date(successTimestamp);
        return moment(timestampDate).format(format);
    };

    const timeZoneAbbreviated = () => {
        return Intl.DateTimeFormat().resolvedOptions().timeZone;
    };

    return (
        <div className="grid-container usa-section margin-bottom-10">
            <span
                id="orgName"
                className="text-normal text-base margin-bottom-0"
            >
                {userName.firstName} {userName.lastName}
            </span>
            <h1 className="margin-top-0 margin-bottom-5">
                {organization?.description}
            </h1>
            <h2 className="font-sans-lg">{headerMessage}</h2>
            {reportId && (
                <div>
                    <div className="usa-alert usa-alert--success">
                        <div className="usa-alert__body">
                            <h4 className="usa-alert__heading">
                                Success: File accepted
                            </h4>
                            <p className="usa-alert__text">
                                Your file has been successfully transmitted to
                                the department of health.
                            </p>
                        </div>
                    </div>
                    <div>
                        <p
                            id="orgName"
                            className="text-normal text-base margin-bottom-0"
                        >
                            Confirmation Code
                        </p>
                        <p className="margin-top-05">{reportId}</p>
                    </div>
                    <div>
                        <p
                            id="orgName"
                            className="text-normal text-base margin-bottom-0"
                        >
                            Date Received
                        </p>
                        <p className="margin-top-05">
                            {formattedSuccessDate("DD MMMM YYYY")}
                        </p>
                    </div>

                    <div>
                        <p
                            id="orgName"
                            className="text-normal text-base margin-bottom-0"
                        >
                            Time Received
                        </p>
                        <p className="margin-top-05">{`${formattedSuccessDate(
                            "h:mm"
                        )} ${timeZoneAbbreviated()}`}</p>
                    </div>
                    <div>
                        <p
                            id="orgName"
                            className="text-normal text-base margin-bottom-0"
                        >
                            Recipients
                        </p>
                        {destinations && (
                            <p className="margin-top-05">{destinations}</p>
                        )}
                        {!destinations && (
                            <p className="margin-top-05">
                                There are no known recipients at this time.
                            </p>
                        )}
                    </div>
                </div>
            )}

            {consolidatedErrors.length > 0 && (
                <div>
                    <div className="usa-alert usa-alert--error" role="alert">
                        <div className="usa-alert__body">
                            <h4 className="usa-alert__heading">
                                Error: File not accepted
                            </h4>
                            <p className="usa-alert__text">
                                {errorMessageText}
                            </p>
                        </div>
                    </div>
                    <table className="usa-table usa-table--borderless">
                        <thead>
                            <tr>
                                <th>Requested Edit</th>
                                <th>Areas Containing the Requested Edit</th>
                            </tr>
                        </thead>
                        <tbody>
                            {consolidatedErrors.map((e, i) => {
                                return (
                                    <tr key={"error_" + i}>
                                        <td>{e["message"]}</td>
                                        <td>{e["rows"]}</td>
                                    </tr>
                                );
                            })}
                        </tbody>
                    </table>
                </div>
            )}

            {consolidatedWarnings.length > 0 && (
                <div>
                    <div className="usa-alert usa-alert--warning">
                        <div className="usa-alert__body">
                            <h4 className="usa-alert__heading">
                                Alert: Unusable Fields Detected
                            </h4>
                            <p className="usa-alert__text">
                                Your file has been accepted with warnings. There
                                were fields detected that are unusable for
                                public health action. Enter valid information
                                for future submissions.
                            </p>
                        </div>
                    </div>
                    <table className="usa-table usa-table--borderless">
                        <thead>
                            <tr>
                                <th>Requested Edit</th>
                                <th>Areas Containing the Requested Edit</th>
                            </tr>
                        </thead>
                        <tbody>
                            {consolidatedWarnings.map((e, i) => {
                                return (
                                    <tr key={"warning_" + i}>
                                        <td>{e["message"]}</td>
                                        <td>{e["rows"]}</td>
                                    </tr>
                                );
                            })}
                        </tbody>
                    </table>
                </div>
            )}

            <Form onSubmit={(e) => handleSubmit(e)}>
                <FormGroup className="margin-bottom-3">
                    <Label
                        className="font-sans-xs"
                        id="upload-csv-input-label"
                        htmlFor="upload-csv-input"
                    >
                        Upload your COVID-19 lab results as a .CSV.
                    </Label>
                    <FileInput
                        id="upload-csv-input"
                        name="upload-csv-input"
                        aria-describedby="upload-csv-input-label"
                        accept=".csv, text/csv"
                        onChange={(e) => handleChange(e)}
                        required
                    />
                </FormGroup>
                <Button type="submit" disabled={isSubmitting}>
                    {isSubmitting && (
                        <span>
                            <FontAwesomeIcon
                                icon="sync"
                                spin
                                className="margin-right-05"
                            />
                            <span>Processing file...</span>
                        </span>
                    )}

                    {!isSubmitting && <span>{buttonText}</span>}
                </Button>
            </Form>
        </div>
    );
};
