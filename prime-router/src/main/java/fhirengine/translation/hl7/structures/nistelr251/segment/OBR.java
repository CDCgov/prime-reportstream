/*
 * This class is an auto-generated source file for a HAPI
 * HL7 v2.x standard structure class.
 *
 * For more information, visit: http://hl7api.sourceforge.net/
 *
 * The contents of this file are subject to the Mozilla Public License Version 1.1
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.mozilla.org/MPL/
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for the
 * specific language governing rights and limitations under the License.
 *
 * The Original Code is "[file_name]".  Description:
 * "[one_line_description]"
 *
 * The Initial Developer of the Original Code is University Health Network. Copyright (C)
 * 2012.  All Rights Reserved.
 *
 * Contributor(s): ______________________________________.
 *
 * Alternatively, the contents of this file may be used under the terms of the
 * GNU General Public License (the  "GPL"), in which case the provisions of the GPL are
 * applicable instead of those above.  If you wish to allow use of your version of this
 * file only under the terms of the GPL and not to allow others to use your version
 * of this file under the MPL, indicate your decision by deleting  the provisions above
 * and replace  them with the notice and other provisions required by the GPL License.
 * If you do not delete the provisions above, a recipient may use your version of
 * this file under either the MPL or the GPL.
 *
 */


package fhirengine.translation.hl7.structures.nistelr251.segment;


import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.AbstractSegment;
import ca.uhn.hl7v2.model.Group;
import ca.uhn.hl7v2.model.Type;
import ca.uhn.hl7v2.model.v251.datatype.*;
import ca.uhn.hl7v2.parser.ModelClassFactory;
import fhirengine.translation.hl7.structures.nistelr251.datatype.CWE_ELR;
import fhirengine.translation.hl7.structures.nistelr251.datatype.PRL_ELR;


/**
 * <p>Represents an HL7 OBR message segment (Observation Request).
 * This segment has the following fields:</p>
 * <ul>
 * <li>OBR-1: Set ID - OBR (SI) <b> </b>
 * <li>OBR-2: Placer Order Number (EI) <b>optional </b>
 * <li>OBR-3: Filler Order Number (EI) <b> </b>
 * <li>OBR-4: Universal Service Identifier (CWE_ELR) <b> </b>
 * <li>OBR-5: Priority - OBR (ID) <b>optional repeating</b>
 * <li>OBR-6: Requested Date/Time (TS) <b>optional repeating</b>
 * <li>OBR-7: Observation Date/Time (TS) <b> </b>
 * <li>OBR-8: Observation End Date/Time (TS) <b>optional </b>
 * <li>OBR-9: Collection Volume (CQ) <b>optional repeating</b>
 * <li>OBR-10: Collector Identifier (XCN) <b>optional repeating</b>
 * <li>OBR-11: Specimen Action Code (ID) <b>optional </b>
 * <li>OBR-12: Danger Code (CWE) <b>optional </b>
 * <li>OBR-13: Relevant Clinical Information (ST) <b>optional </b>
 * <li>OBR-14: Specimen Received Date/Time (TS) <b>optional repeating</b>
 * <li>OBR-15: Specimen Source (SPS) <b>optional repeating</b>
 * <li>OBR-16: Ordering Provider (XCN) <b>optional repeating</b>
 * <li>OBR-17: Order Callback Phone Number (XTN) <b>optional repeating</b>
 * <li>OBR-18: Placer Field 1 (ST) <b>optional </b>
 * <li>OBR-19: Placer Field 2 (ST) <b>optional </b>
 * <li>OBR-20: Filler Field 1 (ST) <b>optional </b>
 * <li>OBR-21: Filler Field 2 (ST) <b>optional </b>
 * <li>OBR-22: Results Rpt/Status Chng - Date/Time (TS) <b> </b>
 * <li>OBR-23: Charge to Practice (MOC) <b>optional </b>
 * <li>OBR-24: Diagnostic Serv Sect ID (ID) <b>optional </b>
 * <li>OBR-25: Result Status (ID) <b> </b>
 * <li>OBR-26: Parent Result (PRL_ELR) <b>optional </b>
 * <li>OBR-27: Quantity/Timing (TQ) <b>optional repeating</b>
 * <li>OBR-28: Result Copies To (XCN) <b>optional repeating</b>
 * <li>OBR-29: Parent (EIP) <b>optional </b>
 * <li>OBR-30: Transportation Mode (ID) <b>optional repeating</b>
 * <li>OBR-31: Reason for Study (CWE_ELR) <b>optional repeating</b>
 * <li>OBR-32: Principal Result Interpreter (NDL) <b>optional </b>
 * <li>OBR-33: Assistant Result Interpreter (NDL) <b>optional repeating</b>
 * <li>OBR-34: Technician (NDL) <b>optional repeating</b>
 * <li>OBR-35: Transcriptionist (NDL) <b>optional repeating</b>
 * <li>OBR-36: Scheduled Date/Time (TS) <b>optional </b>
 * <li>OBR-37: Number of Sample Containers (NM) <b>optional repeating</b>
 * <li>OBR-38: Transport Logistics of Collected Sample (CWE) <b>optional repeating</b>
 * <li>OBR-39: Collector's Comment (CWE) <b>optional repeating</b>
 * <li>OBR-40: Transport Arrangement Responsibility (CWE) <b>optional repeating</b>
 * <li>OBR-41: Transport Arranged (ID) <b>optional repeating</b>
 * <li>OBR-42: Escort Required (ID) <b>optional repeating</b>
 * <li>OBR-43: Planned Patient Transport Comment (CWE) <b>optional repeating</b>
 * <li>OBR-44: Procedure Code (CWE) <b>optional </b>
 * <li>OBR-45: Procedure Code Modifier (CWE) <b>optional repeating</b>
 * <li>OBR-46: Placer Supplemental Service Information (CWE) <b>optional repeating</b>
 * <li>OBR-47: Filler Supplemental Service Information (CWE) <b>optional repeating</b>
 * <li>OBR-48: Medically Necessary Duplicate Procedure Reason. (CWE) <b>optional repeating</b>
 * <li>OBR-49: Result Handling (IS) <b>optional </b>
 * <li>OBR-50: Parent Universal Service Identifier (CWE) <b>optional </b>
 * </ul>
 */
@SuppressWarnings("unused")
public class OBR extends AbstractSegment {

  /**
   * Creates a new OBR segment
   */
  public OBR(Group parent, ModelClassFactory factory) {
    super(parent, factory);
    init(factory);
  }

