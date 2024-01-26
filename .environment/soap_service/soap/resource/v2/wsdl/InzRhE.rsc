<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<wsdl:definitions xmlns:http="http://schemas.xmlsoap.org/wsdl/http/" xmlns:mime="http://schemas.xmlsoap.org/wsdl/mime/" xmlns:ns0="https://www.healthy.arkansas.gov/STAGEService" xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/" xmlns:soap12="http://schemas.xmlsoap.org/wsdl/soap12/" xmlns:wsaw="http://www.w3.org/2006/05/addressing/wsdl" xmlns:wsdl="http://schemas.xmlsoap.org/wsdl/" xmlns:xs="http://www.w3.org/2001/XMLSchema" targetNamespace="https://www.healthy.arkansas.gov/STAGEService">
    <wsp:Policy xmlns:wsp="http://schemas.xmlsoap.org/ws/2004/09/policy" xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd" wsu:Id="servicePolicy">
        <wsp:ExactlyOne>
            <wsp:All>
                <sp:TransportBinding xmlns:sp="http://schemas.xmlsoap.org/ws/2005/07/securitypolicy">
                    <wsp:Policy>
                        <sp:TransportToken>
                            <wsp:Policy>
                                <sp:HttpsToken RequireClientCertificate="true"/>
                            </wsp:Policy>
                        </sp:TransportToken>
                        <sp:AlgorithmSuite>
                            <wsp:Policy>
                                <sp:Basic128/>
                            </wsp:Policy>
                        </sp:AlgorithmSuite>
                        <sp:Layout>
                            <wsp:Policy>
                                <sp:Lax/>
                            </wsp:Policy>
                        </sp:Layout>
                        <sp:IncludeTimestamp/>
                    </wsp:Policy>
                </sp:TransportBinding>
                <wsam:Addressing xmlns:wsam="http://www.w3.org/2007/05/addressing/metadata" wsp:Optional="true">
                    <wsp:Policy>
                        <wsam:AnonymousResponses/>
                    </wsp:Policy>
                </wsam:Addressing>
            </wsp:All>
        </wsp:ExactlyOne>
    </wsp:Policy>
    <wsdl:documentation>Arkansas Department of Health web service for STAGE data and results for all registries.</wsdl:documentation>
    <wsdl:types>
        <xs:schema attributeFormDefault="unqualified" elementFormDefault="qualified" targetNamespace="https://www.healthy.arkansas.gov/STAGEService">
                
            <xs:element name="SubmitMessage">
                        
                <xs:complexType>
                                
                    <xs:sequence>
                                        
                        <xs:element name="payload" type="xs:string"/>
                                    
                    </xs:sequence>
                            
                </xs:complexType>
                    
            </xs:element>
                
            <xs:element name="SubmitMessageResponse">
                        
                <xs:complexType>
                                
                    <xs:sequence>
                                        
                        <xs:element name="payload" type="xs:string"/>
                                    
                    </xs:sequence>
                            
                </xs:complexType>
                    
            </xs:element>
                
            <xs:element name="SubmitMessageFault">
                        
                <xs:complexType>
                                
                    <xs:sequence>
                                        
                        <xs:element name="payload" type="xs:string"/>
                                    
                    </xs:sequence>
                            
                </xs:complexType>
                    
            </xs:element>
            
        </xs:schema>
    </wsdl:types>
    <wsdl:message name="SubmitMessageRequest">
        <wsdl:part element="ns0:SubmitMessage" name="parameters"/>
    </wsdl:message>
    <wsdl:message name="SubmitMessageResponse">
        <wsdl:part element="ns0:SubmitMessageResponse" name="parameters"/>
    </wsdl:message>
    <wsdl:message name="SubmitMessageFault">
        <wsdl:part element="ns0:SubmitMessageFault" name="parameters"/>
    </wsdl:message>
    <wsdl:portType xmlns:wsp="http://schemas.xmlsoap.org/ws/2004/09/policy" name="ADH_STAGEServicePortType" wsp:PolicyURIs="#servicePolicy">
        <wsdl:operation name="SubmitMessage">
            <wsdl:documentation>The operation receives a message and returns an ack on successful parsing of the message.</wsdl:documentation>
            <wsdl:input message="ns0:SubmitMessageRequest" wsaw:Action="urn:SubmitMessage"/>
            <wsdl:output message="ns0:SubmitMessageResponse" wsaw:Action="https://www.healthy.arkansas.gov/STAGEService/ADH_STAGEServicePortType/SubmitMessageResponse"/>
            <wsdl:fault message="ns0:SubmitMessageFault" name="SubmitMessageFault" wsaw:Action="https://www.healthy.arkansas.gov/STAGEService/ADH_STAGEServicePortType/SubmitMessageFault"/>
        </wsdl:operation>
    </wsdl:portType>
    <wsdl:binding name="ADH_STAGEServiceSoap12Binding" type="ns0:ADH_STAGEServicePortType">
        <soap12:binding style="document" transport="http://schemas.xmlsoap.org/soap/http"/>
        <wsaw:UsingAddressing wsdl:required="false"/>
        <wsdl:operation name="SubmitMessage">
            <soap12:operation soapAction="urn:SubmitMessage" style="document"/>
            <wsdl:input>
                <soap12:body use="literal"/>
            </wsdl:input>
            <wsdl:output>
                <soap12:body use="literal"/>
            </wsdl:output>
            <wsdl:fault name="SubmitMessageFault">
                <soap12:fault name="SubmitMessageFault" use="literal"/>
            </wsdl:fault>
        </wsdl:operation>
    </wsdl:binding>
    <wsdl:service name="ADH_STAGEService">
        <wsdl:port binding="ns0:ADH_STAGEServiceSoap12Binding" name="ADH_STAGEServiceHttpsSoap12Endpoint">
            <soap12:address location="https://rhapstage.adh.arkansas.gov:443/services/ADH_STAGEService.ADH_STAGEServiceHttpsSoap12Endpoint"/>
        </wsdl:port>
    </wsdl:service>
</wsdl:definitions>
