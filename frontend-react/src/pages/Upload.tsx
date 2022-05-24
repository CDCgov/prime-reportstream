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
import { NavLink, useHistory } from "react-router-dom";

import SenderOrganizationResource from "../resources/SenderOrganizationResource";
import {
    getStoredOrg,
    getStoredSenderName,
} from "../contexts/SessionStorageTools";
import { showError } from "../components/AlertNotifications";
import Spinner from "../components/Spinner";

// values taken from Report.kt
const PAYLOAD_MAX_BYTES = 50 * 1000 * 1000; // no idea why this isn't in "k" (* 1024).
const REPORT_MAX_ITEMS = 10000;
const REPORT_MAX_ITEM_COLUMNS = 2000;
// const REPORT_MAX_ERRORS = 100;

export const Upload = () => {
    const { authState } = useOktaAuth();
    const history = useHistory();
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [fileInputResetValue, setFileInputResetValue] = useState(0);
    const [fileContent, setFileContent] = useState("");
    const [fileName, setFileName] = useState("");
    const [errors, setErrors] = useState([]);
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

    const client = `${getStoredOrg()}.${getStoredSenderName()}`;
    const organization = useResource(SenderOrganizationResource.detail(), {
        name: getStoredOrg(),
    });

    const userName = {
        firstName: authState?.accessToken?.claims.given_name || "",
        lastName: authState?.accessToken?.claims.family_name || "",
    };

    const uploadReport = async function postData() {
        let textBody;
        let response;
        try {
            response = await fetch(
                `${process.env.REACT_APP_BACKEND_URL}/api/waters`,
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "text/csv",
                        client: client, // ignore.ignore-waters
                        "authentication-type": "okta",
                        Authorization: `Bearer ${authState?.accessToken?.accessToken}`,
                        payloadName: fileName, // header Naming-Convention or namingConvention or naming-convention?
                    },
                    body: fileContent,
                }
            );

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

    const handleFileChange = async (
        event: React.ChangeEvent<HTMLInputElement>
    ) => {
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

            if (file.size > PAYLOAD_MAX_BYTES) {
                const maxkbytes = (PAYLOAD_MAX_BYTES / 1024).toLocaleString(
                    "en-US",
                    { maximumFractionDigits: 2, minimumFractionDigits: 2 }
                );

                showError(
                    `The file '${file.name}' is too large. The maximum file size is ${maxkbytes}k`
                );
                return;
            }
            // load the "contents" of the file. Hope it fits in memory!
            const filecontent = await file.text();

            // count the number of lines
            const linecount = (filecontent.match(/\n/g) || []).length + 1;
            if (linecount > REPORT_MAX_ITEMS) {
                showError(
                    `The file '${file.name}' has too many rows. The maximum number of rows allowed is ${REPORT_MAX_ITEMS}.`
                );
                return;
            }
            if (linecount <= 1) {
                showError(
                    `The file '${file.name}' doesn't contain any valid data. ` +
                        `File should have a header line and at least one line of data.`
                );
                return;
            }

            // get the first line and examine it
            const firstline = (filecontent.match(/^(.*)\n/) || [""])[0];
            // ideally, the columns would be comma seperated, but they may be tabs, because the first
            // line is a header, we don't have to worry about escaped delimitors in strings (e.g. ,"Smith, John",)
            const columncount =
                (firstline.match(/,/g) || []).length ||
                (firstline.match(/\t/g) || []).length;

            if (columncount > REPORT_MAX_ITEM_COLUMNS) {
                showError(
                    `The file '${file.name}' has too many columns. The maximum number of allowed columns is ${REPORT_MAX_ITEM_COLUMNS}.`
                );
                return;
            }

            setFileName(file.name);
            setFileContent(filecontent);
            // todo: this is a good place to do basic validation of the upload file. e.g. does it have
            // all the required columns? Are any rows obviously not correct (empty or obviously wrong type)?
        } catch (err: any) {
            // todo: have central error reporting mechanism.
            console.error(err);
            showError(`An unexpected error happened: '${err.toString()}'`);
        }
    };

    const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
        event.preventDefault();

        // reset the state on subsequent uploads
        setIsSubmitting(true);
        setReportId(null);
        setSuccessTimestamp("");
        setErrors([]);
        setDestinations("");

        if (fileContent.length === 0) {
            return;
        }

        let response;
        try {
            response = await uploadReport();
            if (response?.destinations?.length) {
                // NOTE: `{ readonly [key: string]: string }` means a key:value object
                setDestinations(
                    response.destinations
                        .map(
                            (d: { readonly [key: string]: string }) =>
                                d["organization"]
                        )
                        .join(", ")
                );
            }

            if (response?.id) {
                setReportId(response.id);
                setSuccessTimestamp(response.timestamp);
                event.currentTarget.reset();
            }

            if (response?.errors?.length) {
                // if there is a response status, then there was most likely a server-side error as the json was not parsed
                setErrorMessageText(
                    response?.status
                        ? "There was a server error. Your file has not been accepted."
                        : "Please resolve the errors below and upload your edited file. Your file has not been accepted."
                );
            }

            setHeaderMessage("Your COVID-19 Results");
        } catch (error) {
            // Noop.  Errors are collected below
        }

        // Process the error messages
        if (response?.errors && response.errors.length > 0) {
            // Add a string to properly display the indices if available.
            response.errors.map(
                (errorMsg: any) =>
                    (errorMsg.rowList =
                        errorMsg.indices && errorMsg.indices.length > 0
                            ? errorMsg.indices.join(", ")
                            : "")
            );
            setErrors(response.errors);
        }
        setButtonText("Upload another file");
        // Changing the key to force the FileInput to reset. Otherwise it won't recognize changes to the file's content unless the file name changes
        setFileInputResetValue(fileInputResetValue + 1);
        setIsSubmitting(false);
    };

    const formattedSuccessDate = (format: string) => {
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
                            <p className="margin-top-0">
                                <NavLink
                                    to="/submissions"
                                    className="text-no-underline"
                                >
                                    Review your file status in Submissions.
                                </NavLink>
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

            {errors.length > 0 && (
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
                            {errors.map((e, i) => {
                                return (
                                    <tr key={"error_" + i}>
                                        <td>{e["message"]}</td>
                                        <td>Row(s): {e["rowList"]}</td>
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
                        key={fileInputResetValue}
                        id="upload-csv-input"
                        name="upload-csv-input"
                        aria-describedby="upload-csv-input-label"
                        accept="text/csv, .csv"
                        onChange={(e) => handleFileChange(e)}
                        required
                    />
                </FormGroup>
                <div className="display-flex">
                    <Button
                        className={`${reportId && "flex-1 margin-right-3"}`}
                        type="submit"
                        disabled={isSubmitting || fileName.length === 0}
                    >
                        {isSubmitting && (
                            <span>
                                <Spinner />
                                <span>Processing file...</span>
                            </span>
                        )}

                        {!isSubmitting && <span>{buttonText}</span>}
                    </Button>
                    {reportId && (
                        <Button
                            type="button"
                            className="usa-button flex-1"
                            onClick={() => history.push("/submissions")}
                        >
                            <span>Review Submissions</span>
                        </Button>
                    )}
                </div>
            </Form>
        </div>
    );
};
