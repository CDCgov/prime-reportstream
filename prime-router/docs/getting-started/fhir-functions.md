# FHIR Functions
FHIR functions are functions that can be run on a bundle (ex. retrieve an age or telephone area code) via the command line using the fhirpath tool. To use the FHIR 
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

Currently, there are two places where FHIR functions are created. Both are in a file called CustomFhirPathFunctions and
both extend an interface called FhirPathFunctions. There are two places for these functions because some
are internal functions, meaning that they can only be called from within the code rather than from the command line.

### Internal
There is currently only one internal FHIR path function and that is the LIVD Lookup function

- livdTableLookup - This function must be called on a resource of type `Observation` with a parameter specifying 
    the field in the `LivdLookupTable` that you want to search on. If the `deviceId` on the 
    Observation is set, it will do a lookup on the `deviceId` for the parameter specified. If the `deviceId` is not present
    and the `equipmentModelId` is present, it will search on the `equipmentModelId`. If both of those are not present, 
    but the `deviceName` is, it will search for the `equipmentModelName`. What is returned are any results that match 
    the criteria found in the LIVD table. A preview of this table can be found in the `LIVD-SARS-CoV-2.csv` file.

### External
There are many external FHIR path functions.
- getPhoneNumberCountryCode() - The resource must be a phone number, no parameters passed. Will return the country code 
associated with the phone number. Example output: `Primitive: IntegerType[1]`
- getPhoneNumberAreaCode() - The resource must be a phone number, no parameters passed. Will return the area code
  associated with the phone number. Example output: `Primitive: IntegerType[679]`
- getPhoneNumberLocalNumber() - The resource must be a phone number, no parameters passed. Will return the number 
without the area code. Example Output: `Primitive: IntegerType[1125593]`
- getPhoneNumberExtension() - The resource must be a phone number, no parameters passed. Will return the extension, if 
present.
- hasPhoneNumberExtension() - The resource must be a phone number, no parameters passed. Will return true or false 
depending on whether there is an extension. Example Output: `Primitive: BooleanType[false]`
- split(delimiter) - The resource must be a string, the parameter is the delimiter to split the string on. Will return a 
mutableList of the split strings. Example Output: 
> - Primitive: B 
> - Primitive: CL
> - Primitive:
> Number of results = 3 ---------------------------- 
- getId() - The resource must be a singular, primitive value, no parameters passed.
Returns a list with one ID value or an empty list. Example Output: 
> Primitive: BECLE  
>Number of results = 1 -------------------------`
- getIdType() - The resource must be a singular, primitive value, no parameters passed. 
Returns a list with one value denoting the ID type, or an empty list. Example Output: ``
> Primitive: OID  
>Number of results = 1 -------------------------`
- changeTimezone(timeZone) - The resource must be a DateTime, the timezone to change to is passed as a parameter. 
Returns a date in the new timezone.
>Primitive: DateTimeType[2022-06-22T05:06:00+09:30]  
>Number of results = 1 ----------------------------
- convertDateToAge(<optional> timeUnit, <optional> comparisonDate) - The resource must be a date. Since this function 
required a lot of code to complete, it is broken out into a separate helper function. This is the pattern we hope to 
use moving forward. This required so much extra code partially because it takes multiple, optional params which can be
in any order. One can specify whether this value is returned in years, months, or days. 
If left off, the method assumes years if it is greater than one year, months if it is less than a year but 
greater than one month, and days if the value is less than one month. There is also an optional param to pass a 
comparison date if you don't want to get the age based off of how old they are today.
(timeUnit, comparisonDate) and (comparisonDate, timeUnit) are both acceptable. It returns an age in years, months, or 
days.Example Output: 
>{  
>"value": "DecimalType[31]"  
>"unit": "year" &nbsp;  
>"code": "a"  
>}  
>Number of results = 1 ----------------------------
