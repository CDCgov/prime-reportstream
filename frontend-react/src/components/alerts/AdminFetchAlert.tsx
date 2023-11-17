import Alert from "../../shared/Alert/Alert";

export default function AdminFetchAlert() {
    return (
        <Alert type="error" heading="Cannot fetch Organization data as admin">
            {"Please try again as an Organization"}
        </Alert>
    );
}
