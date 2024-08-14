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
import fhirengine.translation.hl7.structures.nistelr251.datatype.CWE_ELR;
import fhirengine.translation.hl7.structures.nistelr251.datatype.XPN_ELR;


/**
 * <p>Represents an HL7 NK1 message segment (Next of Kin / Associated Parties).
 * This segment has the following fields:</p>
 * <ul>
 * <li>NK1-1: Set ID - NK1 (SI) <b> </b>
 * <li>NK1-2: Name (XPN_ELR) <b>optional repeating</b>
 * <li>NK1-3: Relationship (CWE_ELR) <b>optional </b>
 * <li>NK1-4: Address (XAD) <b>optional repeating</b>
 * <li>NK1-5: Phone Number (XTN) <b>optional repeating</b>
 * <li>NK1-6: Business Phone Number (XTN) <b>optional repeating</b>
 * <li>NK1-7: Contact Role (CWE) <b>optional repeating</b>
 * <li>NK1-8: Start Date (DT) <b>optional repeating</b>
 * <li>NK1-9: End Date (DT) <b>optional repeating</b>
 * <li>NK1-10: Next of Kin / Associated Parties Job Title (ST) <b>optional repeating</b>
 * <li>NK1-11: Next of Kin / Associated Parties Job Code/Class (JCC) <b>optional repeating</b>
 * <li>NK1-12: Next of Kin / Associated Parties Employee Number (CX) <b>optional repeating</b>
 * <li>NK1-13: Organization Name - NK1 (XON) <b>optional </b>
 * <li>NK1-14: Marital Status (CWE) <b>optional repeating</b>
 * <li>NK1-15: Administrative Sex (IS) <b>optional repeating</b>
 * <li>NK1-16: Date/Time of Birth (TS) <b>optional repeating</b>
 * <li>NK1-17: Living Dependency (IS) <b>optional repeating</b>
 * <li>NK1-18: Ambulatory Status (IS) <b>optional repeating</b>
 * <li>NK1-19: Citizenship (CWE) <b>optional repeating</b>
 * <li>NK1-20: Primary Language (CWE) <b>optional </b>
 * <li>NK1-21: Living Arrangement (IS) <b>optional repeating</b>
 * <li>NK1-22: Publicity Code (CWE) <b>optional repeating</b>
 * <li>NK1-23: Protection Indicator (ID) <b>optional repeating</b>
 * <li>NK1-24: Student Indicator (IS) <b>optional repeating</b>
 * <li>NK1-25: Religion (CWE) <b>optional repeating</b>
 * <li>NK1-26: Mother's Maiden Name (XPN) <b>optional repeating</b>
 * <li>NK1-27: Nationality (CWE) <b>optional repeating</b>
 * <li>NK1-28: Ethnic Group (CWE) <b>optional repeating</b>
 * <li>NK1-29: Contact Reason (CWE) <b>optional repeating</b>
 * <li>NK1-30: Contact Person's Name (XPN_ELR) <b>optional repeating</b>
 * <li>NK1-31: Contact Person's Telephone Number (XTN) <b>optional repeating</b>
 * <li>NK1-32: Contact Person's Address (XAD) <b>optional repeating</b>
 * <li>NK1-33: Next of Kin/Associated Party's Identifiers (CX) <b>optional repeating</b>
 * <li>NK1-34: Job Status (IS) <b>optional repeating</b>
 * <li>NK1-35: Race (CWE) <b>optional repeating</b>
 * <li>NK1-36: Handicap (IS) <b>optional repeating</b>
 * <li>NK1-37: Contact Person Social Security Number (ST) <b>optional repeating</b>
 * <li>NK1-38: Next of Kin Birth Place (ST) <b>optional repeating</b>
 * <li>NK1-39: VIP Indicator (IS) <b>optional repeating</b>
 * </ul>
 */
@SuppressWarnings("unused")
public class NK1 extends AbstractSegment {

  /**
   * Creates a new NK1 segment
   */
  public NK1(Group parent, ModelClassFactory factory) {
    super(parent, factory);
    init(factory);
  }

  private void init(ModelClassFactory factory) {
    try {
      this.add(SI.class, true, 1, 0, new Object[]{getMessage()}, "Set ID - NK1");
      this.add(XPN_ELR.class, false, -1, 0, new Object[]{getMessage()}, "Name");
      this.add(CWE_ELR.class, false, 1, 0, new Object[]{getMessage()}, "Relationship");
      this.add(XAD.class, false, -1, 0, new Object[]{getMessage()}, "Address");
      this.add(XTN.class, false, -1, 0, new Object[]{getMessage()}, "Phone Number");
      this.add(XTN.class, false, 0, 0, new Object[]{getMessage()}, "Business Phone Number");
      this.add(CWE.class, false, 0, 0, new Object[]{getMessage()}, "Contact Role");
      this.add(DT.class, false, 0, 0, new Object[]{getMessage()}, "Start Date");
      this.add(DT.class, false, 0, 0, new Object[]{getMessage()}, "End Date");
      this.add(ST.class, false, 0, 0, new Object[]{getMessage()}, "Next of Kin / Associated Parties Job Title");
      this.add(JCC.class, false, 0, 0, new Object[]{getMessage()}, "Next of Kin / Associated Parties Job Code/Class");
      this.add(CX.class, false, 0, 0, new Object[]{getMessage()}, "Next of Kin / Associated Parties Employee Number");
      this.add(XON.class, false, 1, 0, new Object[]{getMessage()}, "Organization Name - NK1");
      this.add(CWE.class, false, 0, 0, new Object[]{getMessage()}, "Marital Status");
      this.add(IS.class, false, 0, 0, new Object[]{getMessage(), Integer.valueOf(0)}, "Administrative Sex");
      this.add(TS.class, false, 0, 0, new Object[]{getMessage()}, "Date/Time of Birth");
      this.add(IS.class, false, 0, 0, new Object[]{getMessage(), Integer.valueOf(0)}, "Living Dependency");
      this.add(IS.class, false, 0, 0, new Object[]{getMessage(), Integer.valueOf(0)}, "Ambulatory Status");
      this.add(CWE.class, false, 0, 0, new Object[]{getMessage()}, "Citizenship");
      this.add(CWE.class, false, 1, 0, new Object[]{getMessage()}, "Primary Language");
      this.add(IS.class, false, 0, 0, new Object[]{getMessage(), Integer.valueOf(0)}, "Living Arrangement");
      this.add(CWE.class, false, 0, 0, new Object[]{getMessage()}, "Publicity Code");
      this.add(ID.class, false, 0, 0, new Object[]{getMessage(), Integer.valueOf(0)}, "Protection Indicator");
      this.add(IS.class, false, 0, 0, new Object[]{getMessage(), Integer.valueOf(0)}, "Student Indicator");
      this.add(CWE.class, false, 0, 0, new Object[]{getMessage()}, "Religion");
      this.add(XPN.class, false, 0, 0, new Object[]{getMessage()}, "Mother's Maiden Name");
      this.add(CWE.class, false, 0, 0, new Object[]{getMessage()}, "Nationality");
      this.add(CWE.class, false, 0, 0, new Object[]{getMessage()}, "Ethnic Group");
      this.add(CWE.class, false, 0, 0, new Object[]{getMessage()}, "Contact Reason");
      this.add(XPN_ELR.class, false, -1, 0, new Object[]{getMessage()}, "Contact Person's Name");
      this.add(XTN.class, false, -1, 0, new Object[]{getMessage()}, "Contact Person's Telephone Number");
      this.add(XAD.class, false, -1, 0, new Object[]{getMessage()}, "Contact Person's Address");
      this.add(CX.class, false, 0, 0, new Object[]{getMessage()}, "Next of Kin/Associated Party's Identifiers");
      this.add(IS.class, false, 0, 0, new Object[]{getMessage(), Integer.valueOf(0)}, "Job Status");
      this.add(CWE.class, false, 0, 0, new Object[]{getMessage()}, "Race");
      this.add(IS.class, false, 0, 0, new Object[]{getMessage(), Integer.valueOf(0)}, "Handicap");
      this.add(ST.class, false, 0, 0, new Object[]{getMessage()}, "Contact Person Social Security Number");
      this.add(ST.class, false, 0, 0, new Object[]{getMessage()}, "Next of Kin Birth Place");
      this.add(IS.class, false, 0, 0, new Object[]{getMessage(), Integer.valueOf(0)}, "VIP Indicator");
    } catch (HL7Exception e) {
      log.error("Unexpected error creating NK1 - this is probably a bug in the source code generator.", e);
    }
  }


  /**
   * Returns
   * NK1-1: "Set ID - NK1" - creates it if necessary
   */
  public SI getSetIDNK1() {
    SI retVal = this.getTypedField(1, 0);
    return retVal;
  }

  /**
   * Returns
   * NK1-1: "Set ID - NK1" - creates it if necessary
   */
  public SI getNk11_SetIDNK1() {
    SI retVal = this.getTypedField(1, 0);
    return retVal;
  }


