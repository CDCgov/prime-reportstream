# FHIR Functions
FHIR functions are methods that can be run on a bundle in order to extract data from it (ex. retrieve an age or telephone area code).
When translating FHIR bundles to other formats, like HL7v2, it is necessary to extract data from the FHIR bundle, so 
that it can be moved over to the new format. We can often accomplish extracting the data we need via a simple FHIR path 
expression like so:

``` 
  - name: designator-namespace-id-from-namespace
   condition: '%resource.extension(%`namespaceExtName`).exists()'
   value: [ '%resource.extension(%`namespaceExtName`).value' ]
   hl7Spec: [ '%{hl7HDField}-1' ]
```

When a simple FHIR path expression is not enough, we use FHIR Functions like so:
``` 
  - name: specimen-received-time-diagnostic
    condition: '%resource.receivedTime.exists().not() and %resource.collection.collected is dateTime'
    value: [ '%resource.collection.collected.changeTimezone(%timezone)' ]
    hl7Spec: [ '%{hl7SpecimenFieldPath}-18' ]
```
FHIR Functions can run complicated 
queries on the bundle using the HAPI FHIR library directly in order to extract or manipulate the pieces of data we 
need to build the new message we are mapping to. In the example above, the `changeTimezone(%timezone)` was necessary 
because this customer wanted to receive the messages with times in their local format, regardless of what timezone was 
sent. The FHIR functions more geared towards extraction like `getPhoneNumberAreaCode()` normally are used when FHIR has
a field that requires data that is not sent over as a separate value. See [Translating with FHIR Functions](#translating-with-fhir-functions) for more details.    

Currently, there are two places where FHIR functions are created. Both files are called `CustomFhirPathFunctions` and
both extend an interface called FhirPathFunctions. There are two places for these functions because some
are internal functions, meaning that they can only be called from within the code rather than from the command line 
via the [FHIR Path Tool](#fhir-path-tool)

## Internal FHIR Path Functions
There is currently only one internal FHIR path function and that is the LIVD Lookup function which can be found [here](../../src/main/kotlin/fhirengine/engine/CustomFhirPathFunctions.kt)

- livdTableLookup - This function must be called on a resource of type `Observation` with a parameter specifying 
    the field in the `LivdLookupTable` that you want to search on. If the `deviceId` on the 
    Observation is set, it will do a lookup on the `deviceId` for the parameter specified. If the `deviceId` is not present
    and the `equipmentModelId` is present, it will search on the `equipmentModelId`. If both of those are not present, 
    but the `deviceName` is, it will search for the `equipmentModelName`. What is returned are any results that match 
    the criteria found in the LIVD table. A preview of this table can be found in the `LIVD-SARS-CoV-2.csv` file.

## External FHIR Path Functions
There are many external FHIR path functions which can be found [here](../../src/main/kotlin/fhirengine/translation/hl7/utils/CustomFHIRFunctions.kt)

An example of one of the currently most complicated functions is the convertDateToAge function:
- convertDateToAge(<optional> timeUnit, <optional> comparisonDate) - The resource must be a date. Since this function 
required a lot of code to complete, it is broken out into a separate helper function. This is the pattern we hope to 
use moving forward. This required so much extra code partially because it takes multiple, optional params which can be
in any order. One can specify whether this value is returned in years, months, or days. 
If left off, the method assumes years if it is greater than one year, months if it is less than a year but 
greater than one month, and days if the value is less than one month. There is also an optional param to pass a 
comparison date if you don't want to get the age based off of how old they are today.
(timeUnit, comparisonDate) and (comparisonDate, timeUnit) are both acceptable. It returns an age in years, months, or 
days. Example Output: 
>{  
>"value": "DecimalType[31]"  
>"unit": "year" &nbsp;  
>"code": "a"  
>}  
>Number of results = 1 ----------------------------

## How to define a FHIR path function
1. Determine which file it will go in by reading the [internal](#internal) and [external](#external) sections of this 
document.
2. Add the function itself. It must accept a parameter `focus: MutableList<Base>`. It has the option to accept 
`parameters: MutableList<MutableList<Base>>?` as a parameter as well. It must return `MutableList<Base>`.
3. Add it to the `resolveFunction` method. This registers it so the command line knows what to expect and is valid.
4. Add it to the `executeFunction` method. This registers it so the command line knows what to call.

## FHIR Path Tool
You can run these functions on a bundle using the prime command line fhirpath tool. To use the FHIR
path tool, in terminal, run `./prime fhirpath -i <bundle file path here>`.
This sets the bundle to be the resource. The resource is the part of the bundle which you can currently run functions
on.

- By default, the resource is the bundle itself. To change what the resource is set to, run a command like
  `resource=Bundle.entry.resource.ofType(Patient)[0]`.


- To evaluate a specific field in the bundle, run a command like
  `Bundle.entry.resource.ofType(Observation)[0].device.resolve().deviceName`.
    - In this example `resolve()` is used. If you encounter a message like
      `Reference to Encounter/1674584645770942000.cffbb057-46bb-4018-8fce-7a4d552ef7e3 -
      use resolve() to navigate into it`you need to evaluate the reference using resolve() and then you'll get the
      actual data inside


- To run a function on a resource, run a command like
  `Bundle.entry.resource.ofType(Patient)[0].contact.telecom.value.GetPhoneNumberCountryCode()`


- To call a function on a resource that is set, use `%resource` as shown here: `%resource.GetPhoneNumberCountryCode()`

- When using the tool, `--help` is your friend.

## Translating with FHIR Functions
Using these methods in mappings is basically the same as using them in the [FHIR Path Tool](#fhir-path-tool) just set 
the value to the resource you want to run the function on with the method called on it like so:
`value: [ '%resource.value.getPhoneNumberAreaCode()' ]`. 

That is one of the awesome things about using the FHIR path
tool, you can figure out the path that you need without having to update the mapping, stop the service, run the service, 
send a message, and then check that what you got is correct repeatedly. Ideally, after you find the path and test the 
function with the FHIR path tool, you will be able to get it right on the first go!
