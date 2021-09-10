import ReportResource from "../resources/ReportResource";

/* INFO
   This feature takes an array of ReportResources (pulled via our API) and uses
   JavaScript's Set type to return a collection of unique senders.
   
   NOTE
   Currently this is returning a Set<string> but it may be beneficial in the future to 
   return this list as an Array rather than a Set. In its only current implementation 
   in TableReports.tsx it's beeing converted using Array.from() */
export function getListOfSenders (reports: ReportResource[]): Set<string> {
    return new Set(reports.map(r => r.sendingOrg));
}