  /**
   * Returns all repetitions of Name (NK1-2).
   */
  public XPN_ELR[] getNK1Name() {
    XPN_ELR[] retVal = this.getTypedField(2, new XPN_ELR[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Name (NK1-2).
   */
  public XPN_ELR[] getNk12_Name() {
    XPN_ELR[] retVal = this.getTypedField(2, new XPN_ELR[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Name (NK1-2).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getNK1NameReps() {
    return this.getReps(2);
  }


  /**
   * Returns a specific repetition of
   * NK1-2: "Name" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XPN_ELR getNK1Name(int rep) {
    XPN_ELR retVal = this.getTypedField(2, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * NK1-2: "Name" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XPN_ELR getNk12_Name(int rep) {
    XPN_ELR retVal = this.getTypedField(2, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Name (NK1-2).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getNk12_NameReps() {
    return this.getReps(2);
  }


  /**
   * Inserts a repetition of
   * NK1-2: "Name" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XPN_ELR insertNK1Name(int rep) throws HL7Exception {
    return (XPN_ELR) super.insertRepetition(2, rep);
  }


  /**
   * Inserts a repetition of
   * NK1-2: "Name" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XPN_ELR insertNk12_Name(int rep) throws HL7Exception {
    return (XPN_ELR) super.insertRepetition(2, rep);
  }


  /**
   * Removes a repetition of
   * NK1-2: "Name" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XPN_ELR removeNK1Name(int rep) throws HL7Exception {
    return (XPN_ELR) super.removeRepetition(2, rep);
  }


  /**
   * Removes a repetition of
   * NK1-2: "Name" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XPN_ELR removeNk12_Name(int rep) throws HL7Exception {
    return (XPN_ELR) super.removeRepetition(2, rep);
  }


  /**
   * Returns
   * NK1-3: "Relationship" - creates it if necessary
   */
  public CWE_ELR getRelationship() {
    CWE_ELR retVal = this.getTypedField(3, 0);
    return retVal;
  }

  /**
   * Returns
   * NK1-3: "Relationship" - creates it if necessary
   */
  public CWE_ELR getNk13_Relationship() {
    CWE_ELR retVal = this.getTypedField(3, 0);
    return retVal;
  }


  /**
   * Returns all repetitions of Address (NK1-4).
   */
  public XAD[] getAddress() {
    XAD[] retVal = this.getTypedField(4, new XAD[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Address (NK1-4).
   */
  public XAD[] getNk14_Address() {
    XAD[] retVal = this.getTypedField(4, new XAD[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Address (NK1-4).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getAddressReps() {
    return this.getReps(4);
  }


  /**
   * Returns a specific repetition of
   * NK1-4: "Address" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XAD getAddress(int rep) {
    XAD retVal = this.getTypedField(4, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * NK1-4: "Address" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XAD getNk14_Address(int rep) {
    XAD retVal = this.getTypedField(4, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Address (NK1-4).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getNk14_AddressReps() {
    return this.getReps(4);
  }


  /**
   * Inserts a repetition of
   * NK1-4: "Address" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XAD insertAddress(int rep) throws HL7Exception {
    return (XAD) super.insertRepetition(4, rep);
  }


  /**
   * Inserts a repetition of
   * NK1-4: "Address" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XAD insertNk14_Address(int rep) throws HL7Exception {
    return (XAD) super.insertRepetition(4, rep);
  }


  /**
   * Removes a repetition of
   * NK1-4: "Address" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XAD removeAddress(int rep) throws HL7Exception {
    return (XAD) super.removeRepetition(4, rep);
  }


  /**
   * Removes a repetition of
   * NK1-4: "Address" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XAD removeNk14_Address(int rep) throws HL7Exception {
    return (XAD) super.removeRepetition(4, rep);
  }


  /**
   * Returns all repetitions of Phone Number (NK1-5).
   */
  public XTN[] getPhoneNumber() {
    XTN[] retVal = this.getTypedField(5, new XTN[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Phone Number (NK1-5).
   */
  public XTN[] getNk15_PhoneNumber() {
    XTN[] retVal = this.getTypedField(5, new XTN[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Phone Number (NK1-5).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPhoneNumberReps() {
    return this.getReps(5);
  }


  /**
   * Returns a specific repetition of
   * NK1-5: "Phone Number" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XTN getPhoneNumber(int rep) {
    XTN retVal = this.getTypedField(5, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * NK1-5: "Phone Number" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XTN getNk15_PhoneNumber(int rep) {
    XTN retVal = this.getTypedField(5, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Phone Number (NK1-5).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getNk15_PhoneNumberReps() {
    return this.getReps(5);
  }


  /**
   * Inserts a repetition of
   * NK1-5: "Phone Number" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XTN insertPhoneNumber(int rep) throws HL7Exception {
    return (XTN) super.insertRepetition(5, rep);
  }


  /**
   * Inserts a repetition of
   * NK1-5: "Phone Number" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XTN insertNk15_PhoneNumber(int rep) throws HL7Exception {
    return (XTN) super.insertRepetition(5, rep);
  }


  /**
   * Removes a repetition of
   * NK1-5: "Phone Number" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XTN removePhoneNumber(int rep) throws HL7Exception {
    return (XTN) super.removeRepetition(5, rep);
  }


  /**
   * Removes a repetition of
   * NK1-5: "Phone Number" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XTN removeNk15_PhoneNumber(int rep) throws HL7Exception {
    return (XTN) super.removeRepetition(5, rep);
  }


  /**
   * Returns all repetitions of Business Phone Number (NK1-6).
   */
  public XTN[] getBusinessPhoneNumber() {
    XTN[] retVal = this.getTypedField(6, new XTN[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Business Phone Number (NK1-6).
   */
  public XTN[] getNk16_BusinessPhoneNumber() {
    XTN[] retVal = this.getTypedField(6, new XTN[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Business Phone Number (NK1-6).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getBusinessPhoneNumberReps() {
    return this.getReps(6);
  }


  /**
   * Returns a specific repetition of
   * NK1-6: "Business Phone Number" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XTN getBusinessPhoneNumber(int rep) {
    XTN retVal = this.getTypedField(6, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * NK1-6: "Business Phone Number" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XTN getNk16_BusinessPhoneNumber(int rep) {
    XTN retVal = this.getTypedField(6, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Business Phone Number (NK1-6).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getNk16_BusinessPhoneNumberReps() {
    return this.getReps(6);
  }


  /**
   * Inserts a repetition of
   * NK1-6: "Business Phone Number" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XTN insertBusinessPhoneNumber(int rep) throws HL7Exception {
    return (XTN) super.insertRepetition(6, rep);
  }


  /**
   * Inserts a repetition of
   * NK1-6: "Business Phone Number" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XTN insertNk16_BusinessPhoneNumber(int rep) throws HL7Exception {
    return (XTN) super.insertRepetition(6, rep);
  }


  /**
   * Removes a repetition of
   * NK1-6: "Business Phone Number" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XTN removeBusinessPhoneNumber(int rep) throws HL7Exception {
    return (XTN) super.removeRepetition(6, rep);
  }


  /**
   * Removes a repetition of
   * NK1-6: "Business Phone Number" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XTN removeNk16_BusinessPhoneNumber(int rep) throws HL7Exception {
    return (XTN) super.removeRepetition(6, rep);
  }


  /**
   * Returns all repetitions of Contact Role (NK1-7).
   */
  public CWE[] getContactRole() {
    CWE[] retVal = this.getTypedField(7, new CWE[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Contact Role (NK1-7).
   */
  public CWE[] getNk17_ContactRole() {
    CWE[] retVal = this.getTypedField(7, new CWE[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Contact Role (NK1-7).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getContactRoleReps() {
    return this.getReps(7);
  }


  /**
   * Returns a specific repetition of
   * NK1-7: "Contact Role" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getContactRole(int rep) {
    CWE retVal = this.getTypedField(7, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * NK1-7: "Contact Role" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getNk17_ContactRole(int rep) {
    CWE retVal = this.getTypedField(7, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Contact Role (NK1-7).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getNk17_ContactRoleReps() {
    return this.getReps(7);
  }


  /**
   * Inserts a repetition of
   * NK1-7: "Contact Role" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertContactRole(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(7, rep);
  }


  /**
   * Inserts a repetition of
   * NK1-7: "Contact Role" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertNk17_ContactRole(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(7, rep);
  }


  /**
   * Removes a repetition of
   * NK1-7: "Contact Role" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeContactRole(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(7, rep);
  }


  /**
   * Removes a repetition of
   * NK1-7: "Contact Role" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeNk17_ContactRole(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(7, rep);
  }


  /**
   * Returns all repetitions of Start Date (NK1-8).
   */
  public DT[] getStartDate() {
    DT[] retVal = this.getTypedField(8, new DT[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Start Date (NK1-8).
   */
  public DT[] getNk18_StartDate() {
    DT[] retVal = this.getTypedField(8, new DT[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Start Date (NK1-8).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getStartDateReps() {
    return this.getReps(8);
  }


  /**
   * Returns a specific repetition of
   * NK1-8: "Start Date" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public DT getStartDate(int rep) {
    DT retVal = this.getTypedField(8, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * NK1-8: "Start Date" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public DT getNk18_StartDate(int rep) {
    DT retVal = this.getTypedField(8, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Start Date (NK1-8).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getNk18_StartDateReps() {
    return this.getReps(8);
  }


  /**
   * Inserts a repetition of
   * NK1-8: "Start Date" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public DT insertStartDate(int rep) throws HL7Exception {
    return (DT) super.insertRepetition(8, rep);
  }


  /**
   * Inserts a repetition of
   * NK1-8: "Start Date" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public DT insertNk18_StartDate(int rep) throws HL7Exception {
    return (DT) super.insertRepetition(8, rep);
  }


  /**
   * Removes a repetition of
   * NK1-8: "Start Date" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public DT removeStartDate(int rep) throws HL7Exception {
    return (DT) super.removeRepetition(8, rep);
  }


  /**
   * Removes a repetition of
   * NK1-8: "Start Date" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public DT removeNk18_StartDate(int rep) throws HL7Exception {
    return (DT) super.removeRepetition(8, rep);
  }


  /**
   * Returns all repetitions of End Date (NK1-9).
   */
  public DT[] getEndDate() {
    DT[] retVal = this.getTypedField(9, new DT[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of End Date (NK1-9).
   */
  public DT[] getNk19_EndDate() {
    DT[] retVal = this.getTypedField(9, new DT[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of End Date (NK1-9).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getEndDateReps() {
    return this.getReps(9);
  }


  /**
   * Returns a specific repetition of
   * NK1-9: "End Date" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public DT getEndDate(int rep) {
    DT retVal = this.getTypedField(9, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * NK1-9: "End Date" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public DT getNk19_EndDate(int rep) {
    DT retVal = this.getTypedField(9, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of End Date (NK1-9).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getNk19_EndDateReps() {
    return this.getReps(9);
  }


  /**
   * Inserts a repetition of
   * NK1-9: "End Date" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public DT insertEndDate(int rep) throws HL7Exception {
    return (DT) super.insertRepetition(9, rep);
  }


  /**
   * Inserts a repetition of
   * NK1-9: "End Date" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public DT insertNk19_EndDate(int rep) throws HL7Exception {
    return (DT) super.insertRepetition(9, rep);
  }


  /**
   * Removes a repetition of
   * NK1-9: "End Date" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public DT removeEndDate(int rep) throws HL7Exception {
    return (DT) super.removeRepetition(9, rep);
  }


  /**
   * Removes a repetition of
   * NK1-9: "End Date" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public DT removeNk19_EndDate(int rep) throws HL7Exception {
    return (DT) super.removeRepetition(9, rep);
  }


  /**
   * Returns all repetitions of Next of Kin / Associated Parties Job Title (NK1-10).
   */
  public ST[] getNextOfKinAssociatedPartiesJobTitle() {
    ST[] retVal = this.getTypedField(10, new ST[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Next of Kin / Associated Parties Job Title (NK1-10).
   */
  public ST[] getNk110_NextOfKinAssociatedPartiesJobTitle() {
    ST[] retVal = this.getTypedField(10, new ST[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Next of Kin / Associated Parties Job Title (NK1-10).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getNextOfKinAssociatedPartiesJobTitleReps() {
    return this.getReps(10);
  }


  /**
   * Returns a specific repetition of
   * NK1-10: "Next of Kin / Associated Parties Job Title" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public ST getNextOfKinAssociatedPartiesJobTitle(int rep) {
    ST retVal = this.getTypedField(10, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * NK1-10: "Next of Kin / Associated Parties Job Title" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public ST getNk110_NextOfKinAssociatedPartiesJobTitle(int rep) {
    ST retVal = this.getTypedField(10, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Next of Kin / Associated Parties Job Title (NK1-10).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getNk110_NextOfKinAssociatedPartiesJobTitleReps() {
    return this.getReps(10);
  }


  /**
   * Inserts a repetition of
   * NK1-10: "Next of Kin / Associated Parties Job Title" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ST insertNextOfKinAssociatedPartiesJobTitle(int rep) throws HL7Exception {
    return (ST) super.insertRepetition(10, rep);
  }


  /**
   * Inserts a repetition of
   * NK1-10: "Next of Kin / Associated Parties Job Title" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ST insertNk110_NextOfKinAssociatedPartiesJobTitle(int rep) throws HL7Exception {
    return (ST) super.insertRepetition(10, rep);
  }


  /**
   * Removes a repetition of
   * NK1-10: "Next of Kin / Associated Parties Job Title" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ST removeNextOfKinAssociatedPartiesJobTitle(int rep) throws HL7Exception {
    return (ST) super.removeRepetition(10, rep);
  }


  /**
   * Removes a repetition of
   * NK1-10: "Next of Kin / Associated Parties Job Title" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ST removeNk110_NextOfKinAssociatedPartiesJobTitle(int rep) throws HL7Exception {
    return (ST) super.removeRepetition(10, rep);
  }


  /**
   * Returns all repetitions of Next of Kin / Associated Parties Job Code/Class (NK1-11).
   */
  public JCC[] getNextOfKinAssociatedPartiesJobCodeClass() {
    JCC[] retVal = this.getTypedField(11, new JCC[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Next of Kin / Associated Parties Job Code/Class (NK1-11).
   */
  public JCC[] getNk111_NextOfKinAssociatedPartiesJobCodeClass() {
    JCC[] retVal = this.getTypedField(11, new JCC[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Next of Kin / Associated Parties Job Code/Class (NK1-11).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getNextOfKinAssociatedPartiesJobCodeClassReps() {
    return this.getReps(11);
  }


  /**
   * Returns a specific repetition of
   * NK1-11: "Next of Kin / Associated Parties Job Code/Class" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public JCC getNextOfKinAssociatedPartiesJobCodeClass(int rep) {
    JCC retVal = this.getTypedField(11, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * NK1-11: "Next of Kin / Associated Parties Job Code/Class" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public JCC getNk111_NextOfKinAssociatedPartiesJobCodeClass(int rep) {
    JCC retVal = this.getTypedField(11, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Next of Kin / Associated Parties Job Code/Class (NK1-11).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getNk111_NextOfKinAssociatedPartiesJobCodeClassReps() {
    return this.getReps(11);
  }


  /**
   * Inserts a repetition of
   * NK1-11: "Next of Kin / Associated Parties Job Code/Class" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public JCC insertNextOfKinAssociatedPartiesJobCodeClass(int rep) throws HL7Exception {
    return (JCC) super.insertRepetition(11, rep);
  }


  /**
   * Inserts a repetition of
   * NK1-11: "Next of Kin / Associated Parties Job Code/Class" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public JCC insertNk111_NextOfKinAssociatedPartiesJobCodeClass(int rep) throws HL7Exception {
    return (JCC) super.insertRepetition(11, rep);
  }


  /**
   * Removes a repetition of
   * NK1-11: "Next of Kin / Associated Parties Job Code/Class" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public JCC removeNextOfKinAssociatedPartiesJobCodeClass(int rep) throws HL7Exception {
    return (JCC) super.removeRepetition(11, rep);
  }


  /**
   * Removes a repetition of
   * NK1-11: "Next of Kin / Associated Parties Job Code/Class" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public JCC removeNk111_NextOfKinAssociatedPartiesJobCodeClass(int rep) throws HL7Exception {
    return (JCC) super.removeRepetition(11, rep);
  }


  /**
   * Returns all repetitions of Next of Kin / Associated Parties Employee Number (NK1-12).
   */
  public CX[] getNextOfKinAssociatedPartiesEmployeeNumber() {
    CX[] retVal = this.getTypedField(12, new CX[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Next of Kin / Associated Parties Employee Number (NK1-12).
   */
  public CX[] getNk112_NextOfKinAssociatedPartiesEmployeeNumber() {
    CX[] retVal = this.getTypedField(12, new CX[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Next of Kin / Associated Parties Employee Number (NK1-12).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getNextOfKinAssociatedPartiesEmployeeNumberReps() {
    return this.getReps(12);
  }


  /**
   * Returns a specific repetition of
   * NK1-12: "Next of Kin / Associated Parties Employee Number" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CX getNextOfKinAssociatedPartiesEmployeeNumber(int rep) {
    CX retVal = this.getTypedField(12, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * NK1-12: "Next of Kin / Associated Parties Employee Number" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CX getNk112_NextOfKinAssociatedPartiesEmployeeNumber(int rep) {
    CX retVal = this.getTypedField(12, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Next of Kin / Associated Parties Employee Number (NK1-12).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getNk112_NextOfKinAssociatedPartiesEmployeeNumberReps() {
    return this.getReps(12);
  }


  /**
   * Inserts a repetition of
   * NK1-12: "Next of Kin / Associated Parties Employee Number" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CX insertNextOfKinAssociatedPartiesEmployeeNumber(int rep) throws HL7Exception {
    return (CX) super.insertRepetition(12, rep);
  }


  /**
   * Inserts a repetition of
   * NK1-12: "Next of Kin / Associated Parties Employee Number" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CX insertNk112_NextOfKinAssociatedPartiesEmployeeNumber(int rep) throws HL7Exception {
    return (CX) super.insertRepetition(12, rep);
  }


  /**
   * Removes a repetition of
   * NK1-12: "Next of Kin / Associated Parties Employee Number" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CX removeNextOfKinAssociatedPartiesEmployeeNumber(int rep) throws HL7Exception {
    return (CX) super.removeRepetition(12, rep);
  }


  /**
   * Removes a repetition of
   * NK1-12: "Next of Kin / Associated Parties Employee Number" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CX removeNk112_NextOfKinAssociatedPartiesEmployeeNumber(int rep) throws HL7Exception {
    return (CX) super.removeRepetition(12, rep);
  }


  /**
   * Returns
   * NK1-13: "Organization Name - NK1" - creates it if necessary
   */
  public XON getOrganizationNameNK1() {
    XON retVal = this.getTypedField(13, 0);
    return retVal;
  }

  /**
   * Returns
   * NK1-13: "Organization Name - NK1" - creates it if necessary
   */
  public XON getNk113_OrganizationNameNK1() {
    XON retVal = this.getTypedField(13, 0);
    return retVal;
  }


  /**
   * Returns all repetitions of Marital Status (NK1-14).
   */
  public CWE[] getMaritalStatus() {
    CWE[] retVal = this.getTypedField(14, new CWE[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Marital Status (NK1-14).
   */
  public CWE[] getNk114_MaritalStatus() {
    CWE[] retVal = this.getTypedField(14, new CWE[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Marital Status (NK1-14).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getMaritalStatusReps() {
    return this.getReps(14);
  }


  /**
   * Returns a specific repetition of
   * NK1-14: "Marital Status" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getMaritalStatus(int rep) {
    CWE retVal = this.getTypedField(14, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * NK1-14: "Marital Status" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getNk114_MaritalStatus(int rep) {
    CWE retVal = this.getTypedField(14, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Marital Status (NK1-14).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getNk114_MaritalStatusReps() {
    return this.getReps(14);
  }


  /**
   * Inserts a repetition of
   * NK1-14: "Marital Status" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertMaritalStatus(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(14, rep);
  }


  /**
   * Inserts a repetition of
   * NK1-14: "Marital Status" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertNk114_MaritalStatus(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(14, rep);
  }


  /**
   * Removes a repetition of
   * NK1-14: "Marital Status" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeMaritalStatus(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(14, rep);
  }


  /**
   * Removes a repetition of
   * NK1-14: "Marital Status" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeNk114_MaritalStatus(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(14, rep);
  }


  /**
   * Returns all repetitions of Administrative Sex (NK1-15).
   */
  public IS[] getAdministrativeSex() {
    IS[] retVal = this.getTypedField(15, new IS[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Administrative Sex (NK1-15).
   */
  public IS[] getNk115_AdministrativeSex() {
    IS[] retVal = this.getTypedField(15, new IS[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Administrative Sex (NK1-15).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getAdministrativeSexReps() {
    return this.getReps(15);
  }


  /**
   * Returns a specific repetition of
   * NK1-15: "Administrative Sex" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public IS getAdministrativeSex(int rep) {
    IS retVal = this.getTypedField(15, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * NK1-15: "Administrative Sex" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public IS getNk115_AdministrativeSex(int rep) {
    IS retVal = this.getTypedField(15, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Administrative Sex (NK1-15).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getNk115_AdministrativeSexReps() {
    return this.getReps(15);
  }


  /**
   * Inserts a repetition of
   * NK1-15: "Administrative Sex" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS insertAdministrativeSex(int rep) throws HL7Exception {
    return (IS) super.insertRepetition(15, rep);
  }


  /**
   * Inserts a repetition of
   * NK1-15: "Administrative Sex" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS insertNk115_AdministrativeSex(int rep) throws HL7Exception {
    return (IS) super.insertRepetition(15, rep);
  }


  /**
   * Removes a repetition of
   * NK1-15: "Administrative Sex" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS removeAdministrativeSex(int rep) throws HL7Exception {
    return (IS) super.removeRepetition(15, rep);
  }


  /**
   * Removes a repetition of
   * NK1-15: "Administrative Sex" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS removeNk115_AdministrativeSex(int rep) throws HL7Exception {
    return (IS) super.removeRepetition(15, rep);
  }


  /**
   * Returns all repetitions of Date/Time of Birth (NK1-16).
   */
  public TS[] getDateTimeOfBirth() {
    TS[] retVal = this.getTypedField(16, new TS[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Date/Time of Birth (NK1-16).
   */
  public TS[] getNk116_DateTimeOfBirth() {
    TS[] retVal = this.getTypedField(16, new TS[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Date/Time of Birth (NK1-16).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getDateTimeOfBirthReps() {
    return this.getReps(16);
  }


  /**
   * Returns a specific repetition of
   * NK1-16: "Date/Time of Birth" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public TS getDateTimeOfBirth(int rep) {
    TS retVal = this.getTypedField(16, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * NK1-16: "Date/Time of Birth" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public TS getNk116_DateTimeOfBirth(int rep) {
    TS retVal = this.getTypedField(16, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Date/Time of Birth (NK1-16).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getNk116_DateTimeOfBirthReps() {
    return this.getReps(16);
  }


  /**
   * Inserts a repetition of
   * NK1-16: "Date/Time of Birth" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public TS insertDateTimeOfBirth(int rep) throws HL7Exception {
    return (TS) super.insertRepetition(16, rep);
  }


  /**
   * Inserts a repetition of
   * NK1-16: "Date/Time of Birth" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public TS insertNk116_DateTimeOfBirth(int rep) throws HL7Exception {
    return (TS) super.insertRepetition(16, rep);
  }


  /**
   * Removes a repetition of
   * NK1-16: "Date/Time of Birth" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public TS removeDateTimeOfBirth(int rep) throws HL7Exception {
    return (TS) super.removeRepetition(16, rep);
  }


  /**
   * Removes a repetition of
   * NK1-16: "Date/Time of Birth" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public TS removeNk116_DateTimeOfBirth(int rep) throws HL7Exception {
    return (TS) super.removeRepetition(16, rep);
  }


  /**
   * Returns all repetitions of Living Dependency (NK1-17).
   */
  public IS[] getLivingDependency() {
    IS[] retVal = this.getTypedField(17, new IS[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Living Dependency (NK1-17).
   */
  public IS[] getNk117_LivingDependency() {
    IS[] retVal = this.getTypedField(17, new IS[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Living Dependency (NK1-17).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getLivingDependencyReps() {
    return this.getReps(17);
  }


  /**
   * Returns a specific repetition of
   * NK1-17: "Living Dependency" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public IS getLivingDependency(int rep) {
    IS retVal = this.getTypedField(17, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * NK1-17: "Living Dependency" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public IS getNk117_LivingDependency(int rep) {
    IS retVal = this.getTypedField(17, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Living Dependency (NK1-17).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getNk117_LivingDependencyReps() {
    return this.getReps(17);
  }


  /**
   * Inserts a repetition of
   * NK1-17: "Living Dependency" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS insertLivingDependency(int rep) throws HL7Exception {
    return (IS) super.insertRepetition(17, rep);
  }


  /**
   * Inserts a repetition of
   * NK1-17: "Living Dependency" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS insertNk117_LivingDependency(int rep) throws HL7Exception {
    return (IS) super.insertRepetition(17, rep);
  }


  /**
   * Removes a repetition of
   * NK1-17: "Living Dependency" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS removeLivingDependency(int rep) throws HL7Exception {
    return (IS) super.removeRepetition(17, rep);
  }


  /**
   * Removes a repetition of
   * NK1-17: "Living Dependency" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS removeNk117_LivingDependency(int rep) throws HL7Exception {
    return (IS) super.removeRepetition(17, rep);
  }


  /**
   * Returns all repetitions of Ambulatory Status (NK1-18).
   */
  public IS[] getAmbulatoryStatus() {
    IS[] retVal = this.getTypedField(18, new IS[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Ambulatory Status (NK1-18).
   */
  public IS[] getNk118_AmbulatoryStatus() {
    IS[] retVal = this.getTypedField(18, new IS[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Ambulatory Status (NK1-18).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getAmbulatoryStatusReps() {
    return this.getReps(18);
  }


  /**
   * Returns a specific repetition of
   * NK1-18: "Ambulatory Status" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public IS getAmbulatoryStatus(int rep) {
    IS retVal = this.getTypedField(18, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * NK1-18: "Ambulatory Status" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public IS getNk118_AmbulatoryStatus(int rep) {
    IS retVal = this.getTypedField(18, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Ambulatory Status (NK1-18).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getNk118_AmbulatoryStatusReps() {
    return this.getReps(18);
  }


  /**
   * Inserts a repetition of
   * NK1-18: "Ambulatory Status" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS insertAmbulatoryStatus(int rep) throws HL7Exception {
    return (IS) super.insertRepetition(18, rep);
  }


  /**
   * Inserts a repetition of
   * NK1-18: "Ambulatory Status" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS insertNk118_AmbulatoryStatus(int rep) throws HL7Exception {
    return (IS) super.insertRepetition(18, rep);
  }


  /**
   * Removes a repetition of
   * NK1-18: "Ambulatory Status" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS removeAmbulatoryStatus(int rep) throws HL7Exception {
    return (IS) super.removeRepetition(18, rep);
  }


  /**
   * Removes a repetition of
   * NK1-18: "Ambulatory Status" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS removeNk118_AmbulatoryStatus(int rep) throws HL7Exception {
    return (IS) super.removeRepetition(18, rep);
  }


  /**
   * Returns all repetitions of Citizenship (NK1-19).
   */
  public CWE[] getCitizenship() {
    CWE[] retVal = this.getTypedField(19, new CWE[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Citizenship (NK1-19).
   */
  public CWE[] getNk119_Citizenship() {
    CWE[] retVal = this.getTypedField(19, new CWE[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Citizenship (NK1-19).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getCitizenshipReps() {
    return this.getReps(19);
  }


  /**
   * Returns a specific repetition of
   * NK1-19: "Citizenship" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getCitizenship(int rep) {
    CWE retVal = this.getTypedField(19, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * NK1-19: "Citizenship" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getNk119_Citizenship(int rep) {
    CWE retVal = this.getTypedField(19, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Citizenship (NK1-19).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getNk119_CitizenshipReps() {
    return this.getReps(19);
  }


  /**
   * Inserts a repetition of
   * NK1-19: "Citizenship" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertCitizenship(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(19, rep);
  }


  /**
   * Inserts a repetition of
   * NK1-19: "Citizenship" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertNk119_Citizenship(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(19, rep);
  }


  /**
   * Removes a repetition of
   * NK1-19: "Citizenship" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeCitizenship(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(19, rep);
  }


  /**
   * Removes a repetition of
   * NK1-19: "Citizenship" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeNk119_Citizenship(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(19, rep);
  }


  /**
   * Returns
   * NK1-20: "Primary Language" - creates it if necessary
   */
  public CWE getPrimaryLanguage() {
    CWE retVal = this.getTypedField(20, 0);
    return retVal;
  }

  /**
   * Returns
   * NK1-20: "Primary Language" - creates it if necessary
   */
  public CWE getNk120_PrimaryLanguage() {
    CWE retVal = this.getTypedField(20, 0);
    return retVal;
  }


  /**
   * Returns all repetitions of Living Arrangement (NK1-21).
   */
  public IS[] getLivingArrangement() {
    IS[] retVal = this.getTypedField(21, new IS[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Living Arrangement (NK1-21).
   */
  public IS[] getNk121_LivingArrangement() {
    IS[] retVal = this.getTypedField(21, new IS[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Living Arrangement (NK1-21).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getLivingArrangementReps() {
    return this.getReps(21);
  }


  /**
   * Returns a specific repetition of
   * NK1-21: "Living Arrangement" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public IS getLivingArrangement(int rep) {
    IS retVal = this.getTypedField(21, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * NK1-21: "Living Arrangement" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public IS getNk121_LivingArrangement(int rep) {
    IS retVal = this.getTypedField(21, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Living Arrangement (NK1-21).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getNk121_LivingArrangementReps() {
    return this.getReps(21);
  }


  /**
   * Inserts a repetition of
   * NK1-21: "Living Arrangement" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS insertLivingArrangement(int rep) throws HL7Exception {
    return (IS) super.insertRepetition(21, rep);
  }


  /**
   * Inserts a repetition of
   * NK1-21: "Living Arrangement" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS insertNk121_LivingArrangement(int rep) throws HL7Exception {
    return (IS) super.insertRepetition(21, rep);
  }


  /**
   * Removes a repetition of
   * NK1-21: "Living Arrangement" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS removeLivingArrangement(int rep) throws HL7Exception {
    return (IS) super.removeRepetition(21, rep);
  }


  /**
   * Removes a repetition of
   * NK1-21: "Living Arrangement" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS removeNk121_LivingArrangement(int rep) throws HL7Exception {
    return (IS) super.removeRepetition(21, rep);
  }


  /**
   * Returns all repetitions of Publicity Code (NK1-22).
   */
  public CWE[] getPublicityCode() {
    CWE[] retVal = this.getTypedField(22, new CWE[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Publicity Code (NK1-22).
   */
  public CWE[] getNk122_PublicityCode() {
    CWE[] retVal = this.getTypedField(22, new CWE[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Publicity Code (NK1-22).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getPublicityCodeReps() {
    return this.getReps(22);
  }


  /**
   * Returns a specific repetition of
   * NK1-22: "Publicity Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getPublicityCode(int rep) {
    CWE retVal = this.getTypedField(22, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * NK1-22: "Publicity Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getNk122_PublicityCode(int rep) {
    CWE retVal = this.getTypedField(22, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Publicity Code (NK1-22).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getNk122_PublicityCodeReps() {
    return this.getReps(22);
  }


  /**
   * Inserts a repetition of
   * NK1-22: "Publicity Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertPublicityCode(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(22, rep);
  }


  /**
   * Inserts a repetition of
   * NK1-22: "Publicity Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertNk122_PublicityCode(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(22, rep);
  }


  /**
   * Removes a repetition of
   * NK1-22: "Publicity Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removePublicityCode(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(22, rep);
  }


  /**
   * Removes a repetition of
   * NK1-22: "Publicity Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeNk122_PublicityCode(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(22, rep);
  }


  /**
   * Returns all repetitions of Protection Indicator (NK1-23).
   */
  public ID[] getProtectionIndicator() {
    ID[] retVal = this.getTypedField(23, new ID[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Protection Indicator (NK1-23).
   */
  public ID[] getNk123_ProtectionIndicator() {
    ID[] retVal = this.getTypedField(23, new ID[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Protection Indicator (NK1-23).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getProtectionIndicatorReps() {
    return this.getReps(23);
  }


  /**
   * Returns a specific repetition of
   * NK1-23: "Protection Indicator" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public ID getProtectionIndicator(int rep) {
    ID retVal = this.getTypedField(23, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * NK1-23: "Protection Indicator" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public ID getNk123_ProtectionIndicator(int rep) {
    ID retVal = this.getTypedField(23, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Protection Indicator (NK1-23).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getNk123_ProtectionIndicatorReps() {
    return this.getReps(23);
  }


  /**
   * Inserts a repetition of
   * NK1-23: "Protection Indicator" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID insertProtectionIndicator(int rep) throws HL7Exception {
    return (ID) super.insertRepetition(23, rep);
  }


  /**
   * Inserts a repetition of
   * NK1-23: "Protection Indicator" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID insertNk123_ProtectionIndicator(int rep) throws HL7Exception {
    return (ID) super.insertRepetition(23, rep);
  }


  /**
   * Removes a repetition of
   * NK1-23: "Protection Indicator" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID removeProtectionIndicator(int rep) throws HL7Exception {
    return (ID) super.removeRepetition(23, rep);
  }


  /**
   * Removes a repetition of
   * NK1-23: "Protection Indicator" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ID removeNk123_ProtectionIndicator(int rep) throws HL7Exception {
    return (ID) super.removeRepetition(23, rep);
  }


  /**
   * Returns all repetitions of Student Indicator (NK1-24).
   */
  public IS[] getStudentIndicator() {
    IS[] retVal = this.getTypedField(24, new IS[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Student Indicator (NK1-24).
   */
  public IS[] getNk124_StudentIndicator() {
    IS[] retVal = this.getTypedField(24, new IS[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Student Indicator (NK1-24).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getStudentIndicatorReps() {
    return this.getReps(24);
  }


  /**
   * Returns a specific repetition of
   * NK1-24: "Student Indicator" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public IS getStudentIndicator(int rep) {
    IS retVal = this.getTypedField(24, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * NK1-24: "Student Indicator" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public IS getNk124_StudentIndicator(int rep) {
    IS retVal = this.getTypedField(24, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Student Indicator (NK1-24).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getNk124_StudentIndicatorReps() {
    return this.getReps(24);
  }


  /**
   * Inserts a repetition of
   * NK1-24: "Student Indicator" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS insertStudentIndicator(int rep) throws HL7Exception {
    return (IS) super.insertRepetition(24, rep);
  }


  /**
   * Inserts a repetition of
   * NK1-24: "Student Indicator" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS insertNk124_StudentIndicator(int rep) throws HL7Exception {
    return (IS) super.insertRepetition(24, rep);
  }


  /**
   * Removes a repetition of
   * NK1-24: "Student Indicator" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS removeStudentIndicator(int rep) throws HL7Exception {
    return (IS) super.removeRepetition(24, rep);
  }


  /**
   * Removes a repetition of
   * NK1-24: "Student Indicator" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS removeNk124_StudentIndicator(int rep) throws HL7Exception {
    return (IS) super.removeRepetition(24, rep);
  }


  /**
   * Returns all repetitions of Religion (NK1-25).
   */
  public CWE[] getReligion() {
    CWE[] retVal = this.getTypedField(25, new CWE[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Religion (NK1-25).
   */
  public CWE[] getNk125_Religion() {
    CWE[] retVal = this.getTypedField(25, new CWE[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Religion (NK1-25).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getReligionReps() {
    return this.getReps(25);
  }


  /**
   * Returns a specific repetition of
   * NK1-25: "Religion" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getReligion(int rep) {
    CWE retVal = this.getTypedField(25, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * NK1-25: "Religion" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getNk125_Religion(int rep) {
    CWE retVal = this.getTypedField(25, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Religion (NK1-25).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getNk125_ReligionReps() {
    return this.getReps(25);
  }


  /**
   * Inserts a repetition of
   * NK1-25: "Religion" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertReligion(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(25, rep);
  }


  /**
   * Inserts a repetition of
   * NK1-25: "Religion" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertNk125_Religion(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(25, rep);
  }


  /**
   * Removes a repetition of
   * NK1-25: "Religion" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeReligion(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(25, rep);
  }


  /**
   * Removes a repetition of
   * NK1-25: "Religion" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeNk125_Religion(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(25, rep);
  }


  /**
   * Returns all repetitions of Mother's Maiden Name (NK1-26).
   */
  public XPN[] getMotherSMaidenName() {
    XPN[] retVal = this.getTypedField(26, new XPN[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Mother's Maiden Name (NK1-26).
   */
  public XPN[] getNk126_MotherSMaidenName() {
    XPN[] retVal = this.getTypedField(26, new XPN[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Mother's Maiden Name (NK1-26).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getMotherSMaidenNameReps() {
    return this.getReps(26);
  }


  /**
   * Returns a specific repetition of
   * NK1-26: "Mother's Maiden Name" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XPN getMotherSMaidenName(int rep) {
    XPN retVal = this.getTypedField(26, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * NK1-26: "Mother's Maiden Name" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XPN getNk126_MotherSMaidenName(int rep) {
    XPN retVal = this.getTypedField(26, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Mother's Maiden Name (NK1-26).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getNk126_MotherSMaidenNameReps() {
    return this.getReps(26);
  }


  /**
   * Inserts a repetition of
   * NK1-26: "Mother's Maiden Name" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XPN insertMotherSMaidenName(int rep) throws HL7Exception {
    return (XPN) super.insertRepetition(26, rep);
  }


  /**
   * Inserts a repetition of
   * NK1-26: "Mother's Maiden Name" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XPN insertNk126_MotherSMaidenName(int rep) throws HL7Exception {
    return (XPN) super.insertRepetition(26, rep);
  }


  /**
   * Removes a repetition of
   * NK1-26: "Mother's Maiden Name" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XPN removeMotherSMaidenName(int rep) throws HL7Exception {
    return (XPN) super.removeRepetition(26, rep);
  }


  /**
   * Removes a repetition of
   * NK1-26: "Mother's Maiden Name" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XPN removeNk126_MotherSMaidenName(int rep) throws HL7Exception {
    return (XPN) super.removeRepetition(26, rep);
  }


  /**
   * Returns all repetitions of Nationality (NK1-27).
   */
  public CWE[] getNationality() {
    CWE[] retVal = this.getTypedField(27, new CWE[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Nationality (NK1-27).
   */
  public CWE[] getNk127_Nationality() {
    CWE[] retVal = this.getTypedField(27, new CWE[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Nationality (NK1-27).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getNationalityReps() {
    return this.getReps(27);
  }


  /**
   * Returns a specific repetition of
   * NK1-27: "Nationality" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getNationality(int rep) {
    CWE retVal = this.getTypedField(27, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * NK1-27: "Nationality" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getNk127_Nationality(int rep) {
    CWE retVal = this.getTypedField(27, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Nationality (NK1-27).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getNk127_NationalityReps() {
    return this.getReps(27);
  }


  /**
   * Inserts a repetition of
   * NK1-27: "Nationality" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertNationality(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(27, rep);
  }


  /**
   * Inserts a repetition of
   * NK1-27: "Nationality" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertNk127_Nationality(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(27, rep);
  }


  /**
   * Removes a repetition of
   * NK1-27: "Nationality" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeNationality(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(27, rep);
  }


  /**
   * Removes a repetition of
   * NK1-27: "Nationality" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeNk127_Nationality(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(27, rep);
  }


  /**
   * Returns all repetitions of Ethnic Group (NK1-28).
   */
  public CWE[] getEthnicGroup() {
    CWE[] retVal = this.getTypedField(28, new CWE[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Ethnic Group (NK1-28).
   */
  public CWE[] getNk128_EthnicGroup() {
    CWE[] retVal = this.getTypedField(28, new CWE[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Ethnic Group (NK1-28).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getEthnicGroupReps() {
    return this.getReps(28);
  }


  /**
   * Returns a specific repetition of
   * NK1-28: "Ethnic Group" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getEthnicGroup(int rep) {
    CWE retVal = this.getTypedField(28, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * NK1-28: "Ethnic Group" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getNk128_EthnicGroup(int rep) {
    CWE retVal = this.getTypedField(28, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Ethnic Group (NK1-28).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getNk128_EthnicGroupReps() {
    return this.getReps(28);
  }


  /**
   * Inserts a repetition of
   * NK1-28: "Ethnic Group" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertEthnicGroup(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(28, rep);
  }


  /**
   * Inserts a repetition of
   * NK1-28: "Ethnic Group" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertNk128_EthnicGroup(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(28, rep);
  }


  /**
   * Removes a repetition of
   * NK1-28: "Ethnic Group" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeEthnicGroup(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(28, rep);
  }


  /**
   * Removes a repetition of
   * NK1-28: "Ethnic Group" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeNk128_EthnicGroup(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(28, rep);
  }


  /**
   * Returns all repetitions of Contact Reason (NK1-29).
   */
  public CWE[] getContactReason() {
    CWE[] retVal = this.getTypedField(29, new CWE[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Contact Reason (NK1-29).
   */
  public CWE[] getNk129_ContactReason() {
    CWE[] retVal = this.getTypedField(29, new CWE[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Contact Reason (NK1-29).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getContactReasonReps() {
    return this.getReps(29);
  }


  /**
   * Returns a specific repetition of
   * NK1-29: "Contact Reason" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getContactReason(int rep) {
    CWE retVal = this.getTypedField(29, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * NK1-29: "Contact Reason" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getNk129_ContactReason(int rep) {
    CWE retVal = this.getTypedField(29, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Contact Reason (NK1-29).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getNk129_ContactReasonReps() {
    return this.getReps(29);
  }


  /**
   * Inserts a repetition of
   * NK1-29: "Contact Reason" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertContactReason(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(29, rep);
  }


  /**
   * Inserts a repetition of
   * NK1-29: "Contact Reason" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertNk129_ContactReason(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(29, rep);
  }


  /**
   * Removes a repetition of
   * NK1-29: "Contact Reason" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeContactReason(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(29, rep);
  }


  /**
   * Removes a repetition of
   * NK1-29: "Contact Reason" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeNk129_ContactReason(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(29, rep);
  }


  /**
   * Returns all repetitions of Contact Person's Name (NK1-30).
   */
  public XPN_ELR[] getContactPersonSName() {
    XPN_ELR[] retVal = this.getTypedField(30, new XPN_ELR[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Contact Person's Name (NK1-30).
   */
  public XPN_ELR[] getNk130_ContactPersonSName() {
    XPN_ELR[] retVal = this.getTypedField(30, new XPN_ELR[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Contact Person's Name (NK1-30).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getContactPersonSNameReps() {
    return this.getReps(30);
  }


  /**
   * Returns a specific repetition of
   * NK1-30: "Contact Person's Name" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XPN_ELR getContactPersonSName(int rep) {
    XPN_ELR retVal = this.getTypedField(30, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * NK1-30: "Contact Person's Name" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XPN_ELR getNk130_ContactPersonSName(int rep) {
    XPN_ELR retVal = this.getTypedField(30, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Contact Person's Name (NK1-30).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getNk130_ContactPersonSNameReps() {
    return this.getReps(30);
  }


  /**
   * Inserts a repetition of
   * NK1-30: "Contact Person's Name" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XPN_ELR insertContactPersonSName(int rep) throws HL7Exception {
    return (XPN_ELR) super.insertRepetition(30, rep);
  }


  /**
   * Inserts a repetition of
   * NK1-30: "Contact Person's Name" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XPN_ELR insertNk130_ContactPersonSName(int rep) throws HL7Exception {
    return (XPN_ELR) super.insertRepetition(30, rep);
  }


  /**
   * Removes a repetition of
   * NK1-30: "Contact Person's Name" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XPN_ELR removeContactPersonSName(int rep) throws HL7Exception {
    return (XPN_ELR) super.removeRepetition(30, rep);
  }


  /**
   * Removes a repetition of
   * NK1-30: "Contact Person's Name" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XPN_ELR removeNk130_ContactPersonSName(int rep) throws HL7Exception {
    return (XPN_ELR) super.removeRepetition(30, rep);
  }


  /**
   * Returns all repetitions of Contact Person's Telephone Number (NK1-31).
   */
  public XTN[] getContactPersonSTelephoneNumber() {
    XTN[] retVal = this.getTypedField(31, new XTN[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Contact Person's Telephone Number (NK1-31).
   */
  public XTN[] getNk131_ContactPersonSTelephoneNumber() {
    XTN[] retVal = this.getTypedField(31, new XTN[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Contact Person's Telephone Number (NK1-31).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getContactPersonSTelephoneNumberReps() {
    return this.getReps(31);
  }


  /**
   * Returns a specific repetition of
   * NK1-31: "Contact Person's Telephone Number" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XTN getContactPersonSTelephoneNumber(int rep) {
    XTN retVal = this.getTypedField(31, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * NK1-31: "Contact Person's Telephone Number" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XTN getNk131_ContactPersonSTelephoneNumber(int rep) {
    XTN retVal = this.getTypedField(31, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Contact Person's Telephone Number (NK1-31).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getNk131_ContactPersonSTelephoneNumberReps() {
    return this.getReps(31);
  }


  /**
   * Inserts a repetition of
   * NK1-31: "Contact Person's Telephone Number" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XTN insertContactPersonSTelephoneNumber(int rep) throws HL7Exception {
    return (XTN) super.insertRepetition(31, rep);
  }


  /**
   * Inserts a repetition of
   * NK1-31: "Contact Person's Telephone Number" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XTN insertNk131_ContactPersonSTelephoneNumber(int rep) throws HL7Exception {
    return (XTN) super.insertRepetition(31, rep);
  }


  /**
   * Removes a repetition of
   * NK1-31: "Contact Person's Telephone Number" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XTN removeContactPersonSTelephoneNumber(int rep) throws HL7Exception {
    return (XTN) super.removeRepetition(31, rep);
  }


  /**
   * Removes a repetition of
   * NK1-31: "Contact Person's Telephone Number" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XTN removeNk131_ContactPersonSTelephoneNumber(int rep) throws HL7Exception {
    return (XTN) super.removeRepetition(31, rep);
  }


  /**
   * Returns all repetitions of Contact Person's Address (NK1-32).
   */
  public XAD[] getContactPersonSAddress() {
    XAD[] retVal = this.getTypedField(32, new XAD[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Contact Person's Address (NK1-32).
   */
  public XAD[] getNk132_ContactPersonSAddress() {
    XAD[] retVal = this.getTypedField(32, new XAD[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Contact Person's Address (NK1-32).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getContactPersonSAddressReps() {
    return this.getReps(32);
  }


  /**
   * Returns a specific repetition of
   * NK1-32: "Contact Person's Address" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XAD getContactPersonSAddress(int rep) {
    XAD retVal = this.getTypedField(32, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * NK1-32: "Contact Person's Address" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XAD getNk132_ContactPersonSAddress(int rep) {
    XAD retVal = this.getTypedField(32, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Contact Person's Address (NK1-32).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getNk132_ContactPersonSAddressReps() {
    return this.getReps(32);
  }


  /**
   * Inserts a repetition of
   * NK1-32: "Contact Person's Address" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XAD insertContactPersonSAddress(int rep) throws HL7Exception {
    return (XAD) super.insertRepetition(32, rep);
  }


  /**
   * Inserts a repetition of
   * NK1-32: "Contact Person's Address" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XAD insertNk132_ContactPersonSAddress(int rep) throws HL7Exception {
    return (XAD) super.insertRepetition(32, rep);
  }


  /**
   * Removes a repetition of
   * NK1-32: "Contact Person's Address" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XAD removeContactPersonSAddress(int rep) throws HL7Exception {
    return (XAD) super.removeRepetition(32, rep);
  }


  /**
   * Removes a repetition of
   * NK1-32: "Contact Person's Address" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XAD removeNk132_ContactPersonSAddress(int rep) throws HL7Exception {
    return (XAD) super.removeRepetition(32, rep);
  }


  /**
   * Returns all repetitions of Next of Kin/Associated Party's Identifiers (NK1-33).
   */
  public CX[] getNextOfKinAssociatedPartySIdentifiers() {
    CX[] retVal = this.getTypedField(33, new CX[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Next of Kin/Associated Party's Identifiers (NK1-33).
   */
  public CX[] getNk133_NextOfKinAssociatedPartySIdentifiers() {
    CX[] retVal = this.getTypedField(33, new CX[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Next of Kin/Associated Party's Identifiers (NK1-33).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getNextOfKinAssociatedPartySIdentifiersReps() {
    return this.getReps(33);
  }


  /**
   * Returns a specific repetition of
   * NK1-33: "Next of Kin/Associated Party's Identifiers" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CX getNextOfKinAssociatedPartySIdentifiers(int rep) {
    CX retVal = this.getTypedField(33, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * NK1-33: "Next of Kin/Associated Party's Identifiers" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CX getNk133_NextOfKinAssociatedPartySIdentifiers(int rep) {
    CX retVal = this.getTypedField(33, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Next of Kin/Associated Party's Identifiers (NK1-33).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getNk133_NextOfKinAssociatedPartySIdentifiersReps() {
    return this.getReps(33);
  }


  /**
   * Inserts a repetition of
   * NK1-33: "Next of Kin/Associated Party's Identifiers" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CX insertNextOfKinAssociatedPartySIdentifiers(int rep) throws HL7Exception {
    return (CX) super.insertRepetition(33, rep);
  }


  /**
   * Inserts a repetition of
   * NK1-33: "Next of Kin/Associated Party's Identifiers" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CX insertNk133_NextOfKinAssociatedPartySIdentifiers(int rep) throws HL7Exception {
    return (CX) super.insertRepetition(33, rep);
  }


  /**
   * Removes a repetition of
   * NK1-33: "Next of Kin/Associated Party's Identifiers" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CX removeNextOfKinAssociatedPartySIdentifiers(int rep) throws HL7Exception {
    return (CX) super.removeRepetition(33, rep);
  }


  /**
   * Removes a repetition of
   * NK1-33: "Next of Kin/Associated Party's Identifiers" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CX removeNk133_NextOfKinAssociatedPartySIdentifiers(int rep) throws HL7Exception {
    return (CX) super.removeRepetition(33, rep);
  }


  /**
   * Returns all repetitions of Job Status (NK1-34).
   */
  public IS[] getJobStatus() {
    IS[] retVal = this.getTypedField(34, new IS[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Job Status (NK1-34).
   */
  public IS[] getNk134_JobStatus() {
    IS[] retVal = this.getTypedField(34, new IS[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Job Status (NK1-34).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getJobStatusReps() {
    return this.getReps(34);
  }


  /**
   * Returns a specific repetition of
   * NK1-34: "Job Status" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public IS getJobStatus(int rep) {
    IS retVal = this.getTypedField(34, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * NK1-34: "Job Status" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public IS getNk134_JobStatus(int rep) {
    IS retVal = this.getTypedField(34, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Job Status (NK1-34).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getNk134_JobStatusReps() {
    return this.getReps(34);
  }


  /**
   * Inserts a repetition of
   * NK1-34: "Job Status" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS insertJobStatus(int rep) throws HL7Exception {
    return (IS) super.insertRepetition(34, rep);
  }


  /**
   * Inserts a repetition of
   * NK1-34: "Job Status" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS insertNk134_JobStatus(int rep) throws HL7Exception {
    return (IS) super.insertRepetition(34, rep);
  }


  /**
   * Removes a repetition of
   * NK1-34: "Job Status" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS removeJobStatus(int rep) throws HL7Exception {
    return (IS) super.removeRepetition(34, rep);
  }


  /**
   * Removes a repetition of
   * NK1-34: "Job Status" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS removeNk134_JobStatus(int rep) throws HL7Exception {
    return (IS) super.removeRepetition(34, rep);
  }


  /**
   * Returns all repetitions of Race (NK1-35).
   */
  public CWE[] getRace() {
    CWE[] retVal = this.getTypedField(35, new CWE[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Race (NK1-35).
   */
  public CWE[] getNk135_Race() {
    CWE[] retVal = this.getTypedField(35, new CWE[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Race (NK1-35).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getRaceReps() {
    return this.getReps(35);
  }


  /**
   * Returns a specific repetition of
   * NK1-35: "Race" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getRace(int rep) {
    CWE retVal = this.getTypedField(35, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * NK1-35: "Race" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getNk135_Race(int rep) {
    CWE retVal = this.getTypedField(35, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Race (NK1-35).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getNk135_RaceReps() {
    return this.getReps(35);
  }


  /**
   * Inserts a repetition of
   * NK1-35: "Race" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertRace(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(35, rep);
  }


  /**
   * Inserts a repetition of
   * NK1-35: "Race" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertNk135_Race(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(35, rep);
  }


  /**
   * Removes a repetition of
   * NK1-35: "Race" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeRace(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(35, rep);
  }


  /**
   * Removes a repetition of
   * NK1-35: "Race" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeNk135_Race(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(35, rep);
  }


  /**
   * Returns all repetitions of Handicap (NK1-36).
   */
  public IS[] getHandicap() {
    IS[] retVal = this.getTypedField(36, new IS[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Handicap (NK1-36).
   */
  public IS[] getNk136_Handicap() {
    IS[] retVal = this.getTypedField(36, new IS[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Handicap (NK1-36).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getHandicapReps() {
    return this.getReps(36);
  }


  /**
   * Returns a specific repetition of
   * NK1-36: "Handicap" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public IS getHandicap(int rep) {
    IS retVal = this.getTypedField(36, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * NK1-36: "Handicap" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public IS getNk136_Handicap(int rep) {
    IS retVal = this.getTypedField(36, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Handicap (NK1-36).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getNk136_HandicapReps() {
    return this.getReps(36);
  }


  /**
   * Inserts a repetition of
   * NK1-36: "Handicap" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS insertHandicap(int rep) throws HL7Exception {
    return (IS) super.insertRepetition(36, rep);
  }


  /**
   * Inserts a repetition of
   * NK1-36: "Handicap" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS insertNk136_Handicap(int rep) throws HL7Exception {
    return (IS) super.insertRepetition(36, rep);
  }


  /**
   * Removes a repetition of
   * NK1-36: "Handicap" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS removeHandicap(int rep) throws HL7Exception {
    return (IS) super.removeRepetition(36, rep);
  }


  /**
   * Removes a repetition of
   * NK1-36: "Handicap" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS removeNk136_Handicap(int rep) throws HL7Exception {
    return (IS) super.removeRepetition(36, rep);
  }


  /**
   * Returns all repetitions of Contact Person Social Security Number (NK1-37).
   */
  public ST[] getContactPersonSocialSecurityNumber() {
    ST[] retVal = this.getTypedField(37, new ST[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Contact Person Social Security Number (NK1-37).
   */
  public ST[] getNk137_ContactPersonSocialSecurityNumber() {
    ST[] retVal = this.getTypedField(37, new ST[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Contact Person Social Security Number (NK1-37).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getContactPersonSocialSecurityNumberReps() {
    return this.getReps(37);
  }


  /**
   * Returns a specific repetition of
   * NK1-37: "Contact Person Social Security Number" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public ST getContactPersonSocialSecurityNumber(int rep) {
    ST retVal = this.getTypedField(37, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * NK1-37: "Contact Person Social Security Number" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public ST getNk137_ContactPersonSocialSecurityNumber(int rep) {
    ST retVal = this.getTypedField(37, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Contact Person Social Security Number (NK1-37).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getNk137_ContactPersonSocialSecurityNumberReps() {
    return this.getReps(37);
  }


  /**
   * Inserts a repetition of
   * NK1-37: "Contact Person Social Security Number" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ST insertContactPersonSocialSecurityNumber(int rep) throws HL7Exception {
    return (ST) super.insertRepetition(37, rep);
  }


  /**
   * Inserts a repetition of
   * NK1-37: "Contact Person Social Security Number" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ST insertNk137_ContactPersonSocialSecurityNumber(int rep) throws HL7Exception {
    return (ST) super.insertRepetition(37, rep);
  }


  /**
   * Removes a repetition of
   * NK1-37: "Contact Person Social Security Number" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ST removeContactPersonSocialSecurityNumber(int rep) throws HL7Exception {
    return (ST) super.removeRepetition(37, rep);
  }


  /**
   * Removes a repetition of
   * NK1-37: "Contact Person Social Security Number" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ST removeNk137_ContactPersonSocialSecurityNumber(int rep) throws HL7Exception {
    return (ST) super.removeRepetition(37, rep);
  }


  /**
   * Returns all repetitions of Next of Kin Birth Place (NK1-38).
   */
  public ST[] getNextOfKinBirthPlace() {
    ST[] retVal = this.getTypedField(38, new ST[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Next of Kin Birth Place (NK1-38).
   */
  public ST[] getNk138_NextOfKinBirthPlace() {
    ST[] retVal = this.getTypedField(38, new ST[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Next of Kin Birth Place (NK1-38).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getNextOfKinBirthPlaceReps() {
    return this.getReps(38);
  }


  /**
   * Returns a specific repetition of
   * NK1-38: "Next of Kin Birth Place" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public ST getNextOfKinBirthPlace(int rep) {
    ST retVal = this.getTypedField(38, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * NK1-38: "Next of Kin Birth Place" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public ST getNk138_NextOfKinBirthPlace(int rep) {
    ST retVal = this.getTypedField(38, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Next of Kin Birth Place (NK1-38).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getNk138_NextOfKinBirthPlaceReps() {
    return this.getReps(38);
  }


  /**
   * Inserts a repetition of
   * NK1-38: "Next of Kin Birth Place" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ST insertNextOfKinBirthPlace(int rep) throws HL7Exception {
    return (ST) super.insertRepetition(38, rep);
  }


  /**
   * Inserts a repetition of
   * NK1-38: "Next of Kin Birth Place" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ST insertNk138_NextOfKinBirthPlace(int rep) throws HL7Exception {
    return (ST) super.insertRepetition(38, rep);
  }


  /**
   * Removes a repetition of
   * NK1-38: "Next of Kin Birth Place" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ST removeNextOfKinBirthPlace(int rep) throws HL7Exception {
    return (ST) super.removeRepetition(38, rep);
  }


  /**
   * Removes a repetition of
   * NK1-38: "Next of Kin Birth Place" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public ST removeNk138_NextOfKinBirthPlace(int rep) throws HL7Exception {
    return (ST) super.removeRepetition(38, rep);
  }


  /**
   * Returns all repetitions of VIP Indicator (NK1-39).
   */
  public IS[] getVIPIndicator() {
    IS[] retVal = this.getTypedField(39, new IS[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of VIP Indicator (NK1-39).
   */
  public IS[] getNk139_VIPIndicator() {
    IS[] retVal = this.getTypedField(39, new IS[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of VIP Indicator (NK1-39).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getVIPIndicatorReps() {
    return this.getReps(39);
  }


  /**
   * Returns a specific repetition of
   * NK1-39: "VIP Indicator" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public IS getVIPIndicator(int rep) {
    IS retVal = this.getTypedField(39, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * NK1-39: "VIP Indicator" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public IS getNk139_VIPIndicator(int rep) {
    IS retVal = this.getTypedField(39, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of VIP Indicator (NK1-39).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getNk139_VIPIndicatorReps() {
    return this.getReps(39);
  }


  /**
   * Inserts a repetition of
   * NK1-39: "VIP Indicator" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS insertVIPIndicator(int rep) throws HL7Exception {
    return (IS) super.insertRepetition(39, rep);
  }


  /**
   * Inserts a repetition of
   * NK1-39: "VIP Indicator" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS insertNk139_VIPIndicator(int rep) throws HL7Exception {
    return (IS) super.insertRepetition(39, rep);
  }


  /**
   * Removes a repetition of
   * NK1-39: "VIP Indicator" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS removeVIPIndicator(int rep) throws HL7Exception {
    return (IS) super.removeRepetition(39, rep);
  }


  /**
   * Removes a repetition of
   * NK1-39: "VIP Indicator" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public IS removeNk139_VIPIndicator(int rep) throws HL7Exception {
    return (IS) super.removeRepetition(39, rep);
  }


  /**
   * {@inheritDoc}
   */
  protected Type createNewTypeWithoutReflection(int field) {
    switch (field) {
      case 0:
        return new SI(getMessage());
      case 1:
        return new XPN_ELR(getMessage());
      case 2:
        return new CWE_ELR(getMessage());
      case 3:
        return new XAD(getMessage());
      case 4:
        return new XTN(getMessage());
      case 5:
        return new XTN(getMessage());
      case 6:
        return new CWE(getMessage());
      case 7:
        return new DT(getMessage());
      case 8:
        return new DT(getMessage());
      case 9:
        return new ST(getMessage());
      case 10:
        return new JCC(getMessage());
      case 11:
        return new CX(getMessage());
      case 12:
        return new XON(getMessage());
      case 13:
        return new CWE(getMessage());
      case 14:
        return new IS(getMessage(), Integer.valueOf(0));
      case 15:
        return new TS(getMessage());
      case 16:
        return new IS(getMessage(), Integer.valueOf(0));
      case 17:
        return new IS(getMessage(), Integer.valueOf(0));
      case 18:
        return new CWE(getMessage());
      case 19:
        return new CWE(getMessage());
      case 20:
        return new IS(getMessage(), Integer.valueOf(0));
      case 21:
        return new CWE(getMessage());
      case 22:
        return new ID(getMessage(), Integer.valueOf(0));
      case 23:
        return new IS(getMessage(), Integer.valueOf(0));
      case 24:
        return new CWE(getMessage());
      case 25:
        return new XPN(getMessage());
      case 26:
        return new CWE(getMessage());
      case 27:
        return new CWE(getMessage());
      case 28:
        return new CWE(getMessage());
      case 29:
        return new XPN_ELR(getMessage());
      case 30:
        return new XTN(getMessage());
      case 31:
        return new XAD(getMessage());
      case 32:
        return new CX(getMessage());
      case 33:
        return new IS(getMessage(), Integer.valueOf(0));
      case 34:
        return new CWE(getMessage());
      case 35:
        return new IS(getMessage(), Integer.valueOf(0));
      case 36:
        return new ST(getMessage());
      case 37:
        return new ST(getMessage());
      case 38:
        return new IS(getMessage(), Integer.valueOf(0));
      default:
        return null;
    }
  }


}

