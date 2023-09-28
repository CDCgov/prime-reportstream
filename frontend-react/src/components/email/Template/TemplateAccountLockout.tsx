import { EmailContentBlock } from "../EmailContentBlock/EmailContentBlock";
import { EmailLayout } from "../EmailLayout/EmailLayout";

export function EmailTemplateAccountLockout() {
    // <tr>
    //     <td
    //         style="
    //             color: #5e5e5e;
    //             font-size: 22px;
    //             line-height: 22px;
    //         "
    //     >
    //         ${org.name} account locked
    //     </td>
    // </tr>
    // <tr>
    //     <td
    //         style="
    //             padding-top: 24px;
    //             vertical-align: bottom;
    //         "
    //     >
    //         Hi
    //         $!{StringTool.escapeHtml($!{user.profile.firstName})},
    //     </td>
    // </tr>
    // <tr>
    //     <td style="padding-top: 24px">
    //         Your Okta account has been locked due to too
    //         many failed login attempts.
    //     </td>
    // </tr>
    // <tr>
    //     <td style="padding-top: 24px">
    //         To reset your account, follow the link
    //         below:
    //     </td>
    // </tr>
    // <tr>
    //     <td align="center">
    //         <table
    //             border="0"
    //             cellpadding="0"
    //             cellspacing="0"
    //             valign="top"
    //         >
    //             <tr>
    //                 <td
    //                     align="center"
    //                     style="
    //                         height: 32px;
    //                         padding-top: 24px;
    //                     "
    //                 >
    //                     <a
    //                         href="${baseURL}/signin/unlock"
    //                         style="
    //                             text-decoration: none;
    //                             display: inline-block;
    //                         "
    //                         ><span
    //                             style="padding: 17px 16px 17px 16px; border: 1px solid; text-align: center; cursor: pointer; color: #fff; border-radius: 3px; background-color: ${brand.theme.primaryColor}; border-color: ${brand.theme.primaryColor}; box-shadow: 0 1px 0 ${brand.theme.primaryColor};"
    //                             >Unlock account</span
    //                         ></a
    //                     >
    //                 </td>
    //             </tr>
    //         </table>
    //     </td>
    // </tr>
    return (
        <EmailLayout>
            <EmailContentBlock type="header">
                {"${org.name}"} account locked
            </EmailContentBlock>
            <EmailContentBlock isFirst>
                Hi {"$!{StringTool.escapeHtml($!{user.profile.firstName})}"},
            </EmailContentBlock>
            <EmailContentBlock>
                Your Okta account has been locked due to too many failed login
                attempts.
            </EmailContentBlock>
        </EmailLayout>
    );
}

export default EmailTemplateAccountLockout;
