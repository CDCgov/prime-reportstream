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

## Notes
In order to login to the CastleMock service, start Docker, and then navigate to http://localhost:8087/CastleMock. Login is admin/admin.

## Further Reading
- https://en.wikipedia.org/wiki/Web_Services_Description_Language
- https://en.wikipedia.org/wiki/SOAP
- https://castlemock.github.io