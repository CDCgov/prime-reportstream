import { EmailContentBlock } from "../EmailContentBlock/EmailContentBlock";

export function EmailFooter() {
    return (
        <EmailContentBlock type="footer">
            This is an automatically generated message from
            <a href="https://www.okta.com" style={{ color: "rgb(97, 97, 97)" }}>
                Okta
            </a>
            . Replies are not monitored or answered.
        </EmailContentBlock>
    );
}
