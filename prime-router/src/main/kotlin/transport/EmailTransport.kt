package gov.cdc.prime.router.transport

<<<<<<< HEAD
import com.sendgrid.*
import com.sendgrid.helpers.mail.Mail
import com.sendgrid.helpers.mail.objects.*
=======
>>>>>>> 714c9675eaeaa2acab01e8495c93a41cf601c373
import gov.cdc.prime.router.OrganizationService
import gov.cdc.prime.router.azure.DatabaseAccess

class EmailTransport : ITransport {

    override fun send(
        orgName: String,
        transport: OrganizationService.Transport,
        header: DatabaseAccess.Header,
        contents: ByteArray
    ): Boolean {
<<<<<<< HEAD

        val emailTransport = transport as OrganizationService.Email

        val from = Email("qtv1@cdc.gov")
        val subject = "Your results are ready"
        val to = Email("qtv1@cdc.gov")
        val html = """ 
        <html xmlns=\"http://www.w3.org/1999/xhtml\">
        <head>
          <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />
          <meta name=\"viewport\" content=\"width=device-width\" />
          <title>PRIME Data Hub - Test results ready</title>
          <link rel=\"stylesheet\" href=\"css/foundation-emails.css\" />
          <link rel=\"stylesheet\" href=\"css/custom.css\" />
        </head>
        <body>
          <!-- <style> -->
          <table class=\"body\">
            <tr>
              <td class=\"float-center\" align=\"center\" valign=\"top\">
                <center>
        
                  <table class=\"container\">
                    <tr>
                      <td>
                        <table class=\"spacer\">
                          <tbody>
                            <tr>
                              <td height=\"20\" style=\"font-size:20px;line-height:20px;\">&nbsp;</td>
                            </tr>
                          </tbody>
                        </table>
                      </td>
                    </tr>
                    <tr>
                      <td>
        
                        <table class=\"row hub-header\">
                          <tr>
                            <th class=\"small-12 first columns\">
                              <strong>PRIME Data Hub</strong>
                            </th>
                            <th class=\"expander\"></th>
                          </tr>
                        </table>
                        <table class=\"row hub-body\">
                          <tr>
                            <th class=\"small-12 first columns\">
                              <table class=\"spacer\">
                                <tbody>
                                  <tr>
                                    <td height=30 style=\"font-size:30px;line-height:30px;\">&nbsp;</td>
                                  </tr>
                                </tbody>
                              </table>
                              <h1>COVID-19 test data is ready for download</h1>
                              <table class=\"spacer\">
                                <tbody>
                                  <tr>
                                    <td height=10 style=\"font-size:10px;line-height:10px;\">&nbsp;</td>
                                  </tr>
                                </tbody>
                              </table>
                              <p><strong>{{ County name }} county, {{ ST }}</strong></p>
                              <p>
                                <a href=\"{{ link }}\">Download daily test results for {{ Day name }} {{ Month name }} {{ Day }}, {{ YYYY }} (CSV)</a>
                              </p>
                              <table class=\"spacer\">
                                <tbody>
                                  <tr>
                                    <td height=10 style=\"font-size:10px;line-height:10px;\">&nbsp;</td>
                                  </tr>
                                </tbody>
                              </table>
                              <p>Facilities reporting data today:</p>
                              <ul>
                                <!-- {{ for location in locations }} -->
                                <li>Oz</li>
                                <li>Narnia</li>
                                <li>Goblin City</li>
                                <li>Dinotopia</li>
                                <li> ... </li>
                                <!-- {{ endfor }} -->
                              </ul>
                            </th>
                            <th class=\"expander\"></th>
                          </tr>
                        </table>
                        <table class=\"row hub-footer\">
                          <tr>
                            <td>
                            <table class=\"spacer hub-spacer-footer\">
                              <tbody>
                                <tr>
                                  <td height=\"20\" style=\"font-size:20px;line-height:20px;\">&nbsp;</td>
                                </tr>
                              </tbody>
                            </table>
                          </td>
                          </tr>
                          <tr>
                            <th class=\"small-12 first columns\">
                              <p>
                                <strong>PRIME Data Hub</strong> is a joint project of the Centers for Disease Control and Prevention and the U.S. Digital Service.
                              </p>
                              <p>Per the PRIME Data Hub retention policy, test results are available to download for seven days after receipt of this email. Failure to download data within the alotted time frame may result in the loss of COVID-19 test results.<br /><br /></p>
                              <p>
                                <strong>For support, contact:</strong>
                              </p>
                              <p>
                                {{ email@email.com }}<br />
                                {{ (555) 867-5309 }}
                              </p>
                              <p>
                                {{ Street address line 1 }}<br />
                                {{ Street address line 2 }}
                              </p>
                            </th>
                            <th class=\"expander\"></th>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </center>
              </td>
            </tr>
          </table>
        </body>
        </html>
        """

        val content = Content("text/html", html)
        val mail = Mail(from, subject, to, content)

        val sg = SendGrid(System.getenv("SENDGRID_API_KEY"))
        val request = Request()
        request.setMethod(Method.POST)
        request.setEndpoint("mail/send")
        request.setBody(mail.build())
        val response = sg.api(request)
        System.out.println(response.getStatusCode())
        System.out.println(response.getBody())
        System.out.println(response.getHeaders())

=======
>>>>>>> 714c9675eaeaa2acab01e8495c93a41cf601c373
        return true
    }
}