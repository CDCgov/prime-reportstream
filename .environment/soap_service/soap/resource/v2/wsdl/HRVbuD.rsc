<?xml version="1.0" encoding="utf-8" standalone="no"?>
<wsdl:definitions xmlns:msc="http://schemas.microsoft.com/ws/2005/12/wsdl/contract" xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/" xmlns:soap12="http://schemas.xmlsoap.org/wsdl/soap12/" xmlns:soapenc="http://schemas.xmlsoap.org/soap/encoding/" xmlns:tns="http://nedss.state.pa.us/2012/B01/elrwcf" xmlns:wsa="http://schemas.xmlsoap.org/ws/2004/08/addressing" xmlns:wsa10="http://www.w3.org/2005/08/addressing" xmlns:wsam="http://www.w3.org/2007/05/addressing/metadata" xmlns:wsap="http://schemas.xmlsoap.org/ws/2004/08/addressing/policy" xmlns:wsaw="http://www.w3.org/2006/05/addressing/wsdl" xmlns:wsdl="http://schemas.xmlsoap.org/wsdl/" xmlns:wsp="http://schemas.xmlsoap.org/ws/2004/09/policy" xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd" xmlns:wsx="http://schemas.xmlsoap.org/ws/2004/09/mex" xmlns:xsd="http://www.w3.org/2001/XMLSchema" name="ELRService" targetNamespace="http://nedss.state.pa.us/2012/B01/elrwcf">
    <wsp:Policy wsu:Id="BasicHttpBinding_IUploadFile_policy">
        <wsp:ExactlyOne>
            <wsp:All>
                <sp:TransportBinding xmlns:sp="http://schemas.xmlsoap.org/ws/2005/07/securitypolicy">
                    <wsp:Policy>
                        <sp:TransportToken>
                            <wsp:Policy>
                                <sp:HttpsToken RequireClientCertificate="false"/>
                            </wsp:Policy>
                        </sp:TransportToken>
                        <sp:AlgorithmSuite>
                            <wsp:Policy>
                                <sp:Basic256/>
                            </wsp:Policy>
                        </sp:AlgorithmSuite>
                        <sp:Layout>
                            <wsp:Policy>
                                <sp:Strict/>
                            </wsp:Policy>
                        </sp:Layout>
                    </wsp:Policy>
                </sp:TransportBinding>
            </wsp:All>
        </wsp:ExactlyOne>
    </wsp:Policy>
    <wsdl:types>
        <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema" elementFormDefault="qualified" targetNamespace="http://nedss.state.pa.us/2012/B01/elrwcf">
            <xs:element name="UploadFiles">
                <xs:complexType>
                    <xs:sequence>
                        <xs:element minOccurs="0" name="cred" nillable="true" type="tns:Credentials"/>
                        <xs:element minOccurs="0" name="arrLabFile" nillable="true" type="tns:ArrayOfLabFile"/>
                    </xs:sequence>
                </xs:complexType>
            </xs:element>
            <xs:complexType name="Credentials">
                <xs:sequence>
                    <xs:element minOccurs="0" name="Password" nillable="true" type="xs:string"/>
                    <xs:element minOccurs="0" name="TimeStamp" nillable="true" type="xs:string"/>
                    <xs:element minOccurs="0" name="UserName" nillable="true" type="xs:string"/>
                </xs:sequence>
            </xs:complexType>
            <xs:element name="Credentials" nillable="true" type="tns:Credentials"/>
            <xs:complexType name="ArrayOfLabFile">
                <xs:sequence>
                    <xs:element maxOccurs="unbounded" minOccurs="0" name="LabFile" nillable="true" type="tns:LabFile"/>
                </xs:sequence>
            </xs:complexType>
            <xs:element name="ArrayOfLabFile" nillable="true" type="tns:ArrayOfLabFile"/>
            <xs:complexType name="LabFile">
                <xs:sequence>
                    <xs:element minOccurs="0" name="FileName" nillable="true" type="xs:string"/>
                    <xs:element minOccurs="0" name="Index" type="xs:int"/>
                    <xs:element minOccurs="0" name="bytLabFile" nillable="true" type="xs:base64Binary"/>
                    <xs:element minOccurs="0" name="bytSignatureToStore" nillable="true" type="xs:base64Binary"/>
                    <xs:element minOccurs="0" name="purpose" nillable="true" type="xs:string"/>
                    <xs:element minOccurs="0" name="strFileExtension" nillable="true" type="xs:string"/>
                </xs:sequence>
            </xs:complexType>
            <xs:element name="LabFile" nillable="true" type="tns:LabFile"/>
            <xs:element name="UploadFilesResponse">
                <xs:complexType>
                    <xs:sequence>
                        <xs:element minOccurs="0" name="UploadFilesResult" nillable="true" type="tns:ArrayOfELRWcfReturnMessage"/>
                    </xs:sequence>
                </xs:complexType>
            </xs:element>
            <xs:complexType name="ArrayOfELRWcfReturnMessage">
                <xs:sequence>
                    <xs:element maxOccurs="unbounded" minOccurs="0" name="ELRWcfReturnMessage" nillable="true" type="tns:ELRWcfReturnMessage"/>
                </xs:sequence>
            </xs:complexType>
            <xs:element name="ArrayOfELRWcfReturnMessage" nillable="true" type="tns:ArrayOfELRWcfReturnMessage"/>
            <xs:complexType name="ELRWcfReturnMessage">
                <xs:sequence>
                    <xs:element minOccurs="0" name="FileName" nillable="true" type="xs:string"/>
                    <xs:element minOccurs="0" name="Index" type="xs:int"/>
                    <xs:element minOccurs="0" name="Message" nillable="true" type="xs:string"/>
                    <xs:element minOccurs="0" name="Success" nillable="true" type="xs:string"/>
                </xs:sequence>
            </xs:complexType>
            <xs:element name="ELRWcfReturnMessage" nillable="true" type="tns:ELRWcfReturnMessage"/>
        </xs:schema>
        <xs:schema xmlns:tns="http://schemas.microsoft.com/2003/10/Serialization/" xmlns:xs="http://www.w3.org/2001/XMLSchema" attributeFormDefault="qualified" elementFormDefault="qualified" targetNamespace="http://schemas.microsoft.com/2003/10/Serialization/">
            <xs:element name="anyType" nillable="true" type="xs:anyType"/>
            <xs:element name="anyURI" nillable="true" type="xs:anyURI"/>
            <xs:element name="base64Binary" nillable="true" type="xs:base64Binary"/>
            <xs:element name="boolean" nillable="true" type="xs:boolean"/>
            <xs:element name="byte" nillable="true" type="xs:byte"/>
            <xs:element name="dateTime" nillable="true" type="xs:dateTime"/>
            <xs:element name="decimal" nillable="true" type="xs:decimal"/>
            <xs:element name="double" nillable="true" type="xs:double"/>
            <xs:element name="float" nillable="true" type="xs:float"/>
            <xs:element name="int" nillable="true" type="xs:int"/>
            <xs:element name="long" nillable="true" type="xs:long"/>
            <xs:element name="QName" nillable="true" type="xs:QName"/>
            <xs:element name="short" nillable="true" type="xs:short"/>
            <xs:element name="string" nillable="true" type="xs:string"/>
            <xs:element name="unsignedByte" nillable="true" type="xs:unsignedByte"/>
            <xs:element name="unsignedInt" nillable="true" type="xs:unsignedInt"/>
            <xs:element name="unsignedLong" nillable="true" type="xs:unsignedLong"/>
            <xs:element name="unsignedShort" nillable="true" type="xs:unsignedShort"/>
            <xs:element name="char" nillable="true" type="tns:char"/>
            <xs:simpleType name="char">
                <xs:restriction base="xs:int"/>
            </xs:simpleType>
            <xs:element name="duration" nillable="true" type="tns:duration"/>
            <xs:simpleType name="duration">
                <xs:restriction base="xs:duration">
                    <xs:pattern value="\-?P(\d*D)?(T(\d*H)?(\d*M)?(\d*(\.\d*)?S)?)?"/>
                    <xs:minInclusive value="-P10675199DT2H48M5.4775808S"/>
                    <xs:maxInclusive value="P10675199DT2H48M5.4775807S"/>
                </xs:restriction>
            </xs:simpleType>
            <xs:element name="guid" nillable="true" type="tns:guid"/>
            <xs:simpleType name="guid">
                <xs:restriction base="xs:string">
                    <xs:pattern value="[\da-fA-F]{8}-[\da-fA-F]{4}-[\da-fA-F]{4}-[\da-fA-F]{4}-[\da-fA-F]{12}"/>
                </xs:restriction>
            </xs:simpleType>
            <xs:attribute name="FactoryType" type="xs:QName"/>
            <xs:attribute name="Id" type="xs:ID"/>
            <xs:attribute name="Ref" type="xs:IDREF"/>
        </xs:schema>
    </wsdl:types>
    <wsdl:message name="IUploadFile_UploadFiles_InputMessage">
        <wsdl:part element="tns:UploadFiles" name="parameters"/>
    </wsdl:message>
    <wsdl:message name="IUploadFile_UploadFiles_OutputMessage">
        <wsdl:part element="tns:UploadFilesResponse" name="parameters"/>
    </wsdl:message>
    <wsdl:portType name="IUploadFile">
        <wsdl:operation name="UploadFiles">
            <wsdl:input message="tns:IUploadFile_UploadFiles_InputMessage" wsaw:Action="http://nedss.state.pa.us/2012/B01/elrwcf/IUploadFile/UploadFiles"/>
            <wsdl:output message="tns:IUploadFile_UploadFiles_OutputMessage" wsaw:Action="http://nedss.state.pa.us/2012/B01/elrwcf/IUploadFile/UploadFilesResponse"/>
        </wsdl:operation>
    </wsdl:portType>
    <wsdl:binding name="BasicHttpBinding_IUploadFile" type="tns:IUploadFile">
        <wsp:PolicyReference URI="#BasicHttpBinding_IUploadFile_policy"/>
        <soap:binding transport="http://schemas.xmlsoap.org/soap/http"/>
        <wsdl:operation name="UploadFiles">
            <soap:operation soapAction="http://nedss.state.pa.us/2012/B01/elrwcf/IUploadFile/UploadFiles" style="document"/>
            <wsdl:input>
                <soap:body use="literal"/>
            </wsdl:input>
            <wsdl:output>
                <soap:body use="literal"/>
            </wsdl:output>
        </wsdl:operation>
    </wsdl:binding>
    <wsdl:service name="ELRService">
        <wsdl:port binding="tns:BasicHttpBinding_IUploadFile" name="BasicHttpBinding_IUploadFile">
            <soap:address location="https://www.erstest.health.pa.gov/ELRWCF/ELRService.svc/ELR"/>
        </wsdl:port>
        <wsdl:port binding="tns:BasicHttpBinding_IUploadFile" name="BasicHttpBinding_IUploadFile1">
            <soap:address location="https://www.erstest.health.pa.gov/ELRWCF/ELRService.svc/Test"/>
        </wsdl:port>
    </wsdl:service>
</wsdl:definitions>
