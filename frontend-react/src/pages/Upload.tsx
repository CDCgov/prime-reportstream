import React, {useState} from "react";
import {
    Button,
    Form,
    FormGroup,
    Label,
    FileInput
}
    from '@trussworks/react-uswds';
import {useResource} from 'rest-hooks';
import AuthResource from "../resources/AuthResource";
import {useOktaAuth} from "@okta/okta-react";
import {groupToOrg} from "../webreceiver-utils";
import OrganizationResource from "../resources/OrganizationResource";
import moment from "moment";

const Upload = () => {
    const {authState} = useOktaAuth();
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [file, setFile] = useState(null);
    const [warnings, setWarnings] = useState([]);
    const [errors, setErrors] = useState([]);
    const [destinations, setDestinations] = useState('');
    const [httpErrors, setHttpErrors] = useState('');
    const [reportId, setReportId] = useState(null);
    const [successTimestamp, setSuccessTimestamp] = useState('');
    const [buttonText, setButtonText] = useState('Upload');
    const [headerMessage, setHeaderMessage] = useState('Upload your COVID-19 results');

    const claimsSenderOrganization = authState!.accessToken?.claims.organization.find(o => o.includes("DHSender"))
    const senderOrganization = claimsSenderOrganization.replace('Sender_', '');

    const client =
        groupToOrg(senderOrganization);

    const userName = {
        firstName: authState!.accessToken?.claims.given_name,
        lastName: authState!.accessToken?.claims.family_name
    }

    const organization = useResource(OrganizationResource.detail(), {
        name: client
    });

    const uploadReport =
        async function postData(file) {
            const response = await fetch(`${AuthResource.getBaseUrl()}/api/waters`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'text/csv',
                    'client': client,
                    'authentication-type': 'okta',
                    'Authorization': `Bearer ${authState?.accessToken?.accessToken}`
                },
                body: file
            });
            return response.json();
        }

    const handleChange = (event) => {
        setFile(event.target.files[0]);
    }

    const handleSubmit = async (event) => {
        event.preventDefault();

        // reset the state on subsequent uploads
        setIsSubmitting(true);
        setReportId(null);
        setSuccessTimestamp('');
        setWarnings([]);
        setErrors([]);
        setDestinations('');
        //setHttpErrors('');

        if (file) {
            let response;
            try {

                response = await uploadReport(file);
                setWarnings(response.warnings);
                setErrors(response.errors);
                setDestinations(response.destinations.map(d => d['organization']).join(', '));

                if (response.id) {
                    setReportId(response.id);
                    setSuccessTimestamp(response.timestamp);
                    setButtonText('Upload another file');
                    event.target.reset();
                }

                if (response.warnings && response.warnings.length) {
                    setWarnings(response.warnings);
                    setButtonText('Upload my edited file');
                }

                if (response.errors && response.errors.length) {
                    setErrors(response.errors);
                    setButtonText('Upload my edited file');
                }

                setHeaderMessage('Your COVID-19 Results');

            } catch (error) {
                console.log(error);
                if (typeof error === 'object') {
                    setErrors(error);
                } else {
                    setHttpErrors(error);
                }
                setButtonText('Upload my edited file');
            }
            console.log(response);
            setIsSubmitting(false);
        }
    }

    const formattedSuccessDate = (format) => {
        const timestampDate = new Date(successTimestamp);
        return moment(timestampDate).format(format);
    }

    const timeZoneAbbreviated = () => {
        const x: RegExpMatchArray | null = new Date().toString().match(/\((.+)\)/);

        // In Chrome browser, new Date().toString() is
        // "Thu Aug 06 2020 16:21:38 GMT+0530 (India Standard Time)"

        // In Safari browser, new Date().toString() is
        // "Thu Aug 06 2020 16:24:03 GMT+0530 (IST)"
        if (x) {
            if (x["1"].includes(" ")) {
                return x["1"]
                    .split(" ")
                    .map(([first]) => first)
                    .join("");
            } else {
                return x["1"];
            }
        }
        return 'unknown timezone';
    };

    return (
        <div className="grid-container usa-section margin-bottom-10">
                <span id="orgName" className="text-normal text-base margin-bottom-0">
                    {userName.firstName} {userName.lastName}
                </span>
            <h1 className="margin-top-0 margin-bottom-5">{organization?.description}</h1>
            <h2 className="font-sans-lg">{headerMessage}</h2>
            {reportId && (
                <div>
                    <div className="usa-alert usa-alert--success">
                        <div className="usa-alert__body">
                            <h4 className="usa-alert__heading">Success: File accepted</h4>
                            <p className="usa-alert__text">
                                Your file has been successfully transmitted to the department of health.
                            </p>
                        </div>
                    </div>
                    <div>
                        <p id="orgName" className="text-normal text-base margin-bottom-0">
                            Confirmation Code
                        </p>
                        <p className="margin-top-05">{reportId}</p>
                    </div>
                    <div>
                        <p id="orgName" className="text-normal text-base margin-bottom-0">
                            Date Received
                        </p>
                        <p className="margin-top-05">{formattedSuccessDate('DD MMMM YYYY')}</p>
                    </div>

                    <div>
                        <p id="orgName" className="text-normal text-base margin-bottom-0">
                            Time Received
                        </p>
                        <p className="margin-top-05">{`${formattedSuccessDate('h:mm')} ${timeZoneAbbreviated()}`}</p>
                    </div>
                    <div>
                        <p id="orgName" className="text-normal text-base margin-bottom-0">
                            Recipients
                        </p>
                        {destinations && (
                            <p className="margin-top-05">
                                {destinations}
                            </p>
                        )}
                        {(!destinations) && (
                            <p className="margin-top-05">There are no known recipients at this time.</p>
                        )}
                    </div>
                </div>
            )}

            {warnings.length > 0 && (
                <div>
                    <div className="usa-alert usa-alert--warning">
                        <div className="usa-alert__body">
                            <h4 className="usa-alert__heading">Alert: Additional Edits Requested</h4>
                            <p className="usa-alert__text">
                                Your file has been accepted. However, we detected fields that are unusable. These
                                are crucial for public health action. Please consider editing your file and uploading a
                                new  that addresses these issues.
                            </p>
                        </div>
                    </div>
                    <ul>
                        {warnings.map((w, i) => {
                            return (<li key={i}>
                                {w['id'] && (<span>{w['id']}: </span>)}{w['details']}
                            </li>);
                        })}
                    </ul>
                </div>
            )}

            {errors.length > 0 && (
                <div>
                    <div className="usa-alert usa-alert--error" role="alert">
                        <div className="usa-alert__body">
                            <h4 className="usa-alert__heading">Error: File not accepted</h4>
                            <p className="usa-alert__text">
                                Please resolve the errors below and upload your edited file. Your file has not been
                                accepted.
                            </p>
                        </div>
                    </div>
                    <ul>
                        {errors.map((e, i) => {
                            return (<li key={i}>{e['details']}</li>);
                        })}
                    </ul>
                </div>
            )}

            {httpErrors && (
                <div className="usa-alert usa-alert--error" role="alert">
                    <div className="usa-alert__body">
                        <h4 className="usa-alert__heading">Error</h4>
                        <p className="usa-alert__text">
                            {httpErrors}
                        </p>
                    </div>
                </div>
            )}

            <Form onSubmit={(e) => handleSubmit(e)}>
                <FormGroup className="margin-bottom-3">
                    <Label className="font-sans-xs" id="upload-csv-input-label" htmlFor="upload-csv-input">
                        Upload your COVID-19 lab results as a .CSV.
                    </Label>
                    <FileInput
                        id="upload-csv-input"
                        name="upload-csv-input"
                        aria-describedby="upload-csv-input-label"
                        accept="text/csv"
                        onChange={(e) => handleChange(e)}
                        required
                    />
                </FormGroup>
                <Button type="submit" disabled={isSubmitting}>
                {isSubmitting && (
                        <i
                        className="fa fa-refresh fa-spin"
                        style={{ marginRight: "5px" }}
                        />
                    )
                }

                {!isSubmitting && <span>Upload my edited file</span>}
                {isSubmitting && <span>Processing file...</span>}
                    
                    

                </Button>
            </Form>
        </div>
    );
}

export default Upload;
