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

// import gov.cdc.nist.group.*;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.AbstractSegment;
import ca.uhn.hl7v2.model.Group;
import ca.uhn.hl7v2.model.Type;
import ca.uhn.hl7v2.model.v251.datatype.*;
import ca.uhn.hl7v2.parser.ModelClassFactory;


/**
 * <p>Represents an HL7 PV2 message segment (Patient Visit - Additional Information).
 * This segment has the following fields:</p>
 * <ul>
 * <li>PV2-1: Prior Pending Location (PL) <b>optional repeating</b>
 * <li>PV2-2: Accommodation Code (CWE) <b>optional repeating</b>
 * <li>PV2-3: Admit Reason (CWE) <b>optional </b>
 * <li>PV2-4: Transfer Reason (CWE) <b>optional repeating</b>
 * <li>PV2-5: Patient Valuables (ST) <b>optional repeating</b>
 * <li>PV2-6: Patient Valuables Location (ST) <b>optional repeating</b>
 * <li>PV2-7: Visit User Code (IS) <b>optional repeating</b>
 * <li>PV2-8: Expected Admit Date/Time (TS) <b>optional repeating</b>
 * <li>PV2-9: Expected Discharge Date/Time (TS) <b>optional repeating</b>
 * <li>PV2-10: Estimated Length of Inpatient Stay (NM) <b>optional repeating</b>
 * <li>PV2-11: Actual Length of Inpatient Stay (NM) <b>optional repeating</b>
 * <li>PV2-12: Visit Description (ST) <b>optional repeating</b>
 * <li>PV2-13: Referral Source Code (XCN) <b>optional repeating</b>
 * <li>PV2-14: Previous Service Date (DT) <b>optional repeating</b>
 * <li>PV2-15: Employment Illness Related Indicator (ID) <b>optional </b>
 * <li>PV2-16: Purge Status Code (IS) <b>optional repeating</b>
 * <li>PV2-17: Purge Status Date (DT) <b>optional repeating</b>
 * <li>PV2-18: Special Program Code (IS) <b>optional repeating</b>
 * <li>PV2-19: Retention Indicator (ID) <b>optional repeating</b>
 * <li>PV2-20: Expected Number of Insurance Plans (NM) <b>optional repeating</b>
 * <li>PV2-21: Visit Publicity Code (IS) <b>optional repeating</b>
 * <li>PV2-22: Visit Protection Indicator (ID) <b>optional repeating</b>
 * <li>PV2-23: Clinic Organization Name (XON) <b>optional repeating</b>
 * <li>PV2-24: Patient Status Code (IS) <b>optional repeating</b>
 * <li>PV2-25: Visit Priority Code (IS) <b>optional repeating</b>
 * <li>PV2-26: Previous Treatment Date (DT) <b>optional </b>
 * <li>PV2-27: Expected Discharge Disposition (IS) <b>optional repeating</b>
 * <li>PV2-28: Signature on File Date (DT) <b>optional repeating</b>
 * <li>PV2-29: First Similar Illness Date (DT) <b>optional </b>
 * <li>PV2-30: Patient Charge Adjustment Code (CWE) <b>optional repeating</b>
 * <li>PV2-31: Recurring Service Code (IS) <b>optional repeating</b>
 * <li>PV2-32: Billing Media Code (ID) <b>optional repeating</b>
 * <li>PV2-33: Expected Surgery Date and Time (TS) <b>optional repeating</b>
 * <li>PV2-34: Military Partnership Code (ID) <b>optional repeating</b>
 * <li>PV2-35: Military Non-Availability Code (ID) <b>optional repeating</b>
 * <li>PV2-36: Newborn Baby Indicator (ID) <b>optional repeating</b>
 * <li>PV2-37: Baby Detained Indicator (ID) <b>optional repeating</b>
 * <li>PV2-38: Mode of Arrival Code (CWE) <b>optional repeating</b>
 * <li>PV2-39: Recreational Drug Use Code (CWE) <b>optional repeating</b>
 * <li>PV2-40: Admission Level of Care Code (CWE) <b>optional </b>
 * <li>PV2-41: Precaution Code (CWE) <b>optional repeating</b>
 * <li>PV2-42: Patient Condition Code (CWE) <b>optional repeating</b>
 * <li>PV2-43: Living Will Code (IS) <b>optional repeating</b>
 * <li>PV2-44: Organ Donor Code (IS) <b>optional repeating</b>
 * <li>PV2-45: Advance Directive Code (CWE) <b>optional repeating</b>
 * <li>PV2-46: Patient Status Effective Date (DT) <b>optional repeating</b>
 * <li>PV2-47: Expected LOA Return Date/Time (TS) <b>optional repeating</b>
 * <li>PV2-48: Expected Pre-admission Testing Date/Time (TS) <b>optional repeating</b>
 * <li>PV2-49: Notify Clergy Code (IS) <b>optional repeating</b>
 * </ul>
 */
@SuppressWarnings("unused")
public class PV2 extends AbstractSegment {

  /**
   * Creates a new PV2 segment
   */
  public PV2(Group parent, ModelClassFactory factory) {
    super(parent, factory);
    init(factory);
  }

