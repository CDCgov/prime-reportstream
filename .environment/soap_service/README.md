# README for CastleMock Configuration Directory
## Introduction
As part of us supporting SOAP, we are using CastleMock to allow us to verify the connection to SOAP services.
CastleMock is useful in that you can upload a WSDL file to the application and it will allow us mock out
different responses based on the WSDL and XSL logic.

This folder is the actual configuration for the instance running in Docker.

While CastleMock is pretty easy to use, and is powerful for how we want to use it, it has one unfortunate
shortcoming: its documentation is very weak.

The purpose of this README is to remedy that so future engineers who have to support
this will have the ability to with a minimum of pain.

## Structure

In CastleMock there are four top-level folders:

```text
/
|__ configuration
|__ rest
|__ soap
|__ user
```

### Top-Level Folders
- **configuration** - I'll be honest, I'm not sure what this is used for, if at all. I've never seen it have anything in it.
- **rest** - CastleMock has the ability to also mock out REST webservices. We're not using it for that. Ignore that too.
- **soap** - This is all of the configuration for SOAP web services. We'll do a deeper dive on this in a minute
- **user** - User details are stored here. Typically, there's just the admin user in here, though you do have the ability to add other users to CastleMock if you so choose.

### SOAP Folders
The structure in the SOAP directory looks like this:

```text
/
|
|-- soap
    |__ event
        |__ v1
    |__ operation
        |__ v2
    |__ port
        |__ v2
    |__ project
        |__ v2
    |__ resource
        |__ v2
    |__ response
        |__ v2
```

Each of the folders has a subfolder as shown below. Event is the only one that has a `v1` directory, the rest have `v2`.
For our purposes it doesn't really seem to matter. We care about what's inside the folder.

- **event** - Contains `.event` files. Event files are the requests that have come in and the response generated. It's the audit log.
- **operation** - Contains `.opr` files. Opr files are each operation the SOAP web service supports. This is typically generate from the WSDL that is imported.
- **port** - Contains `.port` files. Port files represent the ports as described in the WSDL file
- **project** - Contains `.prj` files, which have some metadata about the project that the operations and responses and ports are associated with.
- **resource** - Contains `.rsc` files that are the actual WSDL files for a service that have been imported to a project.
- **response** - Contains `.res` files that represent the possible responses CastleMock can return for a SOAP request. A response has a `operationId` tag that it uses to map back to the actions it belongs to.