  private void init(ModelClassFactory factory) {
    try {
      this.add(SI.class, true, 1, 0, new Object[]{getMessage()}, "Set ID - OBR");
      this.add(EI.class, false, 1, 0, new Object[]{getMessage()}, "Placer Order Number");
      this.add(EI.class, true, 1, 0, new Object[]{getMessage()}, "Filler Order Number");
      this.add(CWE_ELR.class, true, 1, 0, new Object[]{getMessage()}, "Universal Service Identifier");
      this.add(ID.class, false, 0, 0, new Object[]{getMessage(), Integer.valueOf(0)}, "Priority - OBR");
      this.add(TS.class, false, 0, 0, new Object[]{getMessage()}, "Requested Date/Time");
      this.add(TS.class, true, 1, 0, new Object[]{getMessage()}, "Observation Date/Time");
      this.add(TS.class, false, 1, 0, new Object[]{getMessage()}, "Observation End Date/Time");
      this.add(CQ.class, false, 0, 0, new Object[]{getMessage()}, "Collection Volume");
      this.add(XCN.class, false, -1, 0, new Object[]{getMessage()}, "Collector Identifier");
      this.add(ID.class, false, 1, 0, new Object[]{getMessage(), Integer.valueOf(65)}, "Specimen Action Code");
      this.add(CWE.class, false, 1, 0, new Object[]{getMessage()}, "Danger Code");
      this.add(ST.class, false, 1, 0, new Object[]{getMessage()}, "Relevant Clinical Information");
      this.add(TS.class, false, 0, 0, new Object[]{getMessage()}, "Specimen Received Date/Time");
      this.add(SPS.class, false, 0, 0, new Object[]{getMessage()}, "Specimen Source");
      this.add(XCN.class, false, -1, 0, new Object[]{getMessage()}, "Ordering Provider");
      this.add(XTN.class, false, 2, 0, new Object[]{getMessage()}, "Order Callback Phone Number");
      this.add(ST.class, false, 1, 0, new Object[]{getMessage()}, "Placer Field 1");
      this.add(ST.class, false, 1, 0, new Object[]{getMessage()}, "Placer Field 2");
      this.add(ST.class, false, 1, 0, new Object[]{getMessage()}, "Filler Field 1");
      this.add(ST.class, false, 1, 0, new Object[]{getMessage()}, "Filler Field 2");
      this.add(TS.class, true, 1, 0, new Object[]{getMessage()}, "Results Rpt/Status Chng - Date/Time");
      this.add(MOC.class, false, 1, 0, new Object[]{getMessage()}, "Charge to Practice");
      this.add(ID.class, false, 1, 0, new Object[]{getMessage(), Integer.valueOf(74)}, "Diagnostic Serv Sect ID");
      this.add(ID.class, true, 1, 0, new Object[]{getMessage(), Integer.valueOf(123)}, "Result Status");
      this.add(PRL_ELR.class, false, 1, 0, new Object[]{getMessage()}, "Parent Result");
      this.add(TQ.class, false, 0, 0, new Object[]{getMessage()}, "Quantity/Timing");
      this.add(XCN.class, false, -1, 0, new Object[]{getMessage()}, "Result Copies To");
      this.add(EIP.class, false, 1, 0, new Object[]{getMessage()}, "Parent");
      this.add(ID.class, false, 0, 0, new Object[]{getMessage(), Integer.valueOf(0)}, "Transportation Mode");
      this.add(CWE_ELR.class, false, -1, 0, new Object[]{getMessage()}, "Reason for Study");
      this.add(NDL.class, false, 1, 0, new Object[]{getMessage()}, "Principal Result Interpreter");
      this.add(NDL.class, false, -1, 0, new Object[]{getMessage()}, "Assistant Result Interpreter");
      this.add(NDL.class, false, -1, 0, new Object[]{getMessage()}, "Technician");
      this.add(NDL.class, false, -1, 0, new Object[]{getMessage()}, "Transcriptionist");
      this.add(TS.class, false, 1, 0, new Object[]{getMessage()}, "Scheduled Date/Time");
      this.add(NM.class, false, 0, 0, new Object[]{getMessage()}, "Number of Sample Containers");
      this.add(CWE.class, false, 0, 0, new Object[]{getMessage()}, "Transport Logistics of Collected Sample");
      this.add(CWE.class, false, -1, 0, new Object[]{getMessage()}, "Collector's Comment");
      this.add(CWE.class, false, 0, 0, new Object[]{getMessage()}, "Transport Arrangement Responsibility");
      this.add(ID.class, false, 0, 0, new Object[]{getMessage(), Integer.valueOf(0)}, "Transport Arranged");
      this.add(ID.class, false, 0, 0, new Object[]{getMessage(), Integer.valueOf(0)}, "Escort Required");
      this.add(CWE.class, false, 0, 0, new Object[]{getMessage()}, "Planned Patient Transport Comment");
      this.add(CWE.class, false, 1, 0, new Object[]{getMessage()}, "Procedure Code");
      this.add(CWE.class, false, -1, 0, new Object[]{getMessage()}, "Procedure Code Modifier");
      this.add(CWE.class, false, -1, 0, new Object[]{getMessage()}, "Placer Supplemental Service Information");
      this.add(CWE.class, false, -1, 0, new Object[]{getMessage()}, "Filler Supplemental Service Information");
      this.add(CWE.class, false, -1, 0, new Object[]{getMessage()}, "Medically Necessary Duplicate Procedure Reason.");
      this.add(IS.class, false, 1, 0, new Object[]{getMessage(), Integer.valueOf(507)}, "Result Handling");
      this.add(CWE.class, false, 1, 0, new Object[]{getMessage()}, "Parent Universal Service Identifier");
    } catch (HL7Exception e) {
      log.error("Unexpected error creating OBR - this is probably a bug in the source code generator.", e);
    }
  }


  /**
   * Returns
   * OBR-1: "Set ID - OBR" - creates it if necessary
   */
  public SI getSetIDOBR() {
    SI retVal = this.getTypedField(1, 0);
    return retVal;
  }

  /**
   * Returns
   * OBR-1: "Set ID - OBR" - creates it if necessary
   */
  public SI getObr1_SetIDOBR() {
    SI retVal = this.getTypedField(1, 0);
    return retVal;
  }


  /**
   * Returns
   * OBR-2: "Placer Order Number" - creates it if necessary
   */
  public EI getPlacerOrderNumber() {
    EI retVal = this.getTypedField(2, 0);
    return retVal;
  }

  /**
   * Returns
   * OBR-2: "Placer Order Number" - creates it if necessary
   */
  public EI getObr2_PlacerOrderNumber() {
    EI retVal = this.getTypedField(2, 0);
    return retVal;
  }


  /**
   * Returns
   * OBR-3: "Filler Order Number" - creates it if necessary
   */
  public EI getFillerOrderNumber() {
    EI retVal = this.getTypedField(3, 0);
    return retVal;
  }

  /**
   * Returns
   * OBR-3: "Filler Order Number" - creates it if necessary
   */
  public EI getObr3_FillerOrderNumber() {
    EI retVal = this.getTypedField(3, 0);
    return retVal;
  }


  /**
   * Returns
   * OBR-4: "Universal Service Identifier" - creates it if necessary
   */
  public CWE_ELR getUniversalServiceIdentifier() {
    CWE_ELR retVal = this.getTypedField(4, 0);
    return retVal;
  }

  /**
   * Returns
   * OBR-4: "Universal Service Identifier" - creates it if necessary
   */
  public CWE_ELR getObr4_UniversalServiceIdentifier() {
    CWE_ELR retVal = this.getTypedField(4, 0);
    return retVal;
  }


