# Handling multiple HL7 and FHIR specs

_This approach is no longer being pursued in favor of a catchall data structure. This document is preserved for
reference purposes only._

## Problem

ReportStream uses the HL7 -> FHIR inventory as a guide for how to convert HL7 messages into FHIR; the inventory targets
multiple version of HL7 in order to be forward and backwards compatible. The goal for the ReportStream implementation
was to be able to losslessly translate a valid NIST ELR 2.5.1 message to FHIR R4 and back. Under the hood, the actual
implementation maps a specific version of HL7 as represented in a HAPI HL7 java class, which in this case is a
HAPI HL7 2.7 structure into the HAPI FHIR R4 structures.

For the most part, this simply works. The HL7 specs are designed to be mostly backwards compatible and the NIST ELR
2.5.1 spec almost entirely lines up with what is contained in the HAPI 2.7 java message. However, there are a few
problematic edge cases:

- how accurately the mappings go from HL7->FHIR->HL7 is highly dependent on what data is coming in
    - if a message contains an HL7 field that is deprecated in the NIST ELR spec 2.5.1 and not mapped in the FHIR
      inventory it will currently be lost in translation
- there are some fields that are deprecated in the 2.7 HAPI structures but allowed in the NIST ELR 2.5.1
    - these required special workarounds in the mappings
- there are cases where the NIST ELR 2.5.1 spec and the HAPI structures are incompatible
    - `CE` is a valid value for `OBX.2` in the NIST ELR 2.5.1 spec, but disallowed in the HAPI 2.7 structures
- validation becomes a hard problem as the message structure in code does not always match the structure and rules as
  defined by NIST

To summarize, the current approach to how different specs are handled is ambiguous and can result in both lost data and
messages that can not be properly translated because the targeted specification currently does not match the underlying java
representation

## Context

HL7v2 has a concept of a conformance profile that serves as a description of how a particular HL7v2 message can be
constructed and constraints on the message.

This includes:

- The structure of the groups of the segments
- The shape of the individual segments
- Specialized version of data types
    - i.e. the NIST ELR 2.5.1 conformance profile defines a `CWE_ELR` datatype
- Any constraints
    - i.e. if a field is repeatable

There are two different versions of conformance profiles (samples of which are in this directory):

- one is supported by the HAPI Source gen tool
- one can be constructed by the NIST IGAMT tool

At a high level, both versions can be referenced in order to create a HAPI java message that specifically implements the
profile.

## Goal

The goal is to create a system that can deterministically handle mapping from HL7->FHIR->HL7 when ReportStream
knows that the message lines up with a defined conformance profile allowing us to guarantee senders/receivers a lossless
translation.

## Approach

### Short term handling for new conformance profiles

The overall proposal is to ask senders to use `MSH.21` to indicate which conformance profile they are targeting. If
ReportStream can not determine an implemented conformance profile (either because `MSH.21` is not valued or we have not
implemented the profile), it will continue to fallback to comprehensive HL7->FHIR->HL7 mappings. This means that
receivers can continue to get their messages with a high degree of fidelity just without the absolute guarantee of all
the data making it through.

For each conformance profile ReportStream decides to support, the following steps are taken:

1. Create an implementation of `AbstractMessage` that matches the conformance profile.
    - The HAPI HL7 library provides a tool for generating these from a conformance profile. See the section below for
      more details
2. Copy the generic HL7->FHIR mappings based on the FHIR inventory and then update them to exactly reflect what the
   conformance profile specifies.
    - This work could entail removing schemas for deprecated fields or creating new custom data types (i.e. the radxmars
      spec defines a `CWE_NIH` type)
3. Extend the generic FHIR -> HL7 mapping and update it to reflect the conformance profile
    - That work would entail disabling certain elements if the field is deprecated or mapping custom datatypes
4. Create new tests for each segment and custom data type

### Converting between specs

One additional complexity to consider is how to convert between different conformance profiles; imagine a sender with
NIST ELR 2.5.1 messages and one receiver that is getting NIST ELR 2.5.1 and another that is receiving radxmars. The
actual mechanics of how to do this would need to be handled after a comparison of the two conformance profiles occurs,
but once that comparison is done FHIR transforms can be written to modify the bundle, so it can be translated
successfully to a different conformance profile.

### Long term: code generator

If ReportStream intends to support many different profiles, authoring the java classes by hand will not scale over the
long term; additionally, as profiles change (i.e. the radxmars one is still evolving), updating existing java classes
will be error-prone. A long term solution to this is to implement a solution that involves parsing the XML files and
using a code generator to create the required java classes.

#### Existing code generator solution

The HAPI HL7 dependency does implement a source code generator that _works_, but with a few issues:

- it does not support the XSD that IGAMT uses to generate conformance profiles
- there is a bug in the code for generating datatypes which means datatypes need to be created manually
- it only exists as Maven plugin and is not compatible with the ReportStream gradle build

Example `pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>org.example</groupId>
    <artifactId>TEST_PROJEFT</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>
    <dependencies>
        <!-- https://mvnrepository.com/artifact/ca.uhn.hapi/hapi-base -->
        <dependency>
            <groupId>ca.uhn.hapi</groupId>
            <artifactId>hapi-base</artifactId>
            <version>2.5.1</version>
        </dependency>
        <!-- https://mvnrepository.com/artifact/ca.uhn.hapi/hapi-structures-v25 -->
        <dependency>
            <groupId>ca.uhn.hapi</groupId>
            <artifactId>hapi-structures-v25</artifactId>
            <version>2.5.1</version>
        </dependency>
        <!-- https://mvnrepository.com/artifact/ca.uhn.hapi/hapi-sourcegen -->
        <dependency>
            <groupId>ca.uhn.hapi</groupId>
            <artifactId>hapi-sourcegen</artifactId>
            <version>2.5.1</version>
        </dependency>

    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>ca.uhn.hapi</groupId>
                <artifactId>hapi-sourcegen</artifactId>
                <version>2.5.1</version>
                <executions>
                    <execution>
                        <id>nistelr251</id>
                        <phase>generate-sources</phase>
                        <goals>
                            <goal>confgen</goal>
                        </goals>
                        <configuration>

                            <!-- This is the conformance profile file to use -->
                            <profile>${project.basedir}/src/main/resources/hl7/profiles/nist-elr-2.5.1.xml</profile>

                            <!-- Place generated Java source here -->
                            <targetDirectory>${project.basedir}/src/main/java</targetDirectory>

                            <!-- Generated classes will be placed here -->
                            <packageName>gov.cdc.nist</packageName>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

and then code generation can be invoked with `mvn hapi-sourcegen:confgen@nist`

#### A reportstream code generator tool

Some potential functionality that a new tool could or should implement would be:

- support for conformance profiles created by the IGAMT tool
- integration into the ReportStream build process
- validators
- automatic mappings for HL7 -> FHIR
- FHIR transforms to support translating a FHIR message generated from one conformance profile to a different (i.e. NIST
  ELR 2.5.1 -> FHIR -> radxmars)