### Sample WSDL File
In order to better understand the above, below is a sample WSDL file. This was taken from https://tomd.xyz/wsdl/

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<wsdl:definitions xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/"
                  xmlns:tns="http://www.cleverbuilder.com/BookService/"
                  xmlns:wsdl="http://schemas.xmlsoap.org/wsdl/"
                  xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                  name="BookService"
                  targetNamespace="http://www.cleverbuilder.com/BookService/">
  <wsdl:documentation>Definition for a web service called BookService,
    which can be used to add or retrieve books from a collection.
  </wsdl:documentation>

  <!--
      The `types` element defines the data types (XML elements)
      that are used by the web service.
   -->
  <wsdl:types>
    <xsd:schema targetNamespace="http://www.cleverbuilder.com/BookService/">
      <xsd:element name="Book">
        <xsd:complexType>
          <xsd:sequence>
            <xsd:element name="ID" type="xsd:string" minOccurs="0"/>
            <xsd:element name="Title" type="xsd:string"/>
            <xsd:element name="Author" type="xsd:string"/>
          </xsd:sequence>
        </xsd:complexType>
      </xsd:element>
      <xsd:element name="Books">
        <xsd:complexType>
          <xsd:sequence>
            <xsd:element ref="tns:Book" minOccurs="0" maxOccurs="unbounded"/>
          </xsd:sequence>
        </xsd:complexType>
      </xsd:element>

      <xsd:element name="GetBook">
        <xsd:complexType>
          <xsd:sequence>
            <xsd:element name="ID" type="xsd:string"/>
          </xsd:sequence>
        </xsd:complexType>
      </xsd:element>
      <xsd:element name="GetBookResponse">
        <xsd:complexType>
          <xsd:sequence>
            <xsd:element ref="tns:Book" minOccurs="0" maxOccurs="1"/>
          </xsd:sequence>
        </xsd:complexType>
      </xsd:element>

      <xsd:element name="AddBook">
        <xsd:complexType>
          <xsd:sequence>
            <xsd:element ref="tns:Book" minOccurs="1" maxOccurs="1"/>
          </xsd:sequence>
        </xsd:complexType>
      </xsd:element>
      <xsd:element name="AddBookResponse">
        <xsd:complexType>
          <xsd:sequence>
            <xsd:element ref="tns:Book" minOccurs="0" maxOccurs="1"/>
          </xsd:sequence>
        </xsd:complexType>
      </xsd:element>
      <xsd:element name="GetAllBooks">
        <xsd:complexType/>
      </xsd:element>
      <xsd:element name="GetAllBooksResponse">
        <xsd:complexType>
          <xsd:sequence>
            <xsd:element ref="tns:Book" minOccurs="0" maxOccurs="unbounded"/>
          </xsd:sequence>
        </xsd:complexType>
      </xsd:element>
    </xsd:schema>
  </wsdl:types>


  <!--
      A wsdl `message` element is used to define a message
      exchanged between a web service, consisting of zero
      or more `part`s.
   -->

  <wsdl:message name="GetBookRequest">
    <wsdl:part element="tns:GetBook" name="parameters"/>
  </wsdl:message>
  <wsdl:message name="GetBookResponse">
    <wsdl:part element="tns:GetBookResponse" name="parameters"/>
  </wsdl:message>
  <wsdl:message name="AddBookRequest">
    <wsdl:part name="parameters" element="tns:AddBook"></wsdl:part>
  </wsdl:message>
  <wsdl:message name="AddBookResponse">
    <wsdl:part name="parameters" element="tns:AddBookResponse"></wsdl:part>
  </wsdl:message>
  <wsdl:message name="GetAllBooksRequest">
    <wsdl:part name="parameters" element="tns:GetAllBooks"></wsdl:part>
  </wsdl:message>
  <wsdl:message name="GetAllBooksResponse">
    <wsdl:part name="parameters" element="tns:GetAllBooksResponse"></wsdl:part>
  </wsdl:message>

  <!--
      A WSDL `portType` is used to combine multiple `message`s
      (e.g. input, output) into a single operation.

      Here we define three synchronous (input/output) operations
      and the `message`s that must be used for each.
   -->
  <wsdl:portType name="BookService">
    <wsdl:operation name="GetBook">
      <wsdl:input message="tns:GetBookRequest"/>
      <wsdl:output message="tns:GetBookResponse"/>
    </wsdl:operation>
    <wsdl:operation name="AddBook">
      <wsdl:input message="tns:AddBookRequest"></wsdl:input>
      <wsdl:output message="tns:AddBookResponse"></wsdl:output>
    </wsdl:operation>
    <wsdl:operation name="GetAllBooks">
      <wsdl:input message="tns:GetAllBooksRequest"></wsdl:input>
      <wsdl:output message="tns:GetAllBooksResponse"></wsdl:output>
    </wsdl:operation>
  </wsdl:portType>

  <!--
      The `binding` element defines exactly how each
      `operation` will take place over the network.
      In this case, we are using SOAP.
   -->
  <wsdl:binding name="BookServiceSOAP" type="tns:BookService">
    <soap:binding style="document"
                  transport="http://schemas.xmlsoap.org/soap/http"/>
    <wsdl:operation name="GetBook">
      <soap:operation
              soapAction="http://www.cleverbuilder.com/BookService/GetBook"/>
      <wsdl:input>
        <soap:body use="literal"/>
      </wsdl:input>
      <wsdl:output>
        <soap:body use="literal"/>
      </wsdl:output>
    </wsdl:operation>
    <wsdl:operation name="AddBook">
      <soap:operation
              soapAction="http://www.cleverbuilder.com/BookService/AddBook"/>
      <wsdl:input>
        <soap:body use="literal"/>
      </wsdl:input>
      <wsdl:output>
        <soap:body use="literal"/>
      </wsdl:output>
    </wsdl:operation>
    <wsdl:operation name="GetAllBooks">
      <soap:operation
              soapAction="http://www.cleverbuilder.com/BookService/GetAllBooks"/>
      <wsdl:input>
        <soap:body use="literal"/>
      </wsdl:input>
      <wsdl:output>
        <soap:body use="literal"/>
      </wsdl:output>
    </wsdl:operation>
  </wsdl:binding>

  <!--
      The `service` element finally says where the service
      can be accessed from - in other words, its endpoint.
   -->
  <wsdl:service name="BookService">
    <wsdl:port binding="tns:BookServiceSOAP" name="BookServiceSOAP">
      <soap:address location="http://www.example.org/BookService"/>
    </wsdl:port>
  </wsdl:service>
</wsdl:definitions>
```

## Notes
In order to login to the CastleMock service, start Docker, and then navigate to http://localhost:8087/CastleMock. Login is admin/admin.

## Further Reading
- https://en.wikipedia.org/wiki/Web_Services_Description_Language
- https://en.wikipedia.org/wiki/SOAP
- https://castlemock.github.io