  /**
   * Returns all repetitions of Priority - OBR (OBR-5).
   */
  public ID[] getPriorityOBR() {
    ID[] retVal = this.getTypedField(5, new ID[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Priority - OBR (OBR-5).
   */
  public ID[] getObr5_PriorityOBR() {
    ID[] retVal = this.getTypedField(5, new ID[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Priority - OBR (OBR-5).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPriorityOBRReps() {
    return this.getReps(5);
  }


  /**
   * Returns a specific repetition of
   * OBR-5: "Priority - OBR" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public ID getPriorityOBR(int rep) {
    ID retVal = this.getTypedField(5, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * OBR-5: "Priority - OBR" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public ID getObr5_PriorityOBR(int rep) {
    ID retVal = this.getTypedField(5, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Priority - OBR (OBR-5).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getObr5_PriorityOBRReps() {
    return this.getReps(5);
  }


  /**
   * Inserts a repetition of
   * OBR-5: "Priority - OBR" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID insertPriorityOBR(int rep) throws HL7Exception {
    return (ID) super.insertRepetition(5, rep);
  }


  /**
   * Inserts a repetition of
   * OBR-5: "Priority - OBR" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID insertObr5_PriorityOBR(int rep) throws HL7Exception {
    return (ID) super.insertRepetition(5, rep);
  }


  /**
   * Removes a repetition of
   * OBR-5: "Priority - OBR" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID removePriorityOBR(int rep) throws HL7Exception {
    return (ID) super.removeRepetition(5, rep);
  }


  /**
   * Removes a repetition of
   * OBR-5: "Priority - OBR" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID removeObr5_PriorityOBR(int rep) throws HL7Exception {
    return (ID) super.removeRepetition(5, rep);
  }


  /**
   * Returns all repetitions of Requested Date/Time (OBR-6).
   */
  public TS[] getRequestedDateTime() {
    TS[] retVal = this.getTypedField(6, new TS[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Requested Date/Time (OBR-6).
   */
  public TS[] getObr6_RequestedDateTime() {
    TS[] retVal = this.getTypedField(6, new TS[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Requested Date/Time (OBR-6).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getRequestedDateTimeReps() {
    return this.getReps(6);
  }


  /**
   * Returns a specific repetition of
   * OBR-6: "Requested Date/Time" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public TS getRequestedDateTime(int rep) {
    TS retVal = this.getTypedField(6, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * OBR-6: "Requested Date/Time" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public TS getObr6_RequestedDateTime(int rep) {
    TS retVal = this.getTypedField(6, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Requested Date/Time (OBR-6).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getObr6_RequestedDateTimeReps() {
    return this.getReps(6);
  }


  /**
   * Inserts a repetition of
   * OBR-6: "Requested Date/Time" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public TS insertRequestedDateTime(int rep) throws HL7Exception {
    return (TS) super.insertRepetition(6, rep);
  }


  /**
   * Inserts a repetition of
   * OBR-6: "Requested Date/Time" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public TS insertObr6_RequestedDateTime(int rep) throws HL7Exception {
    return (TS) super.insertRepetition(6, rep);
  }


  /**
   * Removes a repetition of
   * OBR-6: "Requested Date/Time" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public TS removeRequestedDateTime(int rep) throws HL7Exception {
    return (TS) super.removeRepetition(6, rep);
  }


  /**
   * Removes a repetition of
   * OBR-6: "Requested Date/Time" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public TS removeObr6_RequestedDateTime(int rep) throws HL7Exception {
    return (TS) super.removeRepetition(6, rep);
  }


  /**
   * Returns
   * OBR-7: "Observation Date/Time" - creates it if necessary
   */
  public TS getObservationDateTime() {
    TS retVal = this.getTypedField(7, 0);
    return retVal;
  }

  /**
   * Returns
   * OBR-7: "Observation Date/Time" - creates it if necessary
   */
  public TS getObr7_ObservationDateTime() {
    TS retVal = this.getTypedField(7, 0);
    return retVal;
  }


  /**
   * Returns
   * OBR-8: "Observation End Date/Time" - creates it if necessary
   */
  public TS getObservationEndDateTime() {
    TS retVal = this.getTypedField(8, 0);
    return retVal;
  }

  /**
   * Returns
   * OBR-8: "Observation End Date/Time" - creates it if necessary
   */
  public TS getObr8_ObservationEndDateTime() {
    TS retVal = this.getTypedField(8, 0);
    return retVal;
  }


  /**
   * Returns all repetitions of Collection Volume (OBR-9).
   */
  public CQ[] getCollectionVolume() {
    CQ[] retVal = this.getTypedField(9, new CQ[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Collection Volume (OBR-9).
   */
  public CQ[] getObr9_CollectionVolume() {
    CQ[] retVal = this.getTypedField(9, new CQ[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Collection Volume (OBR-9).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getCollectionVolumeReps() {
    return this.getReps(9);
  }


  /**
   * Returns a specific repetition of
   * OBR-9: "Collection Volume" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CQ getCollectionVolume(int rep) {
    CQ retVal = this.getTypedField(9, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * OBR-9: "Collection Volume" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CQ getObr9_CollectionVolume(int rep) {
    CQ retVal = this.getTypedField(9, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Collection Volume (OBR-9).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getObr9_CollectionVolumeReps() {
    return this.getReps(9);
  }


  /**
   * Inserts a repetition of
   * OBR-9: "Collection Volume" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CQ insertCollectionVolume(int rep) throws HL7Exception {
    return (CQ) super.insertRepetition(9, rep);
  }


  /**
   * Inserts a repetition of
   * OBR-9: "Collection Volume" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CQ insertObr9_CollectionVolume(int rep) throws HL7Exception {
    return (CQ) super.insertRepetition(9, rep);
  }


  /**
   * Removes a repetition of
   * OBR-9: "Collection Volume" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CQ removeCollectionVolume(int rep) throws HL7Exception {
    return (CQ) super.removeRepetition(9, rep);
  }


  /**
   * Removes a repetition of
   * OBR-9: "Collection Volume" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CQ removeObr9_CollectionVolume(int rep) throws HL7Exception {
    return (CQ) super.removeRepetition(9, rep);
  }


  /**
   * Returns all repetitions of Collector Identifier (OBR-10).
   */
  public XCN[] getCollectorIdentifier() {
    XCN[] retVal = this.getTypedField(10, new XCN[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Collector Identifier (OBR-10).
   */
  public XCN[] getObr10_CollectorIdentifier() {
    XCN[] retVal = this.getTypedField(10, new XCN[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Collector Identifier (OBR-10).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getCollectorIdentifierReps() {
    return this.getReps(10);
  }


  /**
   * Returns a specific repetition of
   * OBR-10: "Collector Identifier" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XCN getCollectorIdentifier(int rep) {
    XCN retVal = this.getTypedField(10, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * OBR-10: "Collector Identifier" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XCN getObr10_CollectorIdentifier(int rep) {
    XCN retVal = this.getTypedField(10, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Collector Identifier (OBR-10).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getObr10_CollectorIdentifierReps() {
    return this.getReps(10);
  }


  /**
   * Inserts a repetition of
   * OBR-10: "Collector Identifier" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN insertCollectorIdentifier(int rep) throws HL7Exception {
    return (XCN) super.insertRepetition(10, rep);
  }


  /**
   * Inserts a repetition of
   * OBR-10: "Collector Identifier" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN insertObr10_CollectorIdentifier(int rep) throws HL7Exception {
    return (XCN) super.insertRepetition(10, rep);
  }


  /**
   * Removes a repetition of
   * OBR-10: "Collector Identifier" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN removeCollectorIdentifier(int rep) throws HL7Exception {
    return (XCN) super.removeRepetition(10, rep);
  }


  /**
   * Removes a repetition of
   * OBR-10: "Collector Identifier" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN removeObr10_CollectorIdentifier(int rep) throws HL7Exception {
    return (XCN) super.removeRepetition(10, rep);
  }


  /**
   * Returns
   * OBR-11: "Specimen Action Code" - creates it if necessary
   */
  public ID getSpecimenActionCode() {
    ID retVal = this.getTypedField(11, 0);
    return retVal;
  }

  /**
   * Returns
   * OBR-11: "Specimen Action Code" - creates it if necessary
   */
  public ID getObr11_SpecimenActionCode() {
    ID retVal = this.getTypedField(11, 0);
    return retVal;
  }


  /**
   * Returns
   * OBR-12: "Danger Code" - creates it if necessary
   */
  public CWE getDangerCode() {
    CWE retVal = this.getTypedField(12, 0);
    return retVal;
  }

  /**
   * Returns
   * OBR-12: "Danger Code" - creates it if necessary
   */
  public CWE getObr12_DangerCode() {
    CWE retVal = this.getTypedField(12, 0);
    return retVal;
  }


  /**
   * Returns
   * OBR-13: "Relevant Clinical Information" - creates it if necessary
   */
  public ST getRelevantClinicalInformation() {
    ST retVal = this.getTypedField(13, 0);
    return retVal;
  }

  /**
   * Returns
   * OBR-13: "Relevant Clinical Information" - creates it if necessary
   */
  public ST getObr13_RelevantClinicalInformation() {
    ST retVal = this.getTypedField(13, 0);
    return retVal;
  }


  /**
   * Returns all repetitions of Specimen Received Date/Time (OBR-14).
   */
  public TS[] getSpecimenReceivedDateTime() {
    TS[] retVal = this.getTypedField(14, new TS[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Specimen Received Date/Time (OBR-14).
   */
  public TS[] getObr14_SpecimenReceivedDateTime() {
    TS[] retVal = this.getTypedField(14, new TS[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Specimen Received Date/Time (OBR-14).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getSpecimenReceivedDateTimeReps() {
    return this.getReps(14);
  }


  /**
   * Returns a specific repetition of
   * OBR-14: "Specimen Received Date/Time" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public TS getSpecimenReceivedDateTime(int rep) {
    TS retVal = this.getTypedField(14, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * OBR-14: "Specimen Received Date/Time" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public TS getObr14_SpecimenReceivedDateTime(int rep) {
    TS retVal = this.getTypedField(14, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Specimen Received Date/Time (OBR-14).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getObr14_SpecimenReceivedDateTimeReps() {
    return this.getReps(14);
  }


  /**
   * Inserts a repetition of
   * OBR-14: "Specimen Received Date/Time" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public TS insertSpecimenReceivedDateTime(int rep) throws HL7Exception {
    return (TS) super.insertRepetition(14, rep);
  }


  /**
   * Inserts a repetition of
   * OBR-14: "Specimen Received Date/Time" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public TS insertObr14_SpecimenReceivedDateTime(int rep) throws HL7Exception {
    return (TS) super.insertRepetition(14, rep);
  }


  /**
   * Removes a repetition of
   * OBR-14: "Specimen Received Date/Time" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public TS removeSpecimenReceivedDateTime(int rep) throws HL7Exception {
    return (TS) super.removeRepetition(14, rep);
  }


  /**
   * Removes a repetition of
   * OBR-14: "Specimen Received Date/Time" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public TS removeObr14_SpecimenReceivedDateTime(int rep) throws HL7Exception {
    return (TS) super.removeRepetition(14, rep);
  }


  /**
   * Returns all repetitions of Specimen Source (OBR-15).
   */
  public SPS[] getSpecimenSource() {
    SPS[] retVal = this.getTypedField(15, new SPS[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Specimen Source (OBR-15).
   */
  public SPS[] getObr15_SpecimenSource() {
    SPS[] retVal = this.getTypedField(15, new SPS[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Specimen Source (OBR-15).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getSpecimenSourceReps() {
    return this.getReps(15);
  }


  /**
   * Returns a specific repetition of
   * OBR-15: "Specimen Source" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public SPS getSpecimenSource(int rep) {
    SPS retVal = this.getTypedField(15, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * OBR-15: "Specimen Source" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public SPS getObr15_SpecimenSource(int rep) {
    SPS retVal = this.getTypedField(15, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Specimen Source (OBR-15).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getObr15_SpecimenSourceReps() {
    return this.getReps(15);
  }


  /**
   * Inserts a repetition of
   * OBR-15: "Specimen Source" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public SPS insertSpecimenSource(int rep) throws HL7Exception {
    return (SPS) super.insertRepetition(15, rep);
  }


  /**
   * Inserts a repetition of
   * OBR-15: "Specimen Source" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public SPS insertObr15_SpecimenSource(int rep) throws HL7Exception {
    return (SPS) super.insertRepetition(15, rep);
  }


  /**
   * Removes a repetition of
   * OBR-15: "Specimen Source" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public SPS removeSpecimenSource(int rep) throws HL7Exception {
    return (SPS) super.removeRepetition(15, rep);
  }


  /**
   * Removes a repetition of
   * OBR-15: "Specimen Source" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public SPS removeObr15_SpecimenSource(int rep) throws HL7Exception {
    return (SPS) super.removeRepetition(15, rep);
  }


  /**
   * Returns all repetitions of Ordering Provider (OBR-16).
   */
  public XCN[] getOrderingProvider() {
    XCN[] retVal = this.getTypedField(16, new XCN[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Ordering Provider (OBR-16).
   */
  public XCN[] getObr16_OrderingProvider() {
    XCN[] retVal = this.getTypedField(16, new XCN[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Ordering Provider (OBR-16).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getOrderingProviderReps() {
    return this.getReps(16);
  }


  /**
   * Returns a specific repetition of
   * OBR-16: "Ordering Provider" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XCN getOrderingProvider(int rep) {
    XCN retVal = this.getTypedField(16, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * OBR-16: "Ordering Provider" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XCN getObr16_OrderingProvider(int rep) {
    XCN retVal = this.getTypedField(16, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Ordering Provider (OBR-16).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getObr16_OrderingProviderReps() {
    return this.getReps(16);
  }


  /**
   * Inserts a repetition of
   * OBR-16: "Ordering Provider" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN insertOrderingProvider(int rep) throws HL7Exception {
    return (XCN) super.insertRepetition(16, rep);
  }


  /**
   * Inserts a repetition of
   * OBR-16: "Ordering Provider" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN insertObr16_OrderingProvider(int rep) throws HL7Exception {
    return (XCN) super.insertRepetition(16, rep);
  }


  /**
   * Removes a repetition of
   * OBR-16: "Ordering Provider" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN removeOrderingProvider(int rep) throws HL7Exception {
    return (XCN) super.removeRepetition(16, rep);
  }


  /**
   * Removes a repetition of
   * OBR-16: "Ordering Provider" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN removeObr16_OrderingProvider(int rep) throws HL7Exception {
    return (XCN) super.removeRepetition(16, rep);
  }


  /**
   * Returns all repetitions of Order Callback Phone Number (OBR-17).
   */
  public XTN[] getOrderCallbackPhoneNumber() {
    XTN[] retVal = this.getTypedField(17, new XTN[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Order Callback Phone Number (OBR-17).
   */
  public XTN[] getObr17_OrderCallbackPhoneNumber() {
    XTN[] retVal = this.getTypedField(17, new XTN[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Order Callback Phone Number (OBR-17).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getOrderCallbackPhoneNumberReps() {
    return this.getReps(17);
  }


  /**
   * Returns a specific repetition of
   * OBR-17: "Order Callback Phone Number" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XTN getOrderCallbackPhoneNumber(int rep) {
    XTN retVal = this.getTypedField(17, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * OBR-17: "Order Callback Phone Number" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XTN getObr17_OrderCallbackPhoneNumber(int rep) {
    XTN retVal = this.getTypedField(17, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Order Callback Phone Number (OBR-17).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getObr17_OrderCallbackPhoneNumberReps() {
    return this.getReps(17);
  }


  /**
   * Inserts a repetition of
   * OBR-17: "Order Callback Phone Number" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XTN insertOrderCallbackPhoneNumber(int rep) throws HL7Exception {
    return (XTN) super.insertRepetition(17, rep);
  }


  /**
   * Inserts a repetition of
   * OBR-17: "Order Callback Phone Number" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XTN insertObr17_OrderCallbackPhoneNumber(int rep) throws HL7Exception {
    return (XTN) super.insertRepetition(17, rep);
  }


  /**
   * Removes a repetition of
   * OBR-17: "Order Callback Phone Number" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XTN removeOrderCallbackPhoneNumber(int rep) throws HL7Exception {
    return (XTN) super.removeRepetition(17, rep);
  }


  /**
   * Removes a repetition of
   * OBR-17: "Order Callback Phone Number" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XTN removeObr17_OrderCallbackPhoneNumber(int rep) throws HL7Exception {
    return (XTN) super.removeRepetition(17, rep);
  }


  /**
   * Returns
   * OBR-18: "Placer Field 1" - creates it if necessary
   */
  public ST getPlacerField1() {
    ST retVal = this.getTypedField(18, 0);
    return retVal;
  }

  /**
   * Returns
   * OBR-18: "Placer Field 1" - creates it if necessary
   */
  public ST getObr18_PlacerField1() {
    ST retVal = this.getTypedField(18, 0);
    return retVal;
  }


  /**
   * Returns
   * OBR-19: "Placer Field 2" - creates it if necessary
   */
  public ST getPlacerField2() {
    ST retVal = this.getTypedField(19, 0);
    return retVal;
  }

  /**
   * Returns
   * OBR-19: "Placer Field 2" - creates it if necessary
   */
  public ST getObr19_PlacerField2() {
    ST retVal = this.getTypedField(19, 0);
    return retVal;
  }


  /**
   * Returns
   * OBR-20: "Filler Field 1" - creates it if necessary
   */
  public ST getFillerField1() {
    ST retVal = this.getTypedField(20, 0);
    return retVal;
  }

  /**
   * Returns
   * OBR-20: "Filler Field 1" - creates it if necessary
   */
  public ST getObr20_FillerField1() {
    ST retVal = this.getTypedField(20, 0);
    return retVal;
  }


  /**
   * Returns
   * OBR-21: "Filler Field 2" - creates it if necessary
   */
  public ST getFillerField2() {
    ST retVal = this.getTypedField(21, 0);
    return retVal;
  }

  /**
   * Returns
   * OBR-21: "Filler Field 2" - creates it if necessary
   */
  public ST getObr21_FillerField2() {
    ST retVal = this.getTypedField(21, 0);
    return retVal;
  }


  /**
   * Returns
   * OBR-22: "Results Rpt/Status Chng - Date/Time" - creates it if necessary
   */
  public TS getResultsRptStatusChngDateTime() {
    TS retVal = this.getTypedField(22, 0);
    return retVal;
  }

  /**
   * Returns
   * OBR-22: "Results Rpt/Status Chng - Date/Time" - creates it if necessary
   */
  public TS getObr22_ResultsRptStatusChngDateTime() {
    TS retVal = this.getTypedField(22, 0);
    return retVal;
  }


  /**
   * Returns
   * OBR-23: "Charge to Practice" - creates it if necessary
   */
  public MOC getChargeToPractice() {
    MOC retVal = this.getTypedField(23, 0);
    return retVal;
  }

  /**
   * Returns
   * OBR-23: "Charge to Practice" - creates it if necessary
   */
  public MOC getObr23_ChargeToPractice() {
    MOC retVal = this.getTypedField(23, 0);
    return retVal;
  }


  /**
   * Returns
   * OBR-24: "Diagnostic Serv Sect ID" - creates it if necessary
   */
  public ID getDiagnosticServSectID() {
    ID retVal = this.getTypedField(24, 0);
    return retVal;
  }

  /**
   * Returns
   * OBR-24: "Diagnostic Serv Sect ID" - creates it if necessary
   */
  public ID getObr24_DiagnosticServSectID() {
    ID retVal = this.getTypedField(24, 0);
    return retVal;
  }


  /**
   * Returns
   * OBR-25: "Result Status" - creates it if necessary
   */
  public ID getResultStatus() {
    ID retVal = this.getTypedField(25, 0);
    return retVal;
  }

  /**
   * Returns
   * OBR-25: "Result Status" - creates it if necessary
   */
  public ID getObr25_ResultStatus() {
    ID retVal = this.getTypedField(25, 0);
    return retVal;
  }


  /**
   * Returns
   * OBR-26: "Parent Result" - creates it if necessary
   */
  public PRL_ELR getParentResult() {
    PRL_ELR retVal = this.getTypedField(26, 0);
    return retVal;
  }

  /**
   * Returns
   * OBR-26: "Parent Result" - creates it if necessary
   */
  public PRL_ELR getObr26_ParentResult() {
    PRL_ELR retVal = this.getTypedField(26, 0);
    return retVal;
  }


  /**
   * Returns all repetitions of Quantity/Timing (OBR-27).
   */
  public TQ[] getQuantityTiming() {
    TQ[] retVal = this.getTypedField(27, new TQ[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Quantity/Timing (OBR-27).
   */
  public TQ[] getObr27_QuantityTiming() {
    TQ[] retVal = this.getTypedField(27, new TQ[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Quantity/Timing (OBR-27).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getQuantityTimingReps() {
    return this.getReps(27);
  }


  /**
   * Returns a specific repetition of
   * OBR-27: "Quantity/Timing" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public TQ getQuantityTiming(int rep) {
    TQ retVal = this.getTypedField(27, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * OBR-27: "Quantity/Timing" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public TQ getObr27_QuantityTiming(int rep) {
    TQ retVal = this.getTypedField(27, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Quantity/Timing (OBR-27).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getObr27_QuantityTimingReps() {
    return this.getReps(27);
  }


  /**
   * Inserts a repetition of
   * OBR-27: "Quantity/Timing" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public TQ insertQuantityTiming(int rep) throws HL7Exception {
    return (TQ) super.insertRepetition(27, rep);
  }


  /**
   * Inserts a repetition of
   * OBR-27: "Quantity/Timing" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public TQ insertObr27_QuantityTiming(int rep) throws HL7Exception {
    return (TQ) super.insertRepetition(27, rep);
  }


  /**
   * Removes a repetition of
   * OBR-27: "Quantity/Timing" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public TQ removeQuantityTiming(int rep) throws HL7Exception {
    return (TQ) super.removeRepetition(27, rep);
  }


  /**
   * Removes a repetition of
   * OBR-27: "Quantity/Timing" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public TQ removeObr27_QuantityTiming(int rep) throws HL7Exception {
    return (TQ) super.removeRepetition(27, rep);
  }


  /**
   * Returns all repetitions of Result Copies To (OBR-28).
   */
  public XCN[] getResultCopiesTo() {
    XCN[] retVal = this.getTypedField(28, new XCN[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Result Copies To (OBR-28).
   */
  public XCN[] getObr28_ResultCopiesTo() {
    XCN[] retVal = this.getTypedField(28, new XCN[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Result Copies To (OBR-28).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getResultCopiesToReps() {
    return this.getReps(28);
  }


  /**
   * Returns a specific repetition of
   * OBR-28: "Result Copies To" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XCN getResultCopiesTo(int rep) {
    XCN retVal = this.getTypedField(28, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * OBR-28: "Result Copies To" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XCN getObr28_ResultCopiesTo(int rep) {
    XCN retVal = this.getTypedField(28, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Result Copies To (OBR-28).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getObr28_ResultCopiesToReps() {
    return this.getReps(28);
  }


  /**
   * Inserts a repetition of
   * OBR-28: "Result Copies To" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN insertResultCopiesTo(int rep) throws HL7Exception {
    return (XCN) super.insertRepetition(28, rep);
  }


  /**
   * Inserts a repetition of
   * OBR-28: "Result Copies To" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN insertObr28_ResultCopiesTo(int rep) throws HL7Exception {
    return (XCN) super.insertRepetition(28, rep);
  }


  /**
   * Removes a repetition of
   * OBR-28: "Result Copies To" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN removeResultCopiesTo(int rep) throws HL7Exception {
    return (XCN) super.removeRepetition(28, rep);
  }


  /**
   * Removes a repetition of
   * OBR-28: "Result Copies To" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN removeObr28_ResultCopiesTo(int rep) throws HL7Exception {
    return (XCN) super.removeRepetition(28, rep);
  }


  /**
   * Returns
   * OBR-29: "Parent" - creates it if necessary
   */
  public EIP getOBRParent() {
    EIP retVal = this.getTypedField(29, 0);
    return retVal;
  }

  /**
   * Returns
   * OBR-29: "Parent" - creates it if necessary
   */
  public EIP getObr29_Parent() {
    EIP retVal = this.getTypedField(29, 0);
    return retVal;
  }


  /**
   * Returns all repetitions of Transportation Mode (OBR-30).
   */
  public ID[] getTransportationMode() {
    ID[] retVal = this.getTypedField(30, new ID[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Transportation Mode (OBR-30).
   */
  public ID[] getObr30_TransportationMode() {
    ID[] retVal = this.getTypedField(30, new ID[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Transportation Mode (OBR-30).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getTransportationModeReps() {
    return this.getReps(30);
  }


  /**
   * Returns a specific repetition of
   * OBR-30: "Transportation Mode" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public ID getTransportationMode(int rep) {
    ID retVal = this.getTypedField(30, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * OBR-30: "Transportation Mode" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public ID getObr30_TransportationMode(int rep) {
    ID retVal = this.getTypedField(30, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Transportation Mode (OBR-30).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getObr30_TransportationModeReps() {
    return this.getReps(30);
  }


  /**
   * Inserts a repetition of
   * OBR-30: "Transportation Mode" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID insertTransportationMode(int rep) throws HL7Exception {
    return (ID) super.insertRepetition(30, rep);
  }


  /**
   * Inserts a repetition of
   * OBR-30: "Transportation Mode" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID insertObr30_TransportationMode(int rep) throws HL7Exception {
    return (ID) super.insertRepetition(30, rep);
  }


  /**
   * Removes a repetition of
   * OBR-30: "Transportation Mode" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID removeTransportationMode(int rep) throws HL7Exception {
    return (ID) super.removeRepetition(30, rep);
  }


  /**
   * Removes a repetition of
   * OBR-30: "Transportation Mode" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID removeObr30_TransportationMode(int rep) throws HL7Exception {
    return (ID) super.removeRepetition(30, rep);
  }


  /**
   * Returns all repetitions of Reason for Study (OBR-31).
   */
  public CWE_ELR[] getReasonForStudy() {
    CWE_ELR[] retVal = this.getTypedField(31, new CWE_ELR[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Reason for Study (OBR-31).
   */
  public CWE_ELR[] getObr31_ReasonForStudy() {
    CWE_ELR[] retVal = this.getTypedField(31, new CWE_ELR[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Reason for Study (OBR-31).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getReasonForStudyReps() {
    return this.getReps(31);
  }


  /**
   * Returns a specific repetition of
   * OBR-31: "Reason for Study" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE_ELR getReasonForStudy(int rep) {
    CWE_ELR retVal = this.getTypedField(31, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * OBR-31: "Reason for Study" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE_ELR getObr31_ReasonForStudy(int rep) {
    CWE_ELR retVal = this.getTypedField(31, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Reason for Study (OBR-31).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getObr31_ReasonForStudyReps() {
    return this.getReps(31);
  }


  /**
   * Inserts a repetition of
   * OBR-31: "Reason for Study" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE_ELR insertReasonForStudy(int rep) throws HL7Exception {
    return (CWE_ELR) super.insertRepetition(31, rep);
  }


  /**
   * Inserts a repetition of
   * OBR-31: "Reason for Study" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE_ELR insertObr31_ReasonForStudy(int rep) throws HL7Exception {
    return (CWE_ELR) super.insertRepetition(31, rep);
  }


  /**
   * Removes a repetition of
   * OBR-31: "Reason for Study" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE_ELR removeReasonForStudy(int rep) throws HL7Exception {
    return (CWE_ELR) super.removeRepetition(31, rep);
  }


  /**
   * Removes a repetition of
   * OBR-31: "Reason for Study" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE_ELR removeObr31_ReasonForStudy(int rep) throws HL7Exception {
    return (CWE_ELR) super.removeRepetition(31, rep);
  }


  /**
   * Returns
   * OBR-32: "Principal Result Interpreter" - creates it if necessary
   */
  public NDL getPrincipalResultInterpreter() {
    NDL retVal = this.getTypedField(32, 0);
    return retVal;
  }

  /**
   * Returns
   * OBR-32: "Principal Result Interpreter" - creates it if necessary
   */
  public NDL getObr32_PrincipalResultInterpreter() {
    NDL retVal = this.getTypedField(32, 0);
    return retVal;
  }


  /**
   * Returns all repetitions of Assistant Result Interpreter (OBR-33).
   */
  public NDL[] getAssistantResultInterpreter() {
    NDL[] retVal = this.getTypedField(33, new NDL[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Assistant Result Interpreter (OBR-33).
   */
  public NDL[] getObr33_AssistantResultInterpreter() {
    NDL[] retVal = this.getTypedField(33, new NDL[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Assistant Result Interpreter (OBR-33).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getAssistantResultInterpreterReps() {
    return this.getReps(33);
  }


  /**
   * Returns a specific repetition of
   * OBR-33: "Assistant Result Interpreter" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public NDL getAssistantResultInterpreter(int rep) {
    NDL retVal = this.getTypedField(33, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * OBR-33: "Assistant Result Interpreter" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public NDL getObr33_AssistantResultInterpreter(int rep) {
    NDL retVal = this.getTypedField(33, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Assistant Result Interpreter (OBR-33).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getObr33_AssistantResultInterpreterReps() {
    return this.getReps(33);
  }


  /**
   * Inserts a repetition of
   * OBR-33: "Assistant Result Interpreter" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public NDL insertAssistantResultInterpreter(int rep) throws HL7Exception {
    return (NDL) super.insertRepetition(33, rep);
  }


  /**
   * Inserts a repetition of
   * OBR-33: "Assistant Result Interpreter" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public NDL insertObr33_AssistantResultInterpreter(int rep) throws HL7Exception {
    return (NDL) super.insertRepetition(33, rep);
  }


  /**
   * Removes a repetition of
   * OBR-33: "Assistant Result Interpreter" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public NDL removeAssistantResultInterpreter(int rep) throws HL7Exception {
    return (NDL) super.removeRepetition(33, rep);
  }


  /**
   * Removes a repetition of
   * OBR-33: "Assistant Result Interpreter" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public NDL removeObr33_AssistantResultInterpreter(int rep) throws HL7Exception {
    return (NDL) super.removeRepetition(33, rep);
  }


  /**
   * Returns all repetitions of Technician (OBR-34).
   */
  public NDL[] getTechnician() {
    NDL[] retVal = this.getTypedField(34, new NDL[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Technician (OBR-34).
   */
  public NDL[] getObr34_Technician() {
    NDL[] retVal = this.getTypedField(34, new NDL[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Technician (OBR-34).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getTechnicianReps() {
    return this.getReps(34);
  }


  /**
   * Returns a specific repetition of
   * OBR-34: "Technician" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public NDL getTechnician(int rep) {
    NDL retVal = this.getTypedField(34, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * OBR-34: "Technician" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public NDL getObr34_Technician(int rep) {
    NDL retVal = this.getTypedField(34, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Technician (OBR-34).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getObr34_TechnicianReps() {
    return this.getReps(34);
  }


  /**
   * Inserts a repetition of
   * OBR-34: "Technician" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public NDL insertTechnician(int rep) throws HL7Exception {
    return (NDL) super.insertRepetition(34, rep);
  }


  /**
   * Inserts a repetition of
   * OBR-34: "Technician" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public NDL insertObr34_Technician(int rep) throws HL7Exception {
    return (NDL) super.insertRepetition(34, rep);
  }


  /**
   * Removes a repetition of
   * OBR-34: "Technician" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public NDL removeTechnician(int rep) throws HL7Exception {
    return (NDL) super.removeRepetition(34, rep);
  }


  /**
   * Removes a repetition of
   * OBR-34: "Technician" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public NDL removeObr34_Technician(int rep) throws HL7Exception {
    return (NDL) super.removeRepetition(34, rep);
  }


  /**
   * Returns all repetitions of Transcriptionist (OBR-35).
   */
  public NDL[] getTranscriptionist() {
    NDL[] retVal = this.getTypedField(35, new NDL[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Transcriptionist (OBR-35).
   */
  public NDL[] getObr35_Transcriptionist() {
    NDL[] retVal = this.getTypedField(35, new NDL[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Transcriptionist (OBR-35).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getTranscriptionistReps() {
    return this.getReps(35);
  }


  /**
   * Returns a specific repetition of
   * OBR-35: "Transcriptionist" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public NDL getTranscriptionist(int rep) {
    NDL retVal = this.getTypedField(35, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * OBR-35: "Transcriptionist" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public NDL getObr35_Transcriptionist(int rep) {
    NDL retVal = this.getTypedField(35, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Transcriptionist (OBR-35).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getObr35_TranscriptionistReps() {
    return this.getReps(35);
  }


  /**
   * Inserts a repetition of
   * OBR-35: "Transcriptionist" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public NDL insertTranscriptionist(int rep) throws HL7Exception {
    return (NDL) super.insertRepetition(35, rep);
  }


  /**
   * Inserts a repetition of
   * OBR-35: "Transcriptionist" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public NDL insertObr35_Transcriptionist(int rep) throws HL7Exception {
    return (NDL) super.insertRepetition(35, rep);
  }


  /**
   * Removes a repetition of
   * OBR-35: "Transcriptionist" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public NDL removeTranscriptionist(int rep) throws HL7Exception {
    return (NDL) super.removeRepetition(35, rep);
  }


  /**
   * Removes a repetition of
   * OBR-35: "Transcriptionist" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public NDL removeObr35_Transcriptionist(int rep) throws HL7Exception {
    return (NDL) super.removeRepetition(35, rep);
  }


  /**
   * Returns
   * OBR-36: "Scheduled Date/Time" - creates it if necessary
   */
  public TS getScheduledDateTime() {
    TS retVal = this.getTypedField(36, 0);
    return retVal;
  }

  /**
   * Returns
   * OBR-36: "Scheduled Date/Time" - creates it if necessary
   */
  public TS getObr36_ScheduledDateTime() {
    TS retVal = this.getTypedField(36, 0);
    return retVal;
  }


  /**
   * Returns all repetitions of Number of Sample Containers (OBR-37).
   */
  public NM[] getNumberOfSampleContainers() {
    NM[] retVal = this.getTypedField(37, new NM[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Number of Sample Containers (OBR-37).
   */
  public NM[] getObr37_NumberOfSampleContainers() {
    NM[] retVal = this.getTypedField(37, new NM[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Number of Sample Containers (OBR-37).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getNumberOfSampleContainersReps() {
    return this.getReps(37);
  }


  /**
   * Returns a specific repetition of
   * OBR-37: "Number of Sample Containers" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public NM getNumberOfSampleContainers(int rep) {
    NM retVal = this.getTypedField(37, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * OBR-37: "Number of Sample Containers" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public NM getObr37_NumberOfSampleContainers(int rep) {
    NM retVal = this.getTypedField(37, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Number of Sample Containers (OBR-37).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getObr37_NumberOfSampleContainersReps() {
    return this.getReps(37);
  }


  /**
   * Inserts a repetition of
   * OBR-37: "Number of Sample Containers" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public NM insertNumberOfSampleContainers(int rep) throws HL7Exception {
    return (NM) super.insertRepetition(37, rep);
  }


  /**
   * Inserts a repetition of
   * OBR-37: "Number of Sample Containers" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public NM insertObr37_NumberOfSampleContainers(int rep) throws HL7Exception {
    return (NM) super.insertRepetition(37, rep);
  }


  /**
   * Removes a repetition of
   * OBR-37: "Number of Sample Containers" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public NM removeNumberOfSampleContainers(int rep) throws HL7Exception {
    return (NM) super.removeRepetition(37, rep);
  }


  /**
   * Removes a repetition of
   * OBR-37: "Number of Sample Containers" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public NM removeObr37_NumberOfSampleContainers(int rep) throws HL7Exception {
    return (NM) super.removeRepetition(37, rep);
  }


  /**
   * Returns all repetitions of Transport Logistics of Collected Sample (OBR-38).
   */
  public CWE[] getTransportLogisticsOfCollectedSample() {
    CWE[] retVal = this.getTypedField(38, new CWE[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Transport Logistics of Collected Sample (OBR-38).
   */
  public CWE[] getObr38_TransportLogisticsOfCollectedSample() {
    CWE[] retVal = this.getTypedField(38, new CWE[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Transport Logistics of Collected Sample (OBR-38).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getTransportLogisticsOfCollectedSampleReps() {
    return this.getReps(38);
  }


  /**
   * Returns a specific repetition of
   * OBR-38: "Transport Logistics of Collected Sample" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getTransportLogisticsOfCollectedSample(int rep) {
    CWE retVal = this.getTypedField(38, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * OBR-38: "Transport Logistics of Collected Sample" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getObr38_TransportLogisticsOfCollectedSample(int rep) {
    CWE retVal = this.getTypedField(38, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Transport Logistics of Collected Sample (OBR-38).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getObr38_TransportLogisticsOfCollectedSampleReps() {
    return this.getReps(38);
  }


  /**
   * Inserts a repetition of
   * OBR-38: "Transport Logistics of Collected Sample" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertTransportLogisticsOfCollectedSample(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(38, rep);
  }


  /**
   * Inserts a repetition of
   * OBR-38: "Transport Logistics of Collected Sample" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertObr38_TransportLogisticsOfCollectedSample(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(38, rep);
  }


  /**
   * Removes a repetition of
   * OBR-38: "Transport Logistics of Collected Sample" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeTransportLogisticsOfCollectedSample(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(38, rep);
  }


  /**
   * Removes a repetition of
   * OBR-38: "Transport Logistics of Collected Sample" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeObr38_TransportLogisticsOfCollectedSample(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(38, rep);
  }


  /**
   * Returns all repetitions of Collector's Comment (OBR-39).
   */
  public CWE[] getCollectorSComment() {
    CWE[] retVal = this.getTypedField(39, new CWE[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Collector's Comment (OBR-39).
   */
  public CWE[] getObr39_CollectorSComment() {
    CWE[] retVal = this.getTypedField(39, new CWE[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Collector's Comment (OBR-39).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getCollectorSCommentReps() {
    return this.getReps(39);
  }


  /**
   * Returns a specific repetition of
   * OBR-39: "Collector's Comment" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getCollectorSComment(int rep) {
    CWE retVal = this.getTypedField(39, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * OBR-39: "Collector's Comment" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getObr39_CollectorSComment(int rep) {
    CWE retVal = this.getTypedField(39, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Collector's Comment (OBR-39).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getObr39_CollectorSCommentReps() {
    return this.getReps(39);
  }


  /**
   * Inserts a repetition of
   * OBR-39: "Collector's Comment" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertCollectorSComment(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(39, rep);
  }


  /**
   * Inserts a repetition of
   * OBR-39: "Collector's Comment" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertObr39_CollectorSComment(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(39, rep);
  }


  /**
   * Removes a repetition of
   * OBR-39: "Collector's Comment" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeCollectorSComment(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(39, rep);
  }


  /**
   * Removes a repetition of
   * OBR-39: "Collector's Comment" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeObr39_CollectorSComment(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(39, rep);
  }


  /**
   * Returns all repetitions of Transport Arrangement Responsibility (OBR-40).
   */
  public CWE[] getTransportArrangementResponsibility() {
    CWE[] retVal = this.getTypedField(40, new CWE[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Transport Arrangement Responsibility (OBR-40).
   */
  public CWE[] getObr40_TransportArrangementResponsibility() {
    CWE[] retVal = this.getTypedField(40, new CWE[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Transport Arrangement Responsibility (OBR-40).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getTransportArrangementResponsibilityReps() {
    return this.getReps(40);
  }


  /**
   * Returns a specific repetition of
   * OBR-40: "Transport Arrangement Responsibility" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getTransportArrangementResponsibility(int rep) {
    CWE retVal = this.getTypedField(40, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * OBR-40: "Transport Arrangement Responsibility" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getObr40_TransportArrangementResponsibility(int rep) {
    CWE retVal = this.getTypedField(40, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Transport Arrangement Responsibility (OBR-40).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getObr40_TransportArrangementResponsibilityReps() {
    return this.getReps(40);
  }


  /**
   * Inserts a repetition of
   * OBR-40: "Transport Arrangement Responsibility" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertTransportArrangementResponsibility(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(40, rep);
  }


  /**
   * Inserts a repetition of
   * OBR-40: "Transport Arrangement Responsibility" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertObr40_TransportArrangementResponsibility(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(40, rep);
  }


  /**
   * Removes a repetition of
   * OBR-40: "Transport Arrangement Responsibility" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeTransportArrangementResponsibility(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(40, rep);
  }


  /**
   * Removes a repetition of
   * OBR-40: "Transport Arrangement Responsibility" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeObr40_TransportArrangementResponsibility(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(40, rep);
  }


  /**
   * Returns all repetitions of Transport Arranged (OBR-41).
   */
  public ID[] getTransportArranged() {
    ID[] retVal = this.getTypedField(41, new ID[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Transport Arranged (OBR-41).
   */
  public ID[] getObr41_TransportArranged() {
    ID[] retVal = this.getTypedField(41, new ID[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Transport Arranged (OBR-41).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getTransportArrangedReps() {
    return this.getReps(41);
  }


  /**
   * Returns a specific repetition of
   * OBR-41: "Transport Arranged" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public ID getTransportArranged(int rep) {
    ID retVal = this.getTypedField(41, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * OBR-41: "Transport Arranged" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public ID getObr41_TransportArranged(int rep) {
    ID retVal = this.getTypedField(41, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Transport Arranged (OBR-41).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getObr41_TransportArrangedReps() {
    return this.getReps(41);
  }


  /**
   * Inserts a repetition of
   * OBR-41: "Transport Arranged" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID insertTransportArranged(int rep) throws HL7Exception {
    return (ID) super.insertRepetition(41, rep);
  }


  /**
   * Inserts a repetition of
   * OBR-41: "Transport Arranged" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID insertObr41_TransportArranged(int rep) throws HL7Exception {
    return (ID) super.insertRepetition(41, rep);
  }


  /**
   * Removes a repetition of
   * OBR-41: "Transport Arranged" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID removeTransportArranged(int rep) throws HL7Exception {
    return (ID) super.removeRepetition(41, rep);
  }


  /**
   * Removes a repetition of
   * OBR-41: "Transport Arranged" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID removeObr41_TransportArranged(int rep) throws HL7Exception {
    return (ID) super.removeRepetition(41, rep);
  }


  /**
   * Returns all repetitions of Escort Required (OBR-42).
   */
  public ID[] getEscortRequired() {
    ID[] retVal = this.getTypedField(42, new ID[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Escort Required (OBR-42).
   */
  public ID[] getObr42_EscortRequired() {
    ID[] retVal = this.getTypedField(42, new ID[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Escort Required (OBR-42).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getEscortRequiredReps() {
    return this.getReps(42);
  }


  /**
   * Returns a specific repetition of
   * OBR-42: "Escort Required" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public ID getEscortRequired(int rep) {
    ID retVal = this.getTypedField(42, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * OBR-42: "Escort Required" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public ID getObr42_EscortRequired(int rep) {
    ID retVal = this.getTypedField(42, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Escort Required (OBR-42).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getObr42_EscortRequiredReps() {
    return this.getReps(42);
  }


  /**
   * Inserts a repetition of
   * OBR-42: "Escort Required" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID insertEscortRequired(int rep) throws HL7Exception {
    return (ID) super.insertRepetition(42, rep);
  }


  /**
   * Inserts a repetition of
   * OBR-42: "Escort Required" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID insertObr42_EscortRequired(int rep) throws HL7Exception {
    return (ID) super.insertRepetition(42, rep);
  }


  /**
   * Removes a repetition of
   * OBR-42: "Escort Required" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID removeEscortRequired(int rep) throws HL7Exception {
    return (ID) super.removeRepetition(42, rep);
  }


  /**
   * Removes a repetition of
   * OBR-42: "Escort Required" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID removeObr42_EscortRequired(int rep) throws HL7Exception {
    return (ID) super.removeRepetition(42, rep);
  }


  /**
   * Returns all repetitions of Planned Patient Transport Comment (OBR-43).
   */
  public CWE[] getPlannedPatientTransportComment() {
    CWE[] retVal = this.getTypedField(43, new CWE[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Planned Patient Transport Comment (OBR-43).
   */
  public CWE[] getObr43_PlannedPatientTransportComment() {
    CWE[] retVal = this.getTypedField(43, new CWE[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Planned Patient Transport Comment (OBR-43).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPlannedPatientTransportCommentReps() {
    return this.getReps(43);
  }


  /**
   * Returns a specific repetition of
   * OBR-43: "Planned Patient Transport Comment" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getPlannedPatientTransportComment(int rep) {
    CWE retVal = this.getTypedField(43, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * OBR-43: "Planned Patient Transport Comment" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getObr43_PlannedPatientTransportComment(int rep) {
    CWE retVal = this.getTypedField(43, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Planned Patient Transport Comment (OBR-43).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getObr43_PlannedPatientTransportCommentReps() {
    return this.getReps(43);
  }


  /**
   * Inserts a repetition of
   * OBR-43: "Planned Patient Transport Comment" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertPlannedPatientTransportComment(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(43, rep);
  }


  /**
   * Inserts a repetition of
   * OBR-43: "Planned Patient Transport Comment" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertObr43_PlannedPatientTransportComment(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(43, rep);
  }


  /**
   * Removes a repetition of
   * OBR-43: "Planned Patient Transport Comment" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removePlannedPatientTransportComment(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(43, rep);
  }


  /**
   * Removes a repetition of
   * OBR-43: "Planned Patient Transport Comment" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeObr43_PlannedPatientTransportComment(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(43, rep);
  }


  /**
   * Returns
   * OBR-44: "Procedure Code" - creates it if necessary
   */
  public CWE getProcedureCode() {
    CWE retVal = this.getTypedField(44, 0);
    return retVal;
  }

  /**
   * Returns
   * OBR-44: "Procedure Code" - creates it if necessary
   */
  public CWE getObr44_ProcedureCode() {
    CWE retVal = this.getTypedField(44, 0);
    return retVal;
  }


  /**
   * Returns all repetitions of Procedure Code Modifier (OBR-45).
   */
  public CWE[] getProcedureCodeModifier() {
    CWE[] retVal = this.getTypedField(45, new CWE[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Procedure Code Modifier (OBR-45).
   */
  public CWE[] getObr45_ProcedureCodeModifier() {
    CWE[] retVal = this.getTypedField(45, new CWE[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Procedure Code Modifier (OBR-45).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getProcedureCodeModifierReps() {
    return this.getReps(45);
  }


  /**
   * Returns a specific repetition of
   * OBR-45: "Procedure Code Modifier" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getProcedureCodeModifier(int rep) {
    CWE retVal = this.getTypedField(45, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * OBR-45: "Procedure Code Modifier" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getObr45_ProcedureCodeModifier(int rep) {
    CWE retVal = this.getTypedField(45, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Procedure Code Modifier (OBR-45).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getObr45_ProcedureCodeModifierReps() {
    return this.getReps(45);
  }


  /**
   * Inserts a repetition of
   * OBR-45: "Procedure Code Modifier" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertProcedureCodeModifier(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(45, rep);
  }


  /**
   * Inserts a repetition of
   * OBR-45: "Procedure Code Modifier" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertObr45_ProcedureCodeModifier(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(45, rep);
  }


  /**
   * Removes a repetition of
   * OBR-45: "Procedure Code Modifier" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeProcedureCodeModifier(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(45, rep);
  }


  /**
   * Removes a repetition of
   * OBR-45: "Procedure Code Modifier" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeObr45_ProcedureCodeModifier(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(45, rep);
  }


  /**
   * Returns all repetitions of Placer Supplemental Service Information (OBR-46).
   */
  public CWE[] getPlacerSupplementalServiceInformation() {
    CWE[] retVal = this.getTypedField(46, new CWE[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Placer Supplemental Service Information (OBR-46).
   */
  public CWE[] getObr46_PlacerSupplementalServiceInformation() {
    CWE[] retVal = this.getTypedField(46, new CWE[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Placer Supplemental Service Information (OBR-46).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPlacerSupplementalServiceInformationReps() {
    return this.getReps(46);
  }


  /**
   * Returns a specific repetition of
   * OBR-46: "Placer Supplemental Service Information" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getPlacerSupplementalServiceInformation(int rep) {
    CWE retVal = this.getTypedField(46, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * OBR-46: "Placer Supplemental Service Information" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getObr46_PlacerSupplementalServiceInformation(int rep) {
    CWE retVal = this.getTypedField(46, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Placer Supplemental Service Information (OBR-46).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getObr46_PlacerSupplementalServiceInformationReps() {
    return this.getReps(46);
  }


  /**
   * Inserts a repetition of
   * OBR-46: "Placer Supplemental Service Information" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertPlacerSupplementalServiceInformation(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(46, rep);
  }


  /**
   * Inserts a repetition of
   * OBR-46: "Placer Supplemental Service Information" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertObr46_PlacerSupplementalServiceInformation(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(46, rep);
  }


  /**
   * Removes a repetition of
   * OBR-46: "Placer Supplemental Service Information" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removePlacerSupplementalServiceInformation(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(46, rep);
  }


  /**
   * Removes a repetition of
   * OBR-46: "Placer Supplemental Service Information" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeObr46_PlacerSupplementalServiceInformation(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(46, rep);
  }


  /**
   * Returns all repetitions of Filler Supplemental Service Information (OBR-47).
   */
  public CWE[] getFillerSupplementalServiceInformation() {
    CWE[] retVal = this.getTypedField(47, new CWE[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Filler Supplemental Service Information (OBR-47).
   */
  public CWE[] getObr47_FillerSupplementalServiceInformation() {
    CWE[] retVal = this.getTypedField(47, new CWE[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Filler Supplemental Service Information (OBR-47).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getFillerSupplementalServiceInformationReps() {
    return this.getReps(47);
  }


  /**
   * Returns a specific repetition of
   * OBR-47: "Filler Supplemental Service Information" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getFillerSupplementalServiceInformation(int rep) {
    CWE retVal = this.getTypedField(47, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * OBR-47: "Filler Supplemental Service Information" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getObr47_FillerSupplementalServiceInformation(int rep) {
    CWE retVal = this.getTypedField(47, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Filler Supplemental Service Information (OBR-47).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getObr47_FillerSupplementalServiceInformationReps() {
    return this.getReps(47);
  }


  /**
   * Inserts a repetition of
   * OBR-47: "Filler Supplemental Service Information" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertFillerSupplementalServiceInformation(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(47, rep);
  }


  /**
   * Inserts a repetition of
   * OBR-47: "Filler Supplemental Service Information" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertObr47_FillerSupplementalServiceInformation(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(47, rep);
  }


  /**
   * Removes a repetition of
   * OBR-47: "Filler Supplemental Service Information" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeFillerSupplementalServiceInformation(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(47, rep);
  }


  /**
   * Removes a repetition of
   * OBR-47: "Filler Supplemental Service Information" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeObr47_FillerSupplementalServiceInformation(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(47, rep);
  }


  /**
   * Returns all repetitions of Medically Necessary Duplicate Procedure Reason. (OBR-48).
   */
  public CWE[] getMedicallyNecessaryDuplicateProcedureReason() {
    CWE[] retVal = this.getTypedField(48, new CWE[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Medically Necessary Duplicate Procedure Reason. (OBR-48).
   */
  public CWE[] getObr48_MedicallyNecessaryDuplicateProcedureReason() {
    CWE[] retVal = this.getTypedField(48, new CWE[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Medically Necessary Duplicate Procedure Reason. (OBR-48).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getMedicallyNecessaryDuplicateProcedureReasonReps() {
    return this.getReps(48);
  }


  /**
   * Returns a specific repetition of
   * OBR-48: "Medically Necessary Duplicate Procedure Reason." - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getMedicallyNecessaryDuplicateProcedureReason(int rep) {
    CWE retVal = this.getTypedField(48, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * OBR-48: "Medically Necessary Duplicate Procedure Reason." - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getObr48_MedicallyNecessaryDuplicateProcedureReason(int rep) {
    CWE retVal = this.getTypedField(48, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Medically Necessary Duplicate Procedure Reason. (OBR-48).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getObr48_MedicallyNecessaryDuplicateProcedureReasonReps() {
    return this.getReps(48);
  }


  /**
   * Inserts a repetition of
   * OBR-48: "Medically Necessary Duplicate Procedure Reason." at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertMedicallyNecessaryDuplicateProcedureReason(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(48, rep);
  }


  /**
   * Inserts a repetition of
   * OBR-48: "Medically Necessary Duplicate Procedure Reason." at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertObr48_MedicallyNecessaryDuplicateProcedureReason(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(48, rep);
  }


  /**
   * Removes a repetition of
   * OBR-48: "Medically Necessary Duplicate Procedure Reason." at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeMedicallyNecessaryDuplicateProcedureReason(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(48, rep);
  }


  /**
   * Removes a repetition of
   * OBR-48: "Medically Necessary Duplicate Procedure Reason." at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeObr48_MedicallyNecessaryDuplicateProcedureReason(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(48, rep);
  }


  /**
   * Returns
   * OBR-49: "Result Handling" - creates it if necessary
   */
  public IS getResultHandling() {
    IS retVal = this.getTypedField(49, 0);
    return retVal;
  }

  /**
   * Returns
   * OBR-49: "Result Handling" - creates it if necessary
   */
  public IS getObr49_ResultHandling() {
    IS retVal = this.getTypedField(49, 0);
    return retVal;
  }


  /**
   * Returns
   * OBR-50: "Parent Universal Service Identifier" - creates it if necessary
   */
  public CWE getParentUniversalServiceIdentifier() {
    CWE retVal = this.getTypedField(50, 0);
    return retVal;
  }

  /**
   * Returns
   * OBR-50: "Parent Universal Service Identifier" - creates it if necessary
   */
  public CWE getObr50_ParentUniversalServiceIdentifier() {
    CWE retVal = this.getTypedField(50, 0);
    return retVal;
  }


  /**
   * {@inheritDoc}
   */
  protected Type createNewTypeWithoutReflection(int field) {
    switch (field) {
      case 0:
        return new SI(getMessage());
      case 1:
        return new EI(getMessage());
      case 2:
        return new EI(getMessage());
      case 3:
        return new CWE_ELR(getMessage());
      case 4:
        return new ID(getMessage(), 0);
      case 5:
        return new TS(getMessage());
      case 6:
        return new TS(getMessage());
      case 7:
        return new TS(getMessage());
      case 8:
        return new CQ(getMessage());
      case 9:
        return new XCN(getMessage());
      case 10:
        return new ID(getMessage(), 65);
      case 11:
        return new CWE(getMessage());
      case 12:
        return new ST(getMessage());
      case 13:
        return new TS(getMessage());
      case 14:
        return new SPS(getMessage());
      case 15:
        return new XCN(getMessage());
      case 16:
        return new XTN(getMessage());
      case 17:
        return new ST(getMessage());
      case 18:
        return new ST(getMessage());
      case 19:
        return new ST(getMessage());
      case 20:
        return new ST(getMessage());
      case 21:
        return new TS(getMessage());
      case 22:
        return new MOC(getMessage());
      case 23:
        return new ID(getMessage(), 74);
      case 24:
        return new ID(getMessage(), 123);
      case 25:
        return new PRL_ELR(getMessage());
      case 26:
        return new TQ(getMessage());
      case 27:
        return new XCN(getMessage());
      case 28:
        return new EIP(getMessage());
      case 29:
        return new ID(getMessage(), 0);
      case 30:
        return new CWE_ELR(getMessage());
      case 31:
        return new NDL(getMessage());
      case 32:
        return new NDL(getMessage());
      case 33:
        return new NDL(getMessage());
      case 34:
        return new NDL(getMessage());
      case 35:
        return new TS(getMessage());
      case 36:
        return new NM(getMessage());
      case 37:
        return new CWE(getMessage());
      case 38:
        return new CWE(getMessage());
      case 39:
        return new CWE(getMessage());
      case 40:
        return new ID(getMessage(), 0);
      case 41:
        return new ID(getMessage(), 0);
      case 42:
        return new CWE(getMessage());
      case 43:
        return new CWE(getMessage());
      case 44:
        return new CWE(getMessage());
      case 45:
        return new CWE(getMessage());
      case 46:
        return new CWE(getMessage());
      case 47:
        return new CWE(getMessage());
      case 48:
        return new IS(getMessage(), 507);
      case 49:
        return new CWE(getMessage());
      default:
        return null;
    }
  }


}