  private void init(ModelClassFactory factory) {
    try {
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage()}, "Prior Pending Location");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage()}, "Accommodation Code");
      this.add(CWE.class, false, 1, 0, new Object[]{getMessage()}, "Admit Reason");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage()}, "Transfer Reason");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage()}, "Patient Valuables");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage()}, "Patient Valuables Location");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage(), 0}, "Visit User Code");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage()}, "Expected Admit Date/Time");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage()}, "Expected Discharge Date/Time");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage()}, "Estimated Length of Inpatient Stay");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage()}, "Actual Length of Inpatient Stay");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage()}, "Visit Description");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage()}, "Referral Source Code");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage()}, "Previous Service Date");
      this.add(ID.class, false, 1, 0, new Object[]{getMessage(), 136}, "Employment Illness Related Indicator");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage(), 0}, "Purge Status Code");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage()}, "Purge Status Date");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage(), 0}, "Special Program Code");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage(), 0}, "Retention Indicator");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage()}, "Expected Number of Insurance Plans");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage(), 0}, "Visit Publicity Code");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage(), 0}, "Visit Protection Indicator");
      this.add(XON.class, false, -1, 0, new Object[]{getMessage()}, "Clinic Organization Name");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage(), 0}, "Patient Status Code");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage(), 0}, "Visit Priority Code");
      this.add(DT.class, false, 1, 0, new Object[]{getMessage()}, "Previous Treatment Date");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage(), 0}, "Expected Discharge Disposition");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage()}, "Signature on File Date");
      this.add(DT.class, false, 1, 0, new Object[]{getMessage()}, "First Similar Illness Date");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage()}, "Patient Charge Adjustment Code");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage(), 0}, "Recurring Service Code");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage(), 0}, "Billing Media Code");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage()}, "Expected Surgery Date and Time");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage(), 0}, "Military Partnership Code");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage(), 0}, "Military Non-Availability Code");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage(), 0}, "Newborn Baby Indicator");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage(), 0}, "Baby Detained Indicator");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage()}, "Mode of Arrival Code");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage()}, "Recreational Drug Use Code");
      this.add(CWE.class, false, 1, 0, new Object[]{getMessage()}, "Admission Level of Care Code");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage()}, "Precaution Code");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage()}, "Patient Condition Code");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage(), 0}, "Living Will Code");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage(), 0}, "Organ Donor Code");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage()}, "Advance Directive Code");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage()}, "Patient Status Effective Date");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage()}, "Expected LOA Return Date/Time");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage()}, "Expected Pre-admission Testing Date/Time");
      this.add(NULLDT.class, false, 0, 0, new Object[]{getMessage(), 0}, "Notify Clergy Code");
    } catch (HL7Exception e) {
      log.error("Unexpected error creating PV2 - this is probably a bug in the source code generator.", e);
    }
  }


  /**
   * Returns all repetitions of Prior Pending Location (PV2-1).
   */
  public PL[] getPriorPendingLocation() {
    PL[] retVal = this.getTypedField(1, new PL[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Prior Pending Location (PV2-1).
   */
  public PL[] getPv21_PriorPendingLocation() {
    PL[] retVal = this.getTypedField(1, new PL[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Prior Pending Location (PV2-1).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPriorPendingLocationReps() {
    return this.getReps(1);
  }


  /**
   * Returns a specific repetition of
   * PV2-1: "Prior Pending Location" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public PL getPriorPendingLocation(int rep) {
    PL retVal = this.getTypedField(1, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PV2-1: "Prior Pending Location" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public PL getPv21_PriorPendingLocation(int rep) {
    PL retVal = this.getTypedField(1, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Prior Pending Location (PV2-1).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPv21_PriorPendingLocationReps() {
    return this.getReps(1);
  }


  /**
   * Inserts a repetition of
   * PV2-1: "Prior Pending Location" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public PL insertPriorPendingLocation(int rep) throws HL7Exception {
    return (PL) super.insertRepetition(1, rep);
  }


  /**
   * Inserts a repetition of
   * PV2-1: "Prior Pending Location" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public PL insertPv21_PriorPendingLocation(int rep) throws HL7Exception {
    return (PL) super.insertRepetition(1, rep);
  }


  /**
   * Removes a repetition of
   * PV2-1: "Prior Pending Location" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public PL removePriorPendingLocation(int rep) throws HL7Exception {
    return (PL) super.removeRepetition(1, rep);
  }


  /**
   * Removes a repetition of
   * PV2-1: "Prior Pending Location" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public PL removePv21_PriorPendingLocation(int rep) throws HL7Exception {
    return (PL) super.removeRepetition(1, rep);
  }


  /**
   * Returns all repetitions of Accommodation Code (PV2-2).
   */
  public CWE[] getAccommodationCode() {
    CWE[] retVal = this.getTypedField(2, new CWE[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Accommodation Code (PV2-2).
   */
  public CWE[] getPv22_AccommodationCode() {
    CWE[] retVal = this.getTypedField(2, new CWE[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Accommodation Code (PV2-2).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getAccommodationCodeReps() {
    return this.getReps(2);
  }


  /**
   * Returns a specific repetition of
   * PV2-2: "Accommodation Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getAccommodationCode(int rep) {
    CWE retVal = this.getTypedField(2, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PV2-2: "Accommodation Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getPv22_AccommodationCode(int rep) {
    CWE retVal = this.getTypedField(2, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Accommodation Code (PV2-2).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPv22_AccommodationCodeReps() {
    return this.getReps(2);
  }


  /**
   * Inserts a repetition of
   * PV2-2: "Accommodation Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertAccommodationCode(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(2, rep);
  }


  /**
   * Inserts a repetition of
   * PV2-2: "Accommodation Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertPv22_AccommodationCode(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(2, rep);
  }


  /**
   * Removes a repetition of
   * PV2-2: "Accommodation Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeAccommodationCode(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(2, rep);
  }


  /**
   * Removes a repetition of
   * PV2-2: "Accommodation Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removePv22_AccommodationCode(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(2, rep);
  }


  /**
   * Returns
   * PV2-3: "Admit Reason" - creates it if necessary
   */
  public CWE getAdmitReason() {
    CWE retVal = this.getTypedField(3, 0);
    return retVal;
  }

  /**
   * Returns
   * PV2-3: "Admit Reason" - creates it if necessary
   */
  public CWE getPv23_AdmitReason() {
    CWE retVal = this.getTypedField(3, 0);
    return retVal;
  }


  /**
   * Returns all repetitions of Transfer Reason (PV2-4).
   */
  public CWE[] getTransferReason() {
    CWE[] retVal = this.getTypedField(4, new CWE[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Transfer Reason (PV2-4).
   */
  public CWE[] getPv24_TransferReason() {
    CWE[] retVal = this.getTypedField(4, new CWE[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Transfer Reason (PV2-4).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getTransferReasonReps() {
    return this.getReps(4);
  }


  /**
   * Returns a specific repetition of
   * PV2-4: "Transfer Reason" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getTransferReason(int rep) {
    CWE retVal = this.getTypedField(4, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PV2-4: "Transfer Reason" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getPv24_TransferReason(int rep) {
    CWE retVal = this.getTypedField(4, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Transfer Reason (PV2-4).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPv24_TransferReasonReps() {
    return this.getReps(4);
  }


  /**
   * Inserts a repetition of
   * PV2-4: "Transfer Reason" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertTransferReason(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(4, rep);
  }


  /**
   * Inserts a repetition of
   * PV2-4: "Transfer Reason" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertPv24_TransferReason(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(4, rep);
  }


  /**
   * Removes a repetition of
   * PV2-4: "Transfer Reason" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeTransferReason(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(4, rep);
  }


  /**
   * Removes a repetition of
   * PV2-4: "Transfer Reason" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removePv24_TransferReason(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(4, rep);
  }


  /**
   * Returns all repetitions of Patient Valuables (PV2-5).
   */
  public ST[] getPatientValuables() {
    ST[] retVal = this.getTypedField(5, new ST[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Patient Valuables (PV2-5).
   */
  public ST[] getPv25_PatientValuables() {
    ST[] retVal = this.getTypedField(5, new ST[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Patient Valuables (PV2-5).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPatientValuablesReps() {
    return this.getReps(5);
  }


  /**
   * Returns a specific repetition of
   * PV2-5: "Patient Valuables" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public ST getPatientValuables(int rep) {
    ST retVal = this.getTypedField(5, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PV2-5: "Patient Valuables" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public ST getPv25_PatientValuables(int rep) {
    ST retVal = this.getTypedField(5, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Patient Valuables (PV2-5).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPv25_PatientValuablesReps() {
    return this.getReps(5);
  }


  /**
   * Inserts a repetition of
   * PV2-5: "Patient Valuables" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ST insertPatientValuables(int rep) throws HL7Exception {
    return (ST) super.insertRepetition(5, rep);
  }


  /**
   * Inserts a repetition of
   * PV2-5: "Patient Valuables" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ST insertPv25_PatientValuables(int rep) throws HL7Exception {
    return (ST) super.insertRepetition(5, rep);
  }


  /**
   * Removes a repetition of
   * PV2-5: "Patient Valuables" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ST removePatientValuables(int rep) throws HL7Exception {
    return (ST) super.removeRepetition(5, rep);
  }


  /**
   * Removes a repetition of
   * PV2-5: "Patient Valuables" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ST removePv25_PatientValuables(int rep) throws HL7Exception {
    return (ST) super.removeRepetition(5, rep);
  }


  /**
   * Returns all repetitions of Patient Valuables Location (PV2-6).
   */
  public ST[] getPatientValuablesLocation() {
    ST[] retVal = this.getTypedField(6, new ST[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Patient Valuables Location (PV2-6).
   */
  public ST[] getPv26_PatientValuablesLocation() {
    ST[] retVal = this.getTypedField(6, new ST[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Patient Valuables Location (PV2-6).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPatientValuablesLocationReps() {
    return this.getReps(6);
  }


  /**
   * Returns a specific repetition of
   * PV2-6: "Patient Valuables Location" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public ST getPatientValuablesLocation(int rep) {
    ST retVal = this.getTypedField(6, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PV2-6: "Patient Valuables Location" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public ST getPv26_PatientValuablesLocation(int rep) {
    ST retVal = this.getTypedField(6, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Patient Valuables Location (PV2-6).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPv26_PatientValuablesLocationReps() {
    return this.getReps(6);
  }


  /**
   * Inserts a repetition of
   * PV2-6: "Patient Valuables Location" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ST insertPatientValuablesLocation(int rep) throws HL7Exception {
    return (ST) super.insertRepetition(6, rep);
  }


  /**
   * Inserts a repetition of
   * PV2-6: "Patient Valuables Location" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ST insertPv26_PatientValuablesLocation(int rep) throws HL7Exception {
    return (ST) super.insertRepetition(6, rep);
  }


  /**
   * Removes a repetition of
   * PV2-6: "Patient Valuables Location" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ST removePatientValuablesLocation(int rep) throws HL7Exception {
    return (ST) super.removeRepetition(6, rep);
  }


  /**
   * Removes a repetition of
   * PV2-6: "Patient Valuables Location" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ST removePv26_PatientValuablesLocation(int rep) throws HL7Exception {
    return (ST) super.removeRepetition(6, rep);
  }


  /**
   * Returns all repetitions of Visit User Code (PV2-7).
   */
  public IS[] getVisitUserCode() {
    IS[] retVal = this.getTypedField(7, new IS[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Visit User Code (PV2-7).
   */
  public IS[] getPv27_VisitUserCode() {
    IS[] retVal = this.getTypedField(7, new IS[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Visit User Code (PV2-7).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getVisitUserCodeReps() {
    return this.getReps(7);
  }


  /**
   * Returns a specific repetition of
   * PV2-7: "Visit User Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public IS getVisitUserCode(int rep) {
    IS retVal = this.getTypedField(7, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PV2-7: "Visit User Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public IS getPv27_VisitUserCode(int rep) {
    IS retVal = this.getTypedField(7, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Visit User Code (PV2-7).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPv27_VisitUserCodeReps() {
    return this.getReps(7);
  }


  /**
   * Inserts a repetition of
   * PV2-7: "Visit User Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS insertVisitUserCode(int rep) throws HL7Exception {
    return (IS) super.insertRepetition(7, rep);
  }


  /**
   * Inserts a repetition of
   * PV2-7: "Visit User Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS insertPv27_VisitUserCode(int rep) throws HL7Exception {
    return (IS) super.insertRepetition(7, rep);
  }


  /**
   * Removes a repetition of
   * PV2-7: "Visit User Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS removeVisitUserCode(int rep) throws HL7Exception {
    return (IS) super.removeRepetition(7, rep);
  }


  /**
   * Removes a repetition of
   * PV2-7: "Visit User Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS removePv27_VisitUserCode(int rep) throws HL7Exception {
    return (IS) super.removeRepetition(7, rep);
  }


  /**
   * Returns all repetitions of Expected Admit Date/Time (PV2-8).
   */
  public TS[] getExpectedAdmitDateTime() {
    TS[] retVal = this.getTypedField(8, new TS[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Expected Admit Date/Time (PV2-8).
   */
  public TS[] getPv28_ExpectedAdmitDateTime() {
    TS[] retVal = this.getTypedField(8, new TS[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Expected Admit Date/Time (PV2-8).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getExpectedAdmitDateTimeReps() {
    return this.getReps(8);
  }


  /**
   * Returns a specific repetition of
   * PV2-8: "Expected Admit Date/Time" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public TS getExpectedAdmitDateTime(int rep) {
    TS retVal = this.getTypedField(8, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PV2-8: "Expected Admit Date/Time" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public TS getPv28_ExpectedAdmitDateTime(int rep) {
    TS retVal = this.getTypedField(8, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Expected Admit Date/Time (PV2-8).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPv28_ExpectedAdmitDateTimeReps() {
    return this.getReps(8);
  }


  /**
   * Inserts a repetition of
   * PV2-8: "Expected Admit Date/Time" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public TS insertExpectedAdmitDateTime(int rep) throws HL7Exception {
    return (TS) super.insertRepetition(8, rep);
  }


  /**
   * Inserts a repetition of
   * PV2-8: "Expected Admit Date/Time" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public TS insertPv28_ExpectedAdmitDateTime(int rep) throws HL7Exception {
    return (TS) super.insertRepetition(8, rep);
  }


  /**
   * Removes a repetition of
   * PV2-8: "Expected Admit Date/Time" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public TS removeExpectedAdmitDateTime(int rep) throws HL7Exception {
    return (TS) super.removeRepetition(8, rep);
  }


  /**
   * Removes a repetition of
   * PV2-8: "Expected Admit Date/Time" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public TS removePv28_ExpectedAdmitDateTime(int rep) throws HL7Exception {
    return (TS) super.removeRepetition(8, rep);
  }


  /**
   * Returns all repetitions of Expected Discharge Date/Time (PV2-9).
   */
  public TS[] getExpectedDischargeDateTime() {
    TS[] retVal = this.getTypedField(9, new TS[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Expected Discharge Date/Time (PV2-9).
   */
  public TS[] getPv29_ExpectedDischargeDateTime() {
    TS[] retVal = this.getTypedField(9, new TS[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Expected Discharge Date/Time (PV2-9).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getExpectedDischargeDateTimeReps() {
    return this.getReps(9);
  }


  /**
   * Returns a specific repetition of
   * PV2-9: "Expected Discharge Date/Time" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public TS getExpectedDischargeDateTime(int rep) {
    TS retVal = this.getTypedField(9, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PV2-9: "Expected Discharge Date/Time" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public TS getPv29_ExpectedDischargeDateTime(int rep) {
    TS retVal = this.getTypedField(9, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Expected Discharge Date/Time (PV2-9).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPv29_ExpectedDischargeDateTimeReps() {
    return this.getReps(9);
  }


  /**
   * Inserts a repetition of
   * PV2-9: "Expected Discharge Date/Time" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public TS insertExpectedDischargeDateTime(int rep) throws HL7Exception {
    return (TS) super.insertRepetition(9, rep);
  }


  /**
   * Inserts a repetition of
   * PV2-9: "Expected Discharge Date/Time" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public TS insertPv29_ExpectedDischargeDateTime(int rep) throws HL7Exception {
    return (TS) super.insertRepetition(9, rep);
  }


  /**
   * Removes a repetition of
   * PV2-9: "Expected Discharge Date/Time" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public TS removeExpectedDischargeDateTime(int rep) throws HL7Exception {
    return (TS) super.removeRepetition(9, rep);
  }


  /**
   * Removes a repetition of
   * PV2-9: "Expected Discharge Date/Time" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public TS removePv29_ExpectedDischargeDateTime(int rep) throws HL7Exception {
    return (TS) super.removeRepetition(9, rep);
  }


  /**
   * Returns all repetitions of Estimated Length of Inpatient Stay (PV2-10).
   */
  public NM[] getEstimatedLengthOfInpatientStay() {
    NM[] retVal = this.getTypedField(10, new NM[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Estimated Length of Inpatient Stay (PV2-10).
   */
  public NM[] getPv210_EstimatedLengthOfInpatientStay() {
    NM[] retVal = this.getTypedField(10, new NM[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Estimated Length of Inpatient Stay (PV2-10).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getEstimatedLengthOfInpatientStayReps() {
    return this.getReps(10);
  }


  /**
   * Returns a specific repetition of
   * PV2-10: "Estimated Length of Inpatient Stay" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public NM getEstimatedLengthOfInpatientStay(int rep) {
    NM retVal = this.getTypedField(10, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PV2-10: "Estimated Length of Inpatient Stay" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public NM getPv210_EstimatedLengthOfInpatientStay(int rep) {
    NM retVal = this.getTypedField(10, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Estimated Length of Inpatient Stay (PV2-10).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPv210_EstimatedLengthOfInpatientStayReps() {
    return this.getReps(10);
  }


  /**
   * Inserts a repetition of
   * PV2-10: "Estimated Length of Inpatient Stay" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public NM insertEstimatedLengthOfInpatientStay(int rep) throws HL7Exception {
    return (NM) super.insertRepetition(10, rep);
  }


  /**
   * Inserts a repetition of
   * PV2-10: "Estimated Length of Inpatient Stay" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public NM insertPv210_EstimatedLengthOfInpatientStay(int rep) throws HL7Exception {
    return (NM) super.insertRepetition(10, rep);
  }


  /**
   * Removes a repetition of
   * PV2-10: "Estimated Length of Inpatient Stay" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public NM removeEstimatedLengthOfInpatientStay(int rep) throws HL7Exception {
    return (NM) super.removeRepetition(10, rep);
  }


  /**
   * Removes a repetition of
   * PV2-10: "Estimated Length of Inpatient Stay" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public NM removePv210_EstimatedLengthOfInpatientStay(int rep) throws HL7Exception {
    return (NM) super.removeRepetition(10, rep);
  }


  /**
   * Returns all repetitions of Actual Length of Inpatient Stay (PV2-11).
   */
  public NM[] getActualLengthOfInpatientStay() {
    NM[] retVal = this.getTypedField(11, new NM[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Actual Length of Inpatient Stay (PV2-11).
   */
  public NM[] getPv211_ActualLengthOfInpatientStay() {
    NM[] retVal = this.getTypedField(11, new NM[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Actual Length of Inpatient Stay (PV2-11).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getActualLengthOfInpatientStayReps() {
    return this.getReps(11);
  }


  /**
   * Returns a specific repetition of
   * PV2-11: "Actual Length of Inpatient Stay" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public NM getActualLengthOfInpatientStay(int rep) {
    NM retVal = this.getTypedField(11, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PV2-11: "Actual Length of Inpatient Stay" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public NM getPv211_ActualLengthOfInpatientStay(int rep) {
    NM retVal = this.getTypedField(11, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Actual Length of Inpatient Stay (PV2-11).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPv211_ActualLengthOfInpatientStayReps() {
    return this.getReps(11);
  }


  /**
   * Inserts a repetition of
   * PV2-11: "Actual Length of Inpatient Stay" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public NM insertActualLengthOfInpatientStay(int rep) throws HL7Exception {
    return (NM) super.insertRepetition(11, rep);
  }


  /**
   * Inserts a repetition of
   * PV2-11: "Actual Length of Inpatient Stay" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public NM insertPv211_ActualLengthOfInpatientStay(int rep) throws HL7Exception {
    return (NM) super.insertRepetition(11, rep);
  }


  /**
   * Removes a repetition of
   * PV2-11: "Actual Length of Inpatient Stay" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public NM removeActualLengthOfInpatientStay(int rep) throws HL7Exception {
    return (NM) super.removeRepetition(11, rep);
  }


  /**
   * Removes a repetition of
   * PV2-11: "Actual Length of Inpatient Stay" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public NM removePv211_ActualLengthOfInpatientStay(int rep) throws HL7Exception {
    return (NM) super.removeRepetition(11, rep);
  }


  /**
   * Returns all repetitions of Visit Description (PV2-12).
   */
  public ST[] getVisitDescription() {
    ST[] retVal = this.getTypedField(12, new ST[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Visit Description (PV2-12).
   */
  public ST[] getPv212_VisitDescription() {
    ST[] retVal = this.getTypedField(12, new ST[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Visit Description (PV2-12).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getVisitDescriptionReps() {
    return this.getReps(12);
  }


  /**
   * Returns a specific repetition of
   * PV2-12: "Visit Description" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public ST getVisitDescription(int rep) {
    ST retVal = this.getTypedField(12, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PV2-12: "Visit Description" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public ST getPv212_VisitDescription(int rep) {
    ST retVal = this.getTypedField(12, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Visit Description (PV2-12).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPv212_VisitDescriptionReps() {
    return this.getReps(12);
  }


  /**
   * Inserts a repetition of
   * PV2-12: "Visit Description" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ST insertVisitDescription(int rep) throws HL7Exception {
    return (ST) super.insertRepetition(12, rep);
  }


  /**
   * Inserts a repetition of
   * PV2-12: "Visit Description" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ST insertPv212_VisitDescription(int rep) throws HL7Exception {
    return (ST) super.insertRepetition(12, rep);
  }


  /**
   * Removes a repetition of
   * PV2-12: "Visit Description" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ST removeVisitDescription(int rep) throws HL7Exception {
    return (ST) super.removeRepetition(12, rep);
  }


  /**
   * Removes a repetition of
   * PV2-12: "Visit Description" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ST removePv212_VisitDescription(int rep) throws HL7Exception {
    return (ST) super.removeRepetition(12, rep);
  }


  /**
   * Returns all repetitions of Referral Source Code (PV2-13).
   */
  public XCN[] getReferralSourceCode() {
    XCN[] retVal = this.getTypedField(13, new XCN[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Referral Source Code (PV2-13).
   */
  public XCN[] getPv213_ReferralSourceCode() {
    XCN[] retVal = this.getTypedField(13, new XCN[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Referral Source Code (PV2-13).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getReferralSourceCodeReps() {
    return this.getReps(13);
  }


  /**
   * Returns a specific repetition of
   * PV2-13: "Referral Source Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XCN getReferralSourceCode(int rep) {
    XCN retVal = this.getTypedField(13, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PV2-13: "Referral Source Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XCN getPv213_ReferralSourceCode(int rep) {
    XCN retVal = this.getTypedField(13, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Referral Source Code (PV2-13).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPv213_ReferralSourceCodeReps() {
    return this.getReps(13);
  }


  /**
   * Inserts a repetition of
   * PV2-13: "Referral Source Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN insertReferralSourceCode(int rep) throws HL7Exception {
    return (XCN) super.insertRepetition(13, rep);
  }


  /**
   * Inserts a repetition of
   * PV2-13: "Referral Source Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN insertPv213_ReferralSourceCode(int rep) throws HL7Exception {
    return (XCN) super.insertRepetition(13, rep);
  }


  /**
   * Removes a repetition of
   * PV2-13: "Referral Source Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN removeReferralSourceCode(int rep) throws HL7Exception {
    return (XCN) super.removeRepetition(13, rep);
  }


  /**
   * Removes a repetition of
   * PV2-13: "Referral Source Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN removePv213_ReferralSourceCode(int rep) throws HL7Exception {
    return (XCN) super.removeRepetition(13, rep);
  }


  /**
   * Returns all repetitions of Previous Service Date (PV2-14).
   */
  public DT[] getPreviousServiceDate() {
    DT[] retVal = this.getTypedField(14, new DT[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Previous Service Date (PV2-14).
   */
  public DT[] getPv214_PreviousServiceDate() {
    DT[] retVal = this.getTypedField(14, new DT[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Previous Service Date (PV2-14).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPreviousServiceDateReps() {
    return this.getReps(14);
  }


  /**
   * Returns a specific repetition of
   * PV2-14: "Previous Service Date" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public DT getPreviousServiceDate(int rep) {
    DT retVal = this.getTypedField(14, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PV2-14: "Previous Service Date" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public DT getPv214_PreviousServiceDate(int rep) {
    DT retVal = this.getTypedField(14, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Previous Service Date (PV2-14).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPv214_PreviousServiceDateReps() {
    return this.getReps(14);
  }


  /**
   * Inserts a repetition of
   * PV2-14: "Previous Service Date" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public DT insertPreviousServiceDate(int rep) throws HL7Exception {
    return (DT) super.insertRepetition(14, rep);
  }


  /**
   * Inserts a repetition of
   * PV2-14: "Previous Service Date" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public DT insertPv214_PreviousServiceDate(int rep) throws HL7Exception {
    return (DT) super.insertRepetition(14, rep);
  }


  /**
   * Removes a repetition of
   * PV2-14: "Previous Service Date" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public DT removePreviousServiceDate(int rep) throws HL7Exception {
    return (DT) super.removeRepetition(14, rep);
  }


  /**
   * Removes a repetition of
   * PV2-14: "Previous Service Date" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public DT removePv214_PreviousServiceDate(int rep) throws HL7Exception {
    return (DT) super.removeRepetition(14, rep);
  }


  /**
   * Returns
   * PV2-15: "Employment Illness Related Indicator" - creates it if necessary
   */
  public ID getEmploymentIllnessRelatedIndicator() {
    ID retVal = this.getTypedField(15, 0);
    return retVal;
  }

  /**
   * Returns
   * PV2-15: "Employment Illness Related Indicator" - creates it if necessary
   */
  public ID getPv215_EmploymentIllnessRelatedIndicator() {
    ID retVal = this.getTypedField(15, 0);
    return retVal;
  }


  /**
   * Returns all repetitions of Purge Status Code (PV2-16).
   */
  public IS[] getPurgeStatusCode() {
    IS[] retVal = this.getTypedField(16, new IS[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Purge Status Code (PV2-16).
   */
  public IS[] getPv216_PurgeStatusCode() {
    IS[] retVal = this.getTypedField(16, new IS[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Purge Status Code (PV2-16).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPurgeStatusCodeReps() {
    return this.getReps(16);
  }


  /**
   * Returns a specific repetition of
   * PV2-16: "Purge Status Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public IS getPurgeStatusCode(int rep) {
    IS retVal = this.getTypedField(16, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PV2-16: "Purge Status Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public IS getPv216_PurgeStatusCode(int rep) {
    IS retVal = this.getTypedField(16, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Purge Status Code (PV2-16).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPv216_PurgeStatusCodeReps() {
    return this.getReps(16);
  }


  /**
   * Inserts a repetition of
   * PV2-16: "Purge Status Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS insertPurgeStatusCode(int rep) throws HL7Exception {
    return (IS) super.insertRepetition(16, rep);
  }


  /**
   * Inserts a repetition of
   * PV2-16: "Purge Status Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS insertPv216_PurgeStatusCode(int rep) throws HL7Exception {
    return (IS) super.insertRepetition(16, rep);
  }


  /**
   * Removes a repetition of
   * PV2-16: "Purge Status Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS removePurgeStatusCode(int rep) throws HL7Exception {
    return (IS) super.removeRepetition(16, rep);
  }


  /**
   * Removes a repetition of
   * PV2-16: "Purge Status Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS removePv216_PurgeStatusCode(int rep) throws HL7Exception {
    return (IS) super.removeRepetition(16, rep);
  }


  /**
   * Returns all repetitions of Purge Status Date (PV2-17).
   */
  public DT[] getPurgeStatusDate() {
    DT[] retVal = this.getTypedField(17, new DT[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Purge Status Date (PV2-17).
   */
  public DT[] getPv217_PurgeStatusDate() {
    DT[] retVal = this.getTypedField(17, new DT[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Purge Status Date (PV2-17).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPurgeStatusDateReps() {
    return this.getReps(17);
  }


  /**
   * Returns a specific repetition of
   * PV2-17: "Purge Status Date" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public DT getPurgeStatusDate(int rep) {
    DT retVal = this.getTypedField(17, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PV2-17: "Purge Status Date" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public DT getPv217_PurgeStatusDate(int rep) {
    DT retVal = this.getTypedField(17, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Purge Status Date (PV2-17).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPv217_PurgeStatusDateReps() {
    return this.getReps(17);
  }


  /**
   * Inserts a repetition of
   * PV2-17: "Purge Status Date" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public DT insertPurgeStatusDate(int rep) throws HL7Exception {
    return (DT) super.insertRepetition(17, rep);
  }


  /**
   * Inserts a repetition of
   * PV2-17: "Purge Status Date" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public DT insertPv217_PurgeStatusDate(int rep) throws HL7Exception {
    return (DT) super.insertRepetition(17, rep);
  }


  /**
   * Removes a repetition of
   * PV2-17: "Purge Status Date" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public DT removePurgeStatusDate(int rep) throws HL7Exception {
    return (DT) super.removeRepetition(17, rep);
  }


  /**
   * Removes a repetition of
   * PV2-17: "Purge Status Date" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public DT removePv217_PurgeStatusDate(int rep) throws HL7Exception {
    return (DT) super.removeRepetition(17, rep);
  }


  /**
   * Returns all repetitions of Special Program Code (PV2-18).
   */
  public IS[] getSpecialProgramCode() {
    IS[] retVal = this.getTypedField(18, new IS[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Special Program Code (PV2-18).
   */
  public IS[] getPv218_SpecialProgramCode() {
    IS[] retVal = this.getTypedField(18, new IS[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Special Program Code (PV2-18).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getSpecialProgramCodeReps() {
    return this.getReps(18);
  }


  /**
   * Returns a specific repetition of
   * PV2-18: "Special Program Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public IS getSpecialProgramCode(int rep) {
    IS retVal = this.getTypedField(18, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PV2-18: "Special Program Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public IS getPv218_SpecialProgramCode(int rep) {
    IS retVal = this.getTypedField(18, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Special Program Code (PV2-18).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPv218_SpecialProgramCodeReps() {
    return this.getReps(18);
  }


  /**
   * Inserts a repetition of
   * PV2-18: "Special Program Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS insertSpecialProgramCode(int rep) throws HL7Exception {
    return (IS) super.insertRepetition(18, rep);
  }


  /**
   * Inserts a repetition of
   * PV2-18: "Special Program Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS insertPv218_SpecialProgramCode(int rep) throws HL7Exception {
    return (IS) super.insertRepetition(18, rep);
  }


  /**
   * Removes a repetition of
   * PV2-18: "Special Program Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS removeSpecialProgramCode(int rep) throws HL7Exception {
    return (IS) super.removeRepetition(18, rep);
  }


  /**
   * Removes a repetition of
   * PV2-18: "Special Program Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS removePv218_SpecialProgramCode(int rep) throws HL7Exception {
    return (IS) super.removeRepetition(18, rep);
  }


  /**
   * Returns all repetitions of Retention Indicator (PV2-19).
   */
  public ID[] getRetentionIndicator() {
    ID[] retVal = this.getTypedField(19, new ID[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Retention Indicator (PV2-19).
   */
  public ID[] getPv219_RetentionIndicator() {
    ID[] retVal = this.getTypedField(19, new ID[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Retention Indicator (PV2-19).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getRetentionIndicatorReps() {
    return this.getReps(19);
  }


  /**
   * Returns a specific repetition of
   * PV2-19: "Retention Indicator" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public ID getRetentionIndicator(int rep) {
    ID retVal = this.getTypedField(19, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PV2-19: "Retention Indicator" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public ID getPv219_RetentionIndicator(int rep) {
    ID retVal = this.getTypedField(19, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Retention Indicator (PV2-19).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPv219_RetentionIndicatorReps() {
    return this.getReps(19);
  }


  /**
   * Inserts a repetition of
   * PV2-19: "Retention Indicator" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID insertRetentionIndicator(int rep) throws HL7Exception {
    return (ID) super.insertRepetition(19, rep);
  }


  /**
   * Inserts a repetition of
   * PV2-19: "Retention Indicator" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID insertPv219_RetentionIndicator(int rep) throws HL7Exception {
    return (ID) super.insertRepetition(19, rep);
  }


  /**
   * Removes a repetition of
   * PV2-19: "Retention Indicator" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID removeRetentionIndicator(int rep) throws HL7Exception {
    return (ID) super.removeRepetition(19, rep);
  }


  /**
   * Removes a repetition of
   * PV2-19: "Retention Indicator" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID removePv219_RetentionIndicator(int rep) throws HL7Exception {
    return (ID) super.removeRepetition(19, rep);
  }


  /**
   * Returns all repetitions of Expected Number of Insurance Plans (PV2-20).
   */
  public NM[] getExpectedNumberOfInsurancePlans() {
    NM[] retVal = this.getTypedField(20, new NM[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Expected Number of Insurance Plans (PV2-20).
   */
  public NM[] getPv220_ExpectedNumberOfInsurancePlans() {
    NM[] retVal = this.getTypedField(20, new NM[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Expected Number of Insurance Plans (PV2-20).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getExpectedNumberOfInsurancePlansReps() {
    return this.getReps(20);
  }


  /**
   * Returns a specific repetition of
   * PV2-20: "Expected Number of Insurance Plans" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public NM getExpectedNumberOfInsurancePlans(int rep) {
    NM retVal = this.getTypedField(20, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PV2-20: "Expected Number of Insurance Plans" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public NM getPv220_ExpectedNumberOfInsurancePlans(int rep) {
    NM retVal = this.getTypedField(20, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Expected Number of Insurance Plans (PV2-20).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPv220_ExpectedNumberOfInsurancePlansReps() {
    return this.getReps(20);
  }


  /**
   * Inserts a repetition of
   * PV2-20: "Expected Number of Insurance Plans" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public NM insertExpectedNumberOfInsurancePlans(int rep) throws HL7Exception {
    return (NM) super.insertRepetition(20, rep);
  }


  /**
   * Inserts a repetition of
   * PV2-20: "Expected Number of Insurance Plans" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public NM insertPv220_ExpectedNumberOfInsurancePlans(int rep) throws HL7Exception {
    return (NM) super.insertRepetition(20, rep);
  }


  /**
   * Removes a repetition of
   * PV2-20: "Expected Number of Insurance Plans" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public NM removeExpectedNumberOfInsurancePlans(int rep) throws HL7Exception {
    return (NM) super.removeRepetition(20, rep);
  }


  /**
   * Removes a repetition of
   * PV2-20: "Expected Number of Insurance Plans" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public NM removePv220_ExpectedNumberOfInsurancePlans(int rep) throws HL7Exception {
    return (NM) super.removeRepetition(20, rep);
  }


  /**
   * Returns all repetitions of Visit Publicity Code (PV2-21).
   */
  public IS[] getVisitPublicityCode() {
    IS[] retVal = this.getTypedField(21, new IS[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Visit Publicity Code (PV2-21).
   */
  public IS[] getPv221_VisitPublicityCode() {
    IS[] retVal = this.getTypedField(21, new IS[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Visit Publicity Code (PV2-21).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getVisitPublicityCodeReps() {
    return this.getReps(21);
  }


  /**
   * Returns a specific repetition of
   * PV2-21: "Visit Publicity Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public IS getVisitPublicityCode(int rep) {
    IS retVal = this.getTypedField(21, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PV2-21: "Visit Publicity Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public IS getPv221_VisitPublicityCode(int rep) {
    IS retVal = this.getTypedField(21, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Visit Publicity Code (PV2-21).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPv221_VisitPublicityCodeReps() {
    return this.getReps(21);
  }


  /**
   * Inserts a repetition of
   * PV2-21: "Visit Publicity Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS insertVisitPublicityCode(int rep) throws HL7Exception {
    return (IS) super.insertRepetition(21, rep);
  }


  /**
   * Inserts a repetition of
   * PV2-21: "Visit Publicity Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS insertPv221_VisitPublicityCode(int rep) throws HL7Exception {
    return (IS) super.insertRepetition(21, rep);
  }


  /**
   * Removes a repetition of
   * PV2-21: "Visit Publicity Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS removeVisitPublicityCode(int rep) throws HL7Exception {
    return (IS) super.removeRepetition(21, rep);
  }


  /**
   * Removes a repetition of
   * PV2-21: "Visit Publicity Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS removePv221_VisitPublicityCode(int rep) throws HL7Exception {
    return (IS) super.removeRepetition(21, rep);
  }


  /**
   * Returns all repetitions of Visit Protection Indicator (PV2-22).
   */
  public ID[] getVisitProtectionIndicator() {
    ID[] retVal = this.getTypedField(22, new ID[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Visit Protection Indicator (PV2-22).
   */
  public ID[] getPv222_VisitProtectionIndicator() {
    ID[] retVal = this.getTypedField(22, new ID[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Visit Protection Indicator (PV2-22).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getVisitProtectionIndicatorReps() {
    return this.getReps(22);
  }


  /**
   * Returns a specific repetition of
   * PV2-22: "Visit Protection Indicator" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public ID getVisitProtectionIndicator(int rep) {
    ID retVal = this.getTypedField(22, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PV2-22: "Visit Protection Indicator" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public ID getPv222_VisitProtectionIndicator(int rep) {
    ID retVal = this.getTypedField(22, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Visit Protection Indicator (PV2-22).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPv222_VisitProtectionIndicatorReps() {
    return this.getReps(22);
  }


  /**
   * Inserts a repetition of
   * PV2-22: "Visit Protection Indicator" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID insertVisitProtectionIndicator(int rep) throws HL7Exception {
    return (ID) super.insertRepetition(22, rep);
  }


  /**
   * Inserts a repetition of
   * PV2-22: "Visit Protection Indicator" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID insertPv222_VisitProtectionIndicator(int rep) throws HL7Exception {
    return (ID) super.insertRepetition(22, rep);
  }


  /**
   * Removes a repetition of
   * PV2-22: "Visit Protection Indicator" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID removeVisitProtectionIndicator(int rep) throws HL7Exception {
    return (ID) super.removeRepetition(22, rep);
  }


  /**
   * Removes a repetition of
   * PV2-22: "Visit Protection Indicator" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID removePv222_VisitProtectionIndicator(int rep) throws HL7Exception {
    return (ID) super.removeRepetition(22, rep);
  }


  /**
   * Returns all repetitions of Clinic Organization Name (PV2-23).
   */
  public XON[] getClinicOrganizationName() {
    XON[] retVal = this.getTypedField(23, new XON[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Clinic Organization Name (PV2-23).
   */
  public XON[] getPv223_ClinicOrganizationName() {
    XON[] retVal = this.getTypedField(23, new XON[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Clinic Organization Name (PV2-23).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getClinicOrganizationNameReps() {
    return this.getReps(23);
  }


  /**
   * Returns a specific repetition of
   * PV2-23: "Clinic Organization Name" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XON getClinicOrganizationName(int rep) {
    XON retVal = this.getTypedField(23, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PV2-23: "Clinic Organization Name" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XON getPv223_ClinicOrganizationName(int rep) {
    XON retVal = this.getTypedField(23, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Clinic Organization Name (PV2-23).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPv223_ClinicOrganizationNameReps() {
    return this.getReps(23);
  }


  /**
   * Inserts a repetition of
   * PV2-23: "Clinic Organization Name" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XON insertClinicOrganizationName(int rep) throws HL7Exception {
    return (XON) super.insertRepetition(23, rep);
  }


  /**
   * Inserts a repetition of
   * PV2-23: "Clinic Organization Name" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XON insertPv223_ClinicOrganizationName(int rep) throws HL7Exception {
    return (XON) super.insertRepetition(23, rep);
  }


  /**
   * Removes a repetition of
   * PV2-23: "Clinic Organization Name" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XON removeClinicOrganizationName(int rep) throws HL7Exception {
    return (XON) super.removeRepetition(23, rep);
  }


  /**
   * Removes a repetition of
   * PV2-23: "Clinic Organization Name" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XON removePv223_ClinicOrganizationName(int rep) throws HL7Exception {
    return (XON) super.removeRepetition(23, rep);
  }


  /**
   * Returns all repetitions of Patient Status Code (PV2-24).
   */
  public IS[] getPatientStatusCode() {
    IS[] retVal = this.getTypedField(24, new IS[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Patient Status Code (PV2-24).
   */
  public IS[] getPv224_PatientStatusCode() {
    IS[] retVal = this.getTypedField(24, new IS[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Patient Status Code (PV2-24).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPatientStatusCodeReps() {
    return this.getReps(24);
  }


  /**
   * Returns a specific repetition of
   * PV2-24: "Patient Status Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public IS getPatientStatusCode(int rep) {
    IS retVal = this.getTypedField(24, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PV2-24: "Patient Status Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public IS getPv224_PatientStatusCode(int rep) {
    IS retVal = this.getTypedField(24, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Patient Status Code (PV2-24).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPv224_PatientStatusCodeReps() {
    return this.getReps(24);
  }


  /**
   * Inserts a repetition of
   * PV2-24: "Patient Status Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS insertPatientStatusCode(int rep) throws HL7Exception {
    return (IS) super.insertRepetition(24, rep);
  }


  /**
   * Inserts a repetition of
   * PV2-24: "Patient Status Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS insertPv224_PatientStatusCode(int rep) throws HL7Exception {
    return (IS) super.insertRepetition(24, rep);
  }


  /**
   * Removes a repetition of
   * PV2-24: "Patient Status Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS removePatientStatusCode(int rep) throws HL7Exception {
    return (IS) super.removeRepetition(24, rep);
  }


  /**
   * Removes a repetition of
   * PV2-24: "Patient Status Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS removePv224_PatientStatusCode(int rep) throws HL7Exception {
    return (IS) super.removeRepetition(24, rep);
  }


  /**
   * Returns all repetitions of Visit Priority Code (PV2-25).
   */
  public IS[] getVisitPriorityCode() {
    IS[] retVal = this.getTypedField(25, new IS[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Visit Priority Code (PV2-25).
   */
  public IS[] getPv225_VisitPriorityCode() {
    IS[] retVal = this.getTypedField(25, new IS[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Visit Priority Code (PV2-25).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getVisitPriorityCodeReps() {
    return this.getReps(25);
  }


  /**
   * Returns a specific repetition of
   * PV2-25: "Visit Priority Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public IS getVisitPriorityCode(int rep) {
    IS retVal = this.getTypedField(25, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PV2-25: "Visit Priority Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public IS getPv225_VisitPriorityCode(int rep) {
    IS retVal = this.getTypedField(25, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Visit Priority Code (PV2-25).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPv225_VisitPriorityCodeReps() {
    return this.getReps(25);
  }


  /**
   * Inserts a repetition of
   * PV2-25: "Visit Priority Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS insertVisitPriorityCode(int rep) throws HL7Exception {
    return (IS) super.insertRepetition(25, rep);
  }


  /**
   * Inserts a repetition of
   * PV2-25: "Visit Priority Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS insertPv225_VisitPriorityCode(int rep) throws HL7Exception {
    return (IS) super.insertRepetition(25, rep);
  }


  /**
   * Removes a repetition of
   * PV2-25: "Visit Priority Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS removeVisitPriorityCode(int rep) throws HL7Exception {
    return (IS) super.removeRepetition(25, rep);
  }


  /**
   * Removes a repetition of
   * PV2-25: "Visit Priority Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS removePv225_VisitPriorityCode(int rep) throws HL7Exception {
    return (IS) super.removeRepetition(25, rep);
  }


  /**
   * Returns
   * PV2-26: "Previous Treatment Date" - creates it if necessary
   */
  public DT getPreviousTreatmentDate() {
    DT retVal = this.getTypedField(26, 0);
    return retVal;
  }

  /**
   * Returns
   * PV2-26: "Previous Treatment Date" - creates it if necessary
   */
  public DT getPv226_PreviousTreatmentDate() {
    DT retVal = this.getTypedField(26, 0);
    return retVal;
  }


  /**
   * Returns all repetitions of Expected Discharge Disposition (PV2-27).
   */
  public IS[] getExpectedDischargeDisposition() {
    IS[] retVal = this.getTypedField(27, new IS[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Expected Discharge Disposition (PV2-27).
   */
  public IS[] getPv227_ExpectedDischargeDisposition() {
    IS[] retVal = this.getTypedField(27, new IS[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Expected Discharge Disposition (PV2-27).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getExpectedDischargeDispositionReps() {
    return this.getReps(27);
  }


  /**
   * Returns a specific repetition of
   * PV2-27: "Expected Discharge Disposition" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public IS getExpectedDischargeDisposition(int rep) {
    IS retVal = this.getTypedField(27, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PV2-27: "Expected Discharge Disposition" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public IS getPv227_ExpectedDischargeDisposition(int rep) {
    IS retVal = this.getTypedField(27, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Expected Discharge Disposition (PV2-27).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPv227_ExpectedDischargeDispositionReps() {
    return this.getReps(27);
  }


  /**
   * Inserts a repetition of
   * PV2-27: "Expected Discharge Disposition" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS insertExpectedDischargeDisposition(int rep) throws HL7Exception {
    return (IS) super.insertRepetition(27, rep);
  }


  /**
   * Inserts a repetition of
   * PV2-27: "Expected Discharge Disposition" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS insertPv227_ExpectedDischargeDisposition(int rep) throws HL7Exception {
    return (IS) super.insertRepetition(27, rep);
  }


  /**
   * Removes a repetition of
   * PV2-27: "Expected Discharge Disposition" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS removeExpectedDischargeDisposition(int rep) throws HL7Exception {
    return (IS) super.removeRepetition(27, rep);
  }


  /**
   * Removes a repetition of
   * PV2-27: "Expected Discharge Disposition" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS removePv227_ExpectedDischargeDisposition(int rep) throws HL7Exception {
    return (IS) super.removeRepetition(27, rep);
  }


  /**
   * Returns all repetitions of Signature on File Date (PV2-28).
   */
  public DT[] getSignatureOnFileDate() {
    DT[] retVal = this.getTypedField(28, new DT[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Signature on File Date (PV2-28).
   */
  public DT[] getPv228_SignatureOnFileDate() {
    DT[] retVal = this.getTypedField(28, new DT[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Signature on File Date (PV2-28).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getSignatureOnFileDateReps() {
    return this.getReps(28);
  }


  /**
   * Returns a specific repetition of
   * PV2-28: "Signature on File Date" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public DT getSignatureOnFileDate(int rep) {
    DT retVal = this.getTypedField(28, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PV2-28: "Signature on File Date" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public DT getPv228_SignatureOnFileDate(int rep) {
    DT retVal = this.getTypedField(28, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Signature on File Date (PV2-28).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPv228_SignatureOnFileDateReps() {
    return this.getReps(28);
  }


  /**
   * Inserts a repetition of
   * PV2-28: "Signature on File Date" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public DT insertSignatureOnFileDate(int rep) throws HL7Exception {
    return (DT) super.insertRepetition(28, rep);
  }


  /**
   * Inserts a repetition of
   * PV2-28: "Signature on File Date" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public DT insertPv228_SignatureOnFileDate(int rep) throws HL7Exception {
    return (DT) super.insertRepetition(28, rep);
  }


  /**
   * Removes a repetition of
   * PV2-28: "Signature on File Date" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public DT removeSignatureOnFileDate(int rep) throws HL7Exception {
    return (DT) super.removeRepetition(28, rep);
  }


  /**
   * Removes a repetition of
   * PV2-28: "Signature on File Date" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public DT removePv228_SignatureOnFileDate(int rep) throws HL7Exception {
    return (DT) super.removeRepetition(28, rep);
  }


  /**
   * Returns
   * PV2-29: "First Similar Illness Date" - creates it if necessary
   */
  public DT getFirstSimilarIllnessDate() {
    DT retVal = this.getTypedField(29, 0);
    return retVal;
  }

  /**
   * Returns
   * PV2-29: "First Similar Illness Date" - creates it if necessary
   */
  public DT getPv229_FirstSimilarIllnessDate() {
    DT retVal = this.getTypedField(29, 0);
    return retVal;
  }


  /**
   * Returns all repetitions of Patient Charge Adjustment Code (PV2-30).
   */
  public CWE[] getPatientChargeAdjustmentCode() {
    CWE[] retVal = this.getTypedField(30, new CWE[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Patient Charge Adjustment Code (PV2-30).
   */
  public CWE[] getPv230_PatientChargeAdjustmentCode() {
    CWE[] retVal = this.getTypedField(30, new CWE[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Patient Charge Adjustment Code (PV2-30).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPatientChargeAdjustmentCodeReps() {
    return this.getReps(30);
  }


  /**
   * Returns a specific repetition of
   * PV2-30: "Patient Charge Adjustment Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getPatientChargeAdjustmentCode(int rep) {
    CWE retVal = this.getTypedField(30, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PV2-30: "Patient Charge Adjustment Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getPv230_PatientChargeAdjustmentCode(int rep) {
    CWE retVal = this.getTypedField(30, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Patient Charge Adjustment Code (PV2-30).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPv230_PatientChargeAdjustmentCodeReps() {
    return this.getReps(30);
  }


  /**
   * Inserts a repetition of
   * PV2-30: "Patient Charge Adjustment Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertPatientChargeAdjustmentCode(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(30, rep);
  }


  /**
   * Inserts a repetition of
   * PV2-30: "Patient Charge Adjustment Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertPv230_PatientChargeAdjustmentCode(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(30, rep);
  }


  /**
   * Removes a repetition of
   * PV2-30: "Patient Charge Adjustment Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removePatientChargeAdjustmentCode(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(30, rep);
  }


  /**
   * Removes a repetition of
   * PV2-30: "Patient Charge Adjustment Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removePv230_PatientChargeAdjustmentCode(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(30, rep);
  }


  /**
   * Returns all repetitions of Recurring Service Code (PV2-31).
   */
  public IS[] getRecurringServiceCode() {
    IS[] retVal = this.getTypedField(31, new IS[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Recurring Service Code (PV2-31).
   */
  public IS[] getPv231_RecurringServiceCode() {
    IS[] retVal = this.getTypedField(31, new IS[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Recurring Service Code (PV2-31).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getRecurringServiceCodeReps() {
    return this.getReps(31);
  }


  /**
   * Returns a specific repetition of
   * PV2-31: "Recurring Service Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public IS getRecurringServiceCode(int rep) {
    IS retVal = this.getTypedField(31, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PV2-31: "Recurring Service Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public IS getPv231_RecurringServiceCode(int rep) {
    IS retVal = this.getTypedField(31, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Recurring Service Code (PV2-31).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPv231_RecurringServiceCodeReps() {
    return this.getReps(31);
  }


  /**
   * Inserts a repetition of
   * PV2-31: "Recurring Service Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS insertRecurringServiceCode(int rep) throws HL7Exception {
    return (IS) super.insertRepetition(31, rep);
  }


  /**
   * Inserts a repetition of
   * PV2-31: "Recurring Service Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS insertPv231_RecurringServiceCode(int rep) throws HL7Exception {
    return (IS) super.insertRepetition(31, rep);
  }


  /**
   * Removes a repetition of
   * PV2-31: "Recurring Service Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS removeRecurringServiceCode(int rep) throws HL7Exception {
    return (IS) super.removeRepetition(31, rep);
  }


  /**
   * Removes a repetition of
   * PV2-31: "Recurring Service Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS removePv231_RecurringServiceCode(int rep) throws HL7Exception {
    return (IS) super.removeRepetition(31, rep);
  }


  /**
   * Returns all repetitions of Billing Media Code (PV2-32).
   */
  public ID[] getBillingMediaCode() {
    ID[] retVal = this.getTypedField(32, new ID[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Billing Media Code (PV2-32).
   */
  public ID[] getPv232_BillingMediaCode() {
    ID[] retVal = this.getTypedField(32, new ID[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Billing Media Code (PV2-32).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getBillingMediaCodeReps() {
    return this.getReps(32);
  }


  /**
   * Returns a specific repetition of
   * PV2-32: "Billing Media Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public ID getBillingMediaCode(int rep) {
    ID retVal = this.getTypedField(32, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PV2-32: "Billing Media Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public ID getPv232_BillingMediaCode(int rep) {
    ID retVal = this.getTypedField(32, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Billing Media Code (PV2-32).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPv232_BillingMediaCodeReps() {
    return this.getReps(32);
  }


  /**
   * Inserts a repetition of
   * PV2-32: "Billing Media Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID insertBillingMediaCode(int rep) throws HL7Exception {
    return (ID) super.insertRepetition(32, rep);
  }


  /**
   * Inserts a repetition of
   * PV2-32: "Billing Media Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID insertPv232_BillingMediaCode(int rep) throws HL7Exception {
    return (ID) super.insertRepetition(32, rep);
  }


  /**
   * Removes a repetition of
   * PV2-32: "Billing Media Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID removeBillingMediaCode(int rep) throws HL7Exception {
    return (ID) super.removeRepetition(32, rep);
  }


  /**
   * Removes a repetition of
   * PV2-32: "Billing Media Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID removePv232_BillingMediaCode(int rep) throws HL7Exception {
    return (ID) super.removeRepetition(32, rep);
  }


  /**
   * Returns all repetitions of Expected Surgery Date and Time (PV2-33).
   */
  public TS[] getExpectedSurgeryDateAndTime() {
    TS[] retVal = this.getTypedField(33, new TS[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Expected Surgery Date and Time (PV2-33).
   */
  public TS[] getPv233_ExpectedSurgeryDateAndTime() {
    TS[] retVal = this.getTypedField(33, new TS[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Expected Surgery Date and Time (PV2-33).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getExpectedSurgeryDateAndTimeReps() {
    return this.getReps(33);
  }


  /**
   * Returns a specific repetition of
   * PV2-33: "Expected Surgery Date and Time" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public TS getExpectedSurgeryDateAndTime(int rep) {
    TS retVal = this.getTypedField(33, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PV2-33: "Expected Surgery Date and Time" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public TS getPv233_ExpectedSurgeryDateAndTime(int rep) {
    TS retVal = this.getTypedField(33, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Expected Surgery Date and Time (PV2-33).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPv233_ExpectedSurgeryDateAndTimeReps() {
    return this.getReps(33);
  }


  /**
   * Inserts a repetition of
   * PV2-33: "Expected Surgery Date and Time" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public TS insertExpectedSurgeryDateAndTime(int rep) throws HL7Exception {
    return (TS) super.insertRepetition(33, rep);
  }


  /**
   * Inserts a repetition of
   * PV2-33: "Expected Surgery Date and Time" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public TS insertPv233_ExpectedSurgeryDateAndTime(int rep) throws HL7Exception {
    return (TS) super.insertRepetition(33, rep);
  }


  /**
   * Removes a repetition of
   * PV2-33: "Expected Surgery Date and Time" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public TS removeExpectedSurgeryDateAndTime(int rep) throws HL7Exception {
    return (TS) super.removeRepetition(33, rep);
  }


  /**
   * Removes a repetition of
   * PV2-33: "Expected Surgery Date and Time" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public TS removePv233_ExpectedSurgeryDateAndTime(int rep) throws HL7Exception {
    return (TS) super.removeRepetition(33, rep);
  }


  /**
   * Returns all repetitions of Military Partnership Code (PV2-34).
   */
  public ID[] getMilitaryPartnershipCode() {
    ID[] retVal = this.getTypedField(34, new ID[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Military Partnership Code (PV2-34).
   */
  public ID[] getPv234_MilitaryPartnershipCode() {
    ID[] retVal = this.getTypedField(34, new ID[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Military Partnership Code (PV2-34).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getMilitaryPartnershipCodeReps() {
    return this.getReps(34);
  }


  /**
   * Returns a specific repetition of
   * PV2-34: "Military Partnership Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public ID getMilitaryPartnershipCode(int rep) {
    ID retVal = this.getTypedField(34, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PV2-34: "Military Partnership Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public ID getPv234_MilitaryPartnershipCode(int rep) {
    ID retVal = this.getTypedField(34, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Military Partnership Code (PV2-34).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPv234_MilitaryPartnershipCodeReps() {
    return this.getReps(34);
  }


  /**
   * Inserts a repetition of
   * PV2-34: "Military Partnership Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID insertMilitaryPartnershipCode(int rep) throws HL7Exception {
    return (ID) super.insertRepetition(34, rep);
  }


  /**
   * Inserts a repetition of
   * PV2-34: "Military Partnership Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID insertPv234_MilitaryPartnershipCode(int rep) throws HL7Exception {
    return (ID) super.insertRepetition(34, rep);
  }


  /**
   * Removes a repetition of
   * PV2-34: "Military Partnership Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID removeMilitaryPartnershipCode(int rep) throws HL7Exception {
    return (ID) super.removeRepetition(34, rep);
  }


  /**
   * Removes a repetition of
   * PV2-34: "Military Partnership Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID removePv234_MilitaryPartnershipCode(int rep) throws HL7Exception {
    return (ID) super.removeRepetition(34, rep);
  }


  /**
   * Returns all repetitions of Military Non-Availability Code (PV2-35).
   */
  public ID[] getMilitaryNonAvailabilityCode() {
    ID[] retVal = this.getTypedField(35, new ID[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Military Non-Availability Code (PV2-35).
   */
  public ID[] getPv235_MilitaryNonAvailabilityCode() {
    ID[] retVal = this.getTypedField(35, new ID[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Military Non-Availability Code (PV2-35).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getMilitaryNonAvailabilityCodeReps() {
    return this.getReps(35);
  }


  /**
   * Returns a specific repetition of
   * PV2-35: "Military Non-Availability Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public ID getMilitaryNonAvailabilityCode(int rep) {
    ID retVal = this.getTypedField(35, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PV2-35: "Military Non-Availability Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public ID getPv235_MilitaryNonAvailabilityCode(int rep) {
    ID retVal = this.getTypedField(35, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Military Non-Availability Code (PV2-35).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPv235_MilitaryNonAvailabilityCodeReps() {
    return this.getReps(35);
  }


  /**
   * Inserts a repetition of
   * PV2-35: "Military Non-Availability Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID insertMilitaryNonAvailabilityCode(int rep) throws HL7Exception {
    return (ID) super.insertRepetition(35, rep);
  }


  /**
   * Inserts a repetition of
   * PV2-35: "Military Non-Availability Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID insertPv235_MilitaryNonAvailabilityCode(int rep) throws HL7Exception {
    return (ID) super.insertRepetition(35, rep);
  }


  /**
   * Removes a repetition of
   * PV2-35: "Military Non-Availability Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID removeMilitaryNonAvailabilityCode(int rep) throws HL7Exception {
    return (ID) super.removeRepetition(35, rep);
  }


  /**
   * Removes a repetition of
   * PV2-35: "Military Non-Availability Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID removePv235_MilitaryNonAvailabilityCode(int rep) throws HL7Exception {
    return (ID) super.removeRepetition(35, rep);
  }


  /**
   * Returns all repetitions of Newborn Baby Indicator (PV2-36).
   */
  public ID[] getNewbornBabyIndicator() {
    ID[] retVal = this.getTypedField(36, new ID[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Newborn Baby Indicator (PV2-36).
   */
  public ID[] getPv236_NewbornBabyIndicator() {
    ID[] retVal = this.getTypedField(36, new ID[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Newborn Baby Indicator (PV2-36).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getNewbornBabyIndicatorReps() {
    return this.getReps(36);
  }


  /**
   * Returns a specific repetition of
   * PV2-36: "Newborn Baby Indicator" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public ID getNewbornBabyIndicator(int rep) {
    ID retVal = this.getTypedField(36, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PV2-36: "Newborn Baby Indicator" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public ID getPv236_NewbornBabyIndicator(int rep) {
    ID retVal = this.getTypedField(36, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Newborn Baby Indicator (PV2-36).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPv236_NewbornBabyIndicatorReps() {
    return this.getReps(36);
  }


  /**
   * Inserts a repetition of
   * PV2-36: "Newborn Baby Indicator" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID insertNewbornBabyIndicator(int rep) throws HL7Exception {
    return (ID) super.insertRepetition(36, rep);
  }


  /**
   * Inserts a repetition of
   * PV2-36: "Newborn Baby Indicator" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID insertPv236_NewbornBabyIndicator(int rep) throws HL7Exception {
    return (ID) super.insertRepetition(36, rep);
  }


  /**
   * Removes a repetition of
   * PV2-36: "Newborn Baby Indicator" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID removeNewbornBabyIndicator(int rep) throws HL7Exception {
    return (ID) super.removeRepetition(36, rep);
  }


  /**
   * Removes a repetition of
   * PV2-36: "Newborn Baby Indicator" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID removePv236_NewbornBabyIndicator(int rep) throws HL7Exception {
    return (ID) super.removeRepetition(36, rep);
  }


  /**
   * Returns all repetitions of Baby Detained Indicator (PV2-37).
   */
  public ID[] getBabyDetainedIndicator() {
    ID[] retVal = this.getTypedField(37, new ID[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Baby Detained Indicator (PV2-37).
   */
  public ID[] getPv237_BabyDetainedIndicator() {
    ID[] retVal = this.getTypedField(37, new ID[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Baby Detained Indicator (PV2-37).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getBabyDetainedIndicatorReps() {
    return this.getReps(37);
  }


  /**
   * Returns a specific repetition of
   * PV2-37: "Baby Detained Indicator" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public ID getBabyDetainedIndicator(int rep) {
    ID retVal = this.getTypedField(37, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PV2-37: "Baby Detained Indicator" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public ID getPv237_BabyDetainedIndicator(int rep) {
    ID retVal = this.getTypedField(37, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Baby Detained Indicator (PV2-37).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPv237_BabyDetainedIndicatorReps() {
    return this.getReps(37);
  }


  /**
   * Inserts a repetition of
   * PV2-37: "Baby Detained Indicator" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID insertBabyDetainedIndicator(int rep) throws HL7Exception {
    return (ID) super.insertRepetition(37, rep);
  }


  /**
   * Inserts a repetition of
   * PV2-37: "Baby Detained Indicator" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID insertPv237_BabyDetainedIndicator(int rep) throws HL7Exception {
    return (ID) super.insertRepetition(37, rep);
  }


  /**
   * Removes a repetition of
   * PV2-37: "Baby Detained Indicator" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID removeBabyDetainedIndicator(int rep) throws HL7Exception {
    return (ID) super.removeRepetition(37, rep);
  }


  /**
   * Removes a repetition of
   * PV2-37: "Baby Detained Indicator" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID removePv237_BabyDetainedIndicator(int rep) throws HL7Exception {
    return (ID) super.removeRepetition(37, rep);
  }


  /**
   * Returns all repetitions of Mode of Arrival Code (PV2-38).
   */
  public CWE[] getModeOfArrivalCode() {
    CWE[] retVal = this.getTypedField(38, new CWE[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Mode of Arrival Code (PV2-38).
   */
  public CWE[] getPv238_ModeOfArrivalCode() {
    CWE[] retVal = this.getTypedField(38, new CWE[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Mode of Arrival Code (PV2-38).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getModeOfArrivalCodeReps() {
    return this.getReps(38);
  }


  /**
   * Returns a specific repetition of
   * PV2-38: "Mode of Arrival Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getModeOfArrivalCode(int rep) {
    CWE retVal = this.getTypedField(38, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PV2-38: "Mode of Arrival Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getPv238_ModeOfArrivalCode(int rep) {
    CWE retVal = this.getTypedField(38, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Mode of Arrival Code (PV2-38).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPv238_ModeOfArrivalCodeReps() {
    return this.getReps(38);
  }


  /**
   * Inserts a repetition of
   * PV2-38: "Mode of Arrival Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertModeOfArrivalCode(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(38, rep);
  }


  /**
   * Inserts a repetition of
   * PV2-38: "Mode of Arrival Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertPv238_ModeOfArrivalCode(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(38, rep);
  }


  /**
   * Removes a repetition of
   * PV2-38: "Mode of Arrival Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeModeOfArrivalCode(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(38, rep);
  }


  /**
   * Removes a repetition of
   * PV2-38: "Mode of Arrival Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removePv238_ModeOfArrivalCode(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(38, rep);
  }


  /**
   * Returns all repetitions of Recreational Drug Use Code (PV2-39).
   */
  public CWE[] getRecreationalDrugUseCode() {
    CWE[] retVal = this.getTypedField(39, new CWE[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Recreational Drug Use Code (PV2-39).
   */
  public CWE[] getPv239_RecreationalDrugUseCode() {
    CWE[] retVal = this.getTypedField(39, new CWE[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Recreational Drug Use Code (PV2-39).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getRecreationalDrugUseCodeReps() {
    return this.getReps(39);
  }


  /**
   * Returns a specific repetition of
   * PV2-39: "Recreational Drug Use Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getRecreationalDrugUseCode(int rep) {
    CWE retVal = this.getTypedField(39, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PV2-39: "Recreational Drug Use Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getPv239_RecreationalDrugUseCode(int rep) {
    CWE retVal = this.getTypedField(39, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Recreational Drug Use Code (PV2-39).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPv239_RecreationalDrugUseCodeReps() {
    return this.getReps(39);
  }


  /**
   * Inserts a repetition of
   * PV2-39: "Recreational Drug Use Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertRecreationalDrugUseCode(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(39, rep);
  }


  /**
   * Inserts a repetition of
   * PV2-39: "Recreational Drug Use Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertPv239_RecreationalDrugUseCode(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(39, rep);
  }


  /**
   * Removes a repetition of
   * PV2-39: "Recreational Drug Use Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeRecreationalDrugUseCode(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(39, rep);
  }


  /**
   * Removes a repetition of
   * PV2-39: "Recreational Drug Use Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removePv239_RecreationalDrugUseCode(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(39, rep);
  }


  /**
   * Returns
   * PV2-40: "Admission Level of Care Code" - creates it if necessary
   */
  public CWE getAdmissionLevelOfCareCode() {
    CWE retVal = this.getTypedField(40, 0);
    return retVal;
  }

  /**
   * Returns
   * PV2-40: "Admission Level of Care Code" - creates it if necessary
   */
  public CWE getPv240_AdmissionLevelOfCareCode() {
    CWE retVal = this.getTypedField(40, 0);
    return retVal;
  }


  /**
   * Returns all repetitions of Precaution Code (PV2-41).
   */
  public CWE[] getPrecautionCode() {
    CWE[] retVal = this.getTypedField(41, new CWE[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Precaution Code (PV2-41).
   */
  public CWE[] getPv241_PrecautionCode() {
    CWE[] retVal = this.getTypedField(41, new CWE[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Precaution Code (PV2-41).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPrecautionCodeReps() {
    return this.getReps(41);
  }


  /**
   * Returns a specific repetition of
   * PV2-41: "Precaution Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getPrecautionCode(int rep) {
    CWE retVal = this.getTypedField(41, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PV2-41: "Precaution Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getPv241_PrecautionCode(int rep) {
    CWE retVal = this.getTypedField(41, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Precaution Code (PV2-41).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPv241_PrecautionCodeReps() {
    return this.getReps(41);
  }


  /**
   * Inserts a repetition of
   * PV2-41: "Precaution Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertPrecautionCode(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(41, rep);
  }


  /**
   * Inserts a repetition of
   * PV2-41: "Precaution Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertPv241_PrecautionCode(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(41, rep);
  }


  /**
   * Removes a repetition of
   * PV2-41: "Precaution Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removePrecautionCode(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(41, rep);
  }


  /**
   * Removes a repetition of
   * PV2-41: "Precaution Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removePv241_PrecautionCode(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(41, rep);
  }


  /**
   * Returns all repetitions of Patient Condition Code (PV2-42).
   */
  public CWE[] getPatientConditionCode() {
    CWE[] retVal = this.getTypedField(42, new CWE[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Patient Condition Code (PV2-42).
   */
  public CWE[] getPv242_PatientConditionCode() {
    CWE[] retVal = this.getTypedField(42, new CWE[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Patient Condition Code (PV2-42).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPatientConditionCodeReps() {
    return this.getReps(42);
  }


  /**
   * Returns a specific repetition of
   * PV2-42: "Patient Condition Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getPatientConditionCode(int rep) {
    CWE retVal = this.getTypedField(42, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PV2-42: "Patient Condition Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getPv242_PatientConditionCode(int rep) {
    CWE retVal = this.getTypedField(42, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Patient Condition Code (PV2-42).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPv242_PatientConditionCodeReps() {
    return this.getReps(42);
  }


  /**
   * Inserts a repetition of
   * PV2-42: "Patient Condition Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertPatientConditionCode(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(42, rep);
  }


  /**
   * Inserts a repetition of
   * PV2-42: "Patient Condition Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertPv242_PatientConditionCode(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(42, rep);
  }


  /**
   * Removes a repetition of
   * PV2-42: "Patient Condition Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removePatientConditionCode(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(42, rep);
  }


  /**
   * Removes a repetition of
   * PV2-42: "Patient Condition Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removePv242_PatientConditionCode(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(42, rep);
  }


  /**
   * Returns all repetitions of Living Will Code (PV2-43).
   */
  public IS[] getLivingWillCode() {
    IS[] retVal = this.getTypedField(43, new IS[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Living Will Code (PV2-43).
   */
  public IS[] getPv243_LivingWillCode() {
    IS[] retVal = this.getTypedField(43, new IS[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Living Will Code (PV2-43).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getLivingWillCodeReps() {
    return this.getReps(43);
  }


  /**
   * Returns a specific repetition of
   * PV2-43: "Living Will Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public IS getLivingWillCode(int rep) {
    IS retVal = this.getTypedField(43, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PV2-43: "Living Will Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public IS getPv243_LivingWillCode(int rep) {
    IS retVal = this.getTypedField(43, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Living Will Code (PV2-43).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPv243_LivingWillCodeReps() {
    return this.getReps(43);
  }


  /**
   * Inserts a repetition of
   * PV2-43: "Living Will Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS insertLivingWillCode(int rep) throws HL7Exception {
    return (IS) super.insertRepetition(43, rep);
  }


  /**
   * Inserts a repetition of
   * PV2-43: "Living Will Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS insertPv243_LivingWillCode(int rep) throws HL7Exception {
    return (IS) super.insertRepetition(43, rep);
  }


  /**
   * Removes a repetition of
   * PV2-43: "Living Will Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS removeLivingWillCode(int rep) throws HL7Exception {
    return (IS) super.removeRepetition(43, rep);
  }


  /**
   * Removes a repetition of
   * PV2-43: "Living Will Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS removePv243_LivingWillCode(int rep) throws HL7Exception {
    return (IS) super.removeRepetition(43, rep);
  }


  /**
   * Returns all repetitions of Organ Donor Code (PV2-44).
   */
  public IS[] getOrganDonorCode() {
    IS[] retVal = this.getTypedField(44, new IS[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Organ Donor Code (PV2-44).
   */
  public IS[] getPv244_OrganDonorCode() {
    IS[] retVal = this.getTypedField(44, new IS[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Organ Donor Code (PV2-44).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getOrganDonorCodeReps() {
    return this.getReps(44);
  }


  /**
   * Returns a specific repetition of
   * PV2-44: "Organ Donor Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public IS getOrganDonorCode(int rep) {
    IS retVal = this.getTypedField(44, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PV2-44: "Organ Donor Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public IS getPv244_OrganDonorCode(int rep) {
    IS retVal = this.getTypedField(44, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Organ Donor Code (PV2-44).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPv244_OrganDonorCodeReps() {
    return this.getReps(44);
  }


  /**
   * Inserts a repetition of
   * PV2-44: "Organ Donor Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS insertOrganDonorCode(int rep) throws HL7Exception {
    return (IS) super.insertRepetition(44, rep);
  }


  /**
   * Inserts a repetition of
   * PV2-44: "Organ Donor Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS insertPv244_OrganDonorCode(int rep) throws HL7Exception {
    return (IS) super.insertRepetition(44, rep);
  }


  /**
   * Removes a repetition of
   * PV2-44: "Organ Donor Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS removeOrganDonorCode(int rep) throws HL7Exception {
    return (IS) super.removeRepetition(44, rep);
  }


  /**
   * Removes a repetition of
   * PV2-44: "Organ Donor Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS removePv244_OrganDonorCode(int rep) throws HL7Exception {
    return (IS) super.removeRepetition(44, rep);
  }


  /**
   * Returns all repetitions of Advance Directive Code (PV2-45).
   */
  public CWE[] getAdvanceDirectiveCode() {
    CWE[] retVal = this.getTypedField(45, new CWE[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Advance Directive Code (PV2-45).
   */
  public CWE[] getPv245_AdvanceDirectiveCode() {
    CWE[] retVal = this.getTypedField(45, new CWE[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Advance Directive Code (PV2-45).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getAdvanceDirectiveCodeReps() {
    return this.getReps(45);
  }


  /**
   * Returns a specific repetition of
   * PV2-45: "Advance Directive Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getAdvanceDirectiveCode(int rep) {
    CWE retVal = this.getTypedField(45, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PV2-45: "Advance Directive Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getPv245_AdvanceDirectiveCode(int rep) {
    CWE retVal = this.getTypedField(45, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Advance Directive Code (PV2-45).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPv245_AdvanceDirectiveCodeReps() {
    return this.getReps(45);
  }


  /**
   * Inserts a repetition of
   * PV2-45: "Advance Directive Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertAdvanceDirectiveCode(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(45, rep);
  }


  /**
   * Inserts a repetition of
   * PV2-45: "Advance Directive Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertPv245_AdvanceDirectiveCode(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(45, rep);
  }


  /**
   * Removes a repetition of
   * PV2-45: "Advance Directive Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeAdvanceDirectiveCode(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(45, rep);
  }


  /**
   * Removes a repetition of
   * PV2-45: "Advance Directive Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removePv245_AdvanceDirectiveCode(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(45, rep);
  }


  /**
   * Returns all repetitions of Patient Status Effective Date (PV2-46).
   */
  public DT[] getPatientStatusEffectiveDate() {
    DT[] retVal = this.getTypedField(46, new DT[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Patient Status Effective Date (PV2-46).
   */
  public DT[] getPv246_PatientStatusEffectiveDate() {
    DT[] retVal = this.getTypedField(46, new DT[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Patient Status Effective Date (PV2-46).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPatientStatusEffectiveDateReps() {
    return this.getReps(46);
  }


  /**
   * Returns a specific repetition of
   * PV2-46: "Patient Status Effective Date" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public DT getPatientStatusEffectiveDate(int rep) {
    DT retVal = this.getTypedField(46, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PV2-46: "Patient Status Effective Date" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public DT getPv246_PatientStatusEffectiveDate(int rep) {
    DT retVal = this.getTypedField(46, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Patient Status Effective Date (PV2-46).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPv246_PatientStatusEffectiveDateReps() {
    return this.getReps(46);
  }


  /**
   * Inserts a repetition of
   * PV2-46: "Patient Status Effective Date" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public DT insertPatientStatusEffectiveDate(int rep) throws HL7Exception {
    return (DT) super.insertRepetition(46, rep);
  }


  /**
   * Inserts a repetition of
   * PV2-46: "Patient Status Effective Date" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public DT insertPv246_PatientStatusEffectiveDate(int rep) throws HL7Exception {
    return (DT) super.insertRepetition(46, rep);
  }


  /**
   * Removes a repetition of
   * PV2-46: "Patient Status Effective Date" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public DT removePatientStatusEffectiveDate(int rep) throws HL7Exception {
    return (DT) super.removeRepetition(46, rep);
  }


  /**
   * Removes a repetition of
   * PV2-46: "Patient Status Effective Date" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public DT removePv246_PatientStatusEffectiveDate(int rep) throws HL7Exception {
    return (DT) super.removeRepetition(46, rep);
  }


  /**
   * Returns all repetitions of Expected LOA Return Date/Time (PV2-47).
   */
  public TS[] getExpectedLOAReturnDateTime() {
    TS[] retVal = this.getTypedField(47, new TS[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Expected LOA Return Date/Time (PV2-47).
   */
  public TS[] getPv247_ExpectedLOAReturnDateTime() {
    TS[] retVal = this.getTypedField(47, new TS[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Expected LOA Return Date/Time (PV2-47).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getExpectedLOAReturnDateTimeReps() {
    return this.getReps(47);
  }


  /**
   * Returns a specific repetition of
   * PV2-47: "Expected LOA Return Date/Time" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public TS getExpectedLOAReturnDateTime(int rep) {
    TS retVal = this.getTypedField(47, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PV2-47: "Expected LOA Return Date/Time" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public TS getPv247_ExpectedLOAReturnDateTime(int rep) {
    TS retVal = this.getTypedField(47, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Expected LOA Return Date/Time (PV2-47).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPv247_ExpectedLOAReturnDateTimeReps() {
    return this.getReps(47);
  }


  /**
   * Inserts a repetition of
   * PV2-47: "Expected LOA Return Date/Time" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public TS insertExpectedLOAReturnDateTime(int rep) throws HL7Exception {
    return (TS) super.insertRepetition(47, rep);
  }


  /**
   * Inserts a repetition of
   * PV2-47: "Expected LOA Return Date/Time" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public TS insertPv247_ExpectedLOAReturnDateTime(int rep) throws HL7Exception {
    return (TS) super.insertRepetition(47, rep);
  }


  /**
   * Removes a repetition of
   * PV2-47: "Expected LOA Return Date/Time" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public TS removeExpectedLOAReturnDateTime(int rep) throws HL7Exception {
    return (TS) super.removeRepetition(47, rep);
  }


  /**
   * Removes a repetition of
   * PV2-47: "Expected LOA Return Date/Time" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public TS removePv247_ExpectedLOAReturnDateTime(int rep) throws HL7Exception {
    return (TS) super.removeRepetition(47, rep);
  }


  /**
   * Returns all repetitions of Expected Pre-admission Testing Date/Time (PV2-48).
   */
  public TS[] getExpectedPreAdmissionTestingDateTime() {
    TS[] retVal = this.getTypedField(48, new TS[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Expected Pre-admission Testing Date/Time (PV2-48).
   */
  public TS[] getPv248_ExpectedPreAdmissionTestingDateTime() {
    TS[] retVal = this.getTypedField(48, new TS[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Expected Pre-admission Testing Date/Time (PV2-48).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getExpectedPreAdmissionTestingDateTimeReps() {
    return this.getReps(48);
  }


  /**
   * Returns a specific repetition of
   * PV2-48: "Expected Pre-admission Testing Date/Time" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public TS getExpectedPreAdmissionTestingDateTime(int rep) {
    TS retVal = this.getTypedField(48, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PV2-48: "Expected Pre-admission Testing Date/Time" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public TS getPv248_ExpectedPreAdmissionTestingDateTime(int rep) {
    TS retVal = this.getTypedField(48, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Expected Pre-admission Testing Date/Time (PV2-48).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPv248_ExpectedPreAdmissionTestingDateTimeReps() {
    return this.getReps(48);
  }


  /**
   * Inserts a repetition of
   * PV2-48: "Expected Pre-admission Testing Date/Time" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public TS insertExpectedPreAdmissionTestingDateTime(int rep) throws HL7Exception {
    return (TS) super.insertRepetition(48, rep);
  }


  /**
   * Inserts a repetition of
   * PV2-48: "Expected Pre-admission Testing Date/Time" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public TS insertPv248_ExpectedPreAdmissionTestingDateTime(int rep) throws HL7Exception {
    return (TS) super.insertRepetition(48, rep);
  }


  /**
   * Removes a repetition of
   * PV2-48: "Expected Pre-admission Testing Date/Time" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public TS removeExpectedPreAdmissionTestingDateTime(int rep) throws HL7Exception {
    return (TS) super.removeRepetition(48, rep);
  }


  /**
   * Removes a repetition of
   * PV2-48: "Expected Pre-admission Testing Date/Time" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public TS removePv248_ExpectedPreAdmissionTestingDateTime(int rep) throws HL7Exception {
    return (TS) super.removeRepetition(48, rep);
  }


  /**
   * Returns all repetitions of Notify Clergy Code (PV2-49).
   */
  public IS[] getNotifyClergyCode() {
    IS[] retVal = this.getTypedField(49, new IS[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Notify Clergy Code (PV2-49).
   */
  public IS[] getPv249_NotifyClergyCode() {
    IS[] retVal = this.getTypedField(49, new IS[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Notify Clergy Code (PV2-49).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getNotifyClergyCodeReps() {
    return this.getReps(49);
  }


  /**
   * Returns a specific repetition of
   * PV2-49: "Notify Clergy Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public IS getNotifyClergyCode(int rep) {
    IS retVal = this.getTypedField(49, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * PV2-49: "Notify Clergy Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public IS getPv249_NotifyClergyCode(int rep) {
    IS retVal = this.getTypedField(49, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Notify Clergy Code (PV2-49).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPv249_NotifyClergyCodeReps() {
    return this.getReps(49);
  }


  /**
   * Inserts a repetition of
   * PV2-49: "Notify Clergy Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS insertNotifyClergyCode(int rep) throws HL7Exception {
    return (IS) super.insertRepetition(49, rep);
  }


  /**
   * Inserts a repetition of
   * PV2-49: "Notify Clergy Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS insertPv249_NotifyClergyCode(int rep) throws HL7Exception {
    return (IS) super.insertRepetition(49, rep);
  }


  /**
   * Removes a repetition of
   * PV2-49: "Notify Clergy Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS removeNotifyClergyCode(int rep) throws HL7Exception {
    return (IS) super.removeRepetition(49, rep);
  }


  /**
   * Removes a repetition of
   * PV2-49: "Notify Clergy Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS removePv249_NotifyClergyCode(int rep) throws HL7Exception {
    return (IS) super.removeRepetition(49, rep);
  }


  /**
   * {@inheritDoc}
   */
  protected Type createNewTypeWithoutReflection(int field) {
    switch (field) {
      case 0:
        return new PL(getMessage());
      case 1:
        return new CWE(getMessage());
      case 2:
        return new CWE(getMessage());
      case 3:
        return new CWE(getMessage());
      case 4:
        return new ST(getMessage());
      case 5:
        return new ST(getMessage());
      case 6:
        return new IS(getMessage(), 0);
      case 7:
        return new TS(getMessage());
      case 8:
        return new TS(getMessage());
      case 9:
        return new NM(getMessage());
      case 10:
        return new NM(getMessage());
      case 11:
        return new ST(getMessage());
      case 12:
        return new XCN(getMessage());
      case 13:
        return new DT(getMessage());
      case 14:
        return new ID(getMessage(), 136);
      case 15:
        return new IS(getMessage(), 0);
      case 16:
        return new DT(getMessage());
      case 17:
        return new IS(getMessage(), 0);
      case 18:
        return new ID(getMessage(), 0);
      case 19:
        return new NM(getMessage());
      case 20:
        return new IS(getMessage(), 0);
      case 21:
        return new ID(getMessage(), 0);
      case 22:
        return new XON(getMessage());
      case 23:
        return new IS(getMessage(), 0);
      case 24:
        return new IS(getMessage(), 0);
      case 25:
        return new DT(getMessage());
      case 26:
        return new IS(getMessage(), 0);
      case 27:
        return new DT(getMessage());
      case 28:
        return new DT(getMessage());
      case 29:
        return new CWE(getMessage());
      case 30:
        return new IS(getMessage(), 0);
      case 31:
        return new ID(getMessage(), 0);
      case 32:
        return new TS(getMessage());
      case 33:
        return new ID(getMessage(), 0);
      case 34:
        return new ID(getMessage(), 0);
      case 35:
        return new ID(getMessage(), 0);
      case 36:
        return new ID(getMessage(), 0);
      case 37:
        return new CWE(getMessage());
      case 38:
        return new CWE(getMessage());
      case 39:
        return new CWE(getMessage());
      case 40:
        return new CWE(getMessage());
      case 41:
        return new CWE(getMessage());
      case 42:
        return new IS(getMessage(), 0);
      case 43:
        return new IS(getMessage(), 0);
      case 44:
        return new CWE(getMessage());
      case 45:
        return new DT(getMessage());
      case 46:
        return new TS(getMessage());
      case 47:
        return new TS(getMessage());
      case 48:
        return new IS(getMessage(), 0);
      default:
        return null;
    }
  }


}

