<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<soapMockResponse>
    <body>&lt;soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:web="http://nedss.state.pa.us/2012/B01/elrwcf"&gt;
   &lt;soapenv:Header/&gt;
   &lt;soapenv:Body&gt;
      &lt;web:UploadFilesResponse&gt;
          &lt;UserName&gt;${BODY_XPATH(expression="//Envelope/Body/UploadFiles/cred/UserName")}&lt;/UserName&gt;
          &lt;TimeStamp&gt;${BODY_XPATH(expression="//Envelope/Body/UploadFiles/cred/TimeStamp")}&lt;/TimeStamp&gt;
          &lt;Message&gt;Successful submission! Thank you for your business.&lt;/Message&gt;
      &lt;/web:UploadFilesResponse&gt;
   &lt;/soapenv:Body&gt;
&lt;/soapenv:Envelope&gt;</body>
    <contentEncodings/>
    <httpHeaders/>
    <httpStatusCode>200</httpStatusCode>
    <id>PRBkKQ</id>
    <name>Auto-generated mocked response</name>
    <operationId>CTZklI</operationId>
    <status>ENABLED</status>
    <usingExpressions>true</usingExpressions>
    <xpathExpressions/>
</soapMockResponse>
