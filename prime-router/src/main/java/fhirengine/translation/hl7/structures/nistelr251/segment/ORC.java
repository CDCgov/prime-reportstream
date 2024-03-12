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
import ca.uhn.hl7v2.model.v251.datatype.CNE;
import ca.uhn.hl7v2.model.v251.datatype.CWE;
import ca.uhn.hl7v2.model.v251.datatype.EIP;
import ca.uhn.hl7v2.model.v251.datatype.ID;
import ca.uhn.hl7v2.model.v251.datatype.PL;
import ca.uhn.hl7v2.model.v251.datatype.TQ;
import ca.uhn.hl7v2.model.v251.datatype.TS;
import ca.uhn.hl7v2.model.v251.datatype.XCN;
import ca.uhn.hl7v2.parser.ModelClassFactory;
import fhirengine.translation.hl7.structures.nistelr251.datatype.*;


/**
 * <p>Represents an HL7 ORC message segment (Common Order).
 * This segment has the following fields:</p>
 * <ul>
 * <li>ORC-1: Order Control (ID) <b> </b>
 * <li>ORC-2: Placer Order Number (EI_ELR) <b>optional </b>
 * <li>ORC-3: Filler Order Number (EI_ELR) <b> </b>
 * <li>ORC-4: Placer Group Number (EI_ELR) <b>optional </b>
 * <li>ORC-5: Order Status (ID) <b>optional </b>
 * <li>ORC-6: Response Flag (ID) <b>optional </b>
 * <li>ORC-7: Quantity/Timing (TQ) <b>optional repeating</b>
 * <li>ORC-8: Parent (EIP) <b>optional </b>
 * <li>ORC-9: Date/Time of Transaction (TS) <b>optional </b>
 * <li>ORC-10: Entered By (XCN) <b>optional repeating</b>
 * <li>ORC-11: Verified By (XCN) <b>optional repeating</b>
 * <li>ORC-12: Ordering Provider (XCN_ELR) <b>optional repeating</b>
 * <li>ORC-13: Enterer's Location (PL) <b>optional </b>
 * <li>ORC-14: Call Back Phone Number (XTN_ELR) <b>optional repeating</b>
 * <li>ORC-15: Order Effective Date/Time (TS) <b>optional </b>
 * <li>ORC-16: Order Control Code Reason (CWE) <b>optional </b>
 * <li>ORC-17: Entering Organization (CWE) <b>optional </b>
 * <li>ORC-18: Entering Device (CWE) <b>optional </b>
 * <li>ORC-19: Action By (XCN) <b>optional repeating</b>
 * <li>ORC-20: Advanced Beneficiary Notice Code (CWE) <b>optional repeating</b>
 * <li>ORC-21: Ordering Facility Name (XON_ELR) <b> </b>
 * <li>ORC-22: Ordering Facility Address (XAD_ELR) <b> </b>
 * <li>ORC-23: Ordering Facility Phone Number (XTN_ELR) <b> repeating</b>
 * <li>ORC-24: Ordering Provider Address (XAD_ELR) <b>optional repeating</b>
 * <li>ORC-25: Order Status Modifier (CWE) <b>optional </b>
 * <li>ORC-26: Advanced Beneficiary Notice Override Reason (CWE) <b>optional repeating</b>
 * <li>ORC-27: Filler's Expected Availability Date/Time (TS) <b>optional </b>
 * <li>ORC-28: Confidentiality Code (CWE) <b>optional </b>
 * <li>ORC-29: Order Type (CWE) <b>optional </b>
 * <li>ORC-30: Enterer Authorization Mode (CNE) <b>optional </b>
 * <li>ORC-31: Parent Universal Service Identifier (CWE) <b>optional </b>
 * </ul>
 */
@SuppressWarnings("unused")
public class ORC extends AbstractSegment {

  /**
   * Creates a new ORC segment
   */
  public ORC(Group parent, ModelClassFactory factory) {
    super(parent, factory);
    init(factory);
  }

  private void init(ModelClassFactory factory) {
    try {
      this.add(ID.class, true, 1, 0, new Object[]{getMessage(), Integer.valueOf(119)}, "Order Control");
      this.add(EI_ELR.class, false, 1, 0, new Object[]{getMessage()}, "Placer Order Number");
      this.add(EI_ELR.class, true, 1, 0, new Object[]{getMessage()}, "Filler Order Number");
      this.add(EI_ELR.class, false, 1, 0, new Object[]{getMessage()}, "Placer Group Number");
      this.add(ID.class, false, 1, 0, new Object[]{getMessage(), Integer.valueOf(38)}, "Order Status");
      this.add(ID.class, false, 1, 0, new Object[]{getMessage(), Integer.valueOf(121)}, "Response Flag");
      this.add(TQ.class, false, 0, 0, new Object[]{getMessage()}, "Quantity/Timing");
      this.add(EIP.class, false, 1, 0, new Object[]{getMessage()}, "Parent");
      this.add(TS.class, false, 1, 0, new Object[]{getMessage()}, "Date/Time of Transaction");
      this.add(XCN.class, false, -1, 0, new Object[]{getMessage()}, "Entered By");
      this.add(XCN.class, false, -1, 0, new Object[]{getMessage()}, "Verified By");
      this.add(XCN_ELR.class, false, -1, 0, new Object[]{getMessage()}, "Ordering Provider");
      this.add(PL.class, false, 1, 0, new Object[]{getMessage()}, "Enterer's Location");
      this.add(XTN_ELR.class, false, 2, 0, new Object[]{getMessage()}, "Call Back Phone Number");
      this.add(TS.class, false, 1, 0, new Object[]{getMessage()}, "Order Effective Date/Time");
      this.add(CWE.class, false, 1, 0, new Object[]{getMessage()}, "Order Control Code Reason");
      this.add(CWE.class, false, 1, 0, new Object[]{getMessage()}, "Entering Organization");
      this.add(CWE.class, false, 1, 0, new Object[]{getMessage()}, "Entering Device");
      this.add(XCN.class, false, -1, 0, new Object[]{getMessage()}, "Action By");
      this.add(CWE.class, false, 0, 0, new Object[]{getMessage()}, "Advanced Beneficiary Notice Code");
      this.add(XON_ELR.class, true, 1, 0, new Object[]{getMessage()}, "Ordering Facility Name");
      this.add(XAD_ELR.class, true, 1, 0, new Object[]{getMessage()}, "Ordering Facility Address");
      this.add(XTN_ELR.class, true, -1, 0, new Object[]{getMessage()}, "Ordering Facility Phone Number");
      this.add(XAD_ELR.class, false, -1, 0, new Object[]{getMessage()}, "Ordering Provider Address");
      this.add(CWE.class, false, 1, 0, new Object[]{getMessage()}, "Order Status Modifier");
      this.add(CWE.class, false, 0, 0, new Object[]{getMessage()}, "Advanced Beneficiary Notice Override Reason");
      this.add(TS.class, false, 1, 0, new Object[]{getMessage()}, "Filler's Expected Availability Date/Time");
      this.add(CWE.class, false, 1, 0, new Object[]{getMessage()}, "Confidentiality Code");
      this.add(CWE.class, false, 1, 0, new Object[]{getMessage()}, "Order Type");
      this.add(CNE.class, false, 1, 0, new Object[]{getMessage()}, "Enterer Authorization Mode");
      this.add(CWE.class, false, 1, 0, new Object[]{getMessage()}, "Parent Universal Service Identifier");
    } catch (HL7Exception e) {
      log.error("Unexpected error creating ORC - this is probably a bug in the source code generator.", e);
    }
  }


  /**
   * Returns
   * ORC-1: "Order Control" - creates it if necessary
   */
  public ID getOrderControl() {
    ID retVal = this.getTypedField(1, 0);
    return retVal;
  }

  /**
   * Returns
   * ORC-1: "Order Control" - creates it if necessary
   */
  public ID getOrc1_OrderControl() {
    ID retVal = this.getTypedField(1, 0);
    return retVal;
  }


  /**
   * Returns
   * ORC-2: "Placer Order Number" - creates it if necessary
   */
  public EI_ELR getPlacerOrderNumber() {
    EI_ELR retVal = this.getTypedField(2, 0);
    return retVal;
  }

  /**
   * Returns
   * ORC-2: "Placer Order Number" - creates it if necessary
   */
  public EI_ELR getOrc2_PlacerOrderNumber() {
    EI_ELR retVal = this.getTypedField(2, 0);
    return retVal;
  }


  /**
   * Returns
   * ORC-3: "Filler Order Number" - creates it if necessary
   */
  public EI_ELR getFillerOrderNumber() {
    EI_ELR retVal = this.getTypedField(3, 0);
    return retVal;
  }

  /**
   * Returns
   * ORC-3: "Filler Order Number" - creates it if necessary
   */
  public EI_ELR getOrc3_FillerOrderNumber() {
    EI_ELR retVal = this.getTypedField(3, 0);
    return retVal;
  }


  /**
   * Returns
   * ORC-4: "Placer Group Number" - creates it if necessary
   */
  public EI_ELR getPlacerGroupNumber() {
    EI_ELR retVal = this.getTypedField(4, 0);
    return retVal;
  }

  /**
   * Returns
   * ORC-4: "Placer Group Number" - creates it if necessary
   */
  public EI_ELR getOrc4_PlacerGroupNumber() {
    EI_ELR retVal = this.getTypedField(4, 0);
    return retVal;
  }


  /**
   * Returns
   * ORC-5: "Order Status" - creates it if necessary
   */
  public ID getOrderStatus() {
    ID retVal = this.getTypedField(5, 0);
    return retVal;
  }

  /**
   * Returns
   * ORC-5: "Order Status" - creates it if necessary
   */
  public ID getOrc5_OrderStatus() {
    ID retVal = this.getTypedField(5, 0);
    return retVal;
  }


  /**
   * Returns
   * ORC-6: "Response Flag" - creates it if necessary
   */
  public ID getResponseFlag() {
    ID retVal = this.getTypedField(6, 0);
    return retVal;
  }

  /**
   * Returns
   * ORC-6: "Response Flag" - creates it if necessary
   */
  public ID getOrc6_ResponseFlag() {
    ID retVal = this.getTypedField(6, 0);
    return retVal;
  }


  /**
   * Returns all repetitions of Quantity/Timing (ORC-7).
   */
  public TQ[] getQuantityTiming() {
    TQ[] retVal = this.getTypedField(7, new TQ[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Quantity/Timing (ORC-7).
   */
  public TQ[] getOrc7_QuantityTiming() {
    TQ[] retVal = this.getTypedField(7, new TQ[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Quantity/Timing (ORC-7).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getQuantityTimingReps() {
    return this.getReps(7);
  }


  /**
   * Returns a specific repetition of
   * ORC-7: "Quantity/Timing" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public TQ getQuantityTiming(int rep) {
    TQ retVal = this.getTypedField(7, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * ORC-7: "Quantity/Timing" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public TQ getOrc7_QuantityTiming(int rep) {
    TQ retVal = this.getTypedField(7, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Quantity/Timing (ORC-7).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getOrc7_QuantityTimingReps() {
    return this.getReps(7);
  }


  /**
   * Inserts a repetition of
   * ORC-7: "Quantity/Timing" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public TQ insertQuantityTiming(int rep) throws HL7Exception {
    return (TQ) super.insertRepetition(7, rep);
  }


  /**
   * Inserts a repetition of
   * ORC-7: "Quantity/Timing" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public TQ insertOrc7_QuantityTiming(int rep) throws HL7Exception {
    return (TQ) super.insertRepetition(7, rep);
  }


  /**
   * Removes a repetition of
   * ORC-7: "Quantity/Timing" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public TQ removeQuantityTiming(int rep) throws HL7Exception {
    return (TQ) super.removeRepetition(7, rep);
  }


  /**
   * Removes a repetition of
   * ORC-7: "Quantity/Timing" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public TQ removeOrc7_QuantityTiming(int rep) throws HL7Exception {
    return (TQ) super.removeRepetition(7, rep);
  }


  /**
   * Returns
   * ORC-8: "Parent" - creates it if necessary
   */
  public EIP getORCParent() {
    EIP retVal = this.getTypedField(8, 0);
    return retVal;
  }

  /**
   * Returns
   * ORC-8: "Parent" - creates it if necessary
   */
  public EIP getOrc8_Parent() {
    EIP retVal = this.getTypedField(8, 0);
    return retVal;
  }


  /**
   * Returns
   * ORC-9: "Date/Time of Transaction" - creates it if necessary
   */
  public TS getDateTimeOfTransaction() {
    TS retVal = this.getTypedField(9, 0);
    return retVal;
  }

  /**
   * Returns
   * ORC-9: "Date/Time of Transaction" - creates it if necessary
   */
  public TS getOrc9_DateTimeOfTransaction() {
    TS retVal = this.getTypedField(9, 0);
    return retVal;
  }


  /**
   * Returns all repetitions of Entered By (ORC-10).
   */
  public XCN[] getEnteredBy() {
    XCN[] retVal = this.getTypedField(10, new XCN[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Entered By (ORC-10).
   */
  public XCN[] getOrc10_EnteredBy() {
    XCN[] retVal = this.getTypedField(10, new XCN[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Entered By (ORC-10).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getEnteredByReps() {
    return this.getReps(10);
  }


  /**
   * Returns a specific repetition of
   * ORC-10: "Entered By" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XCN getEnteredBy(int rep) {
    XCN retVal = this.getTypedField(10, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * ORC-10: "Entered By" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XCN getOrc10_EnteredBy(int rep) {
    XCN retVal = this.getTypedField(10, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Entered By (ORC-10).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getOrc10_EnteredByReps() {
    return this.getReps(10);
  }


  /**
   * Inserts a repetition of
   * ORC-10: "Entered By" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN insertEnteredBy(int rep) throws HL7Exception {
    return (XCN) super.insertRepetition(10, rep);
  }


  /**
   * Inserts a repetition of
   * ORC-10: "Entered By" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN insertOrc10_EnteredBy(int rep) throws HL7Exception {
    return (XCN) super.insertRepetition(10, rep);
  }


  /**
   * Removes a repetition of
   * ORC-10: "Entered By" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN removeEnteredBy(int rep) throws HL7Exception {
    return (XCN) super.removeRepetition(10, rep);
  }


  /**
   * Removes a repetition of
   * ORC-10: "Entered By" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN removeOrc10_EnteredBy(int rep) throws HL7Exception {
    return (XCN) super.removeRepetition(10, rep);
  }


  /**
   * Returns all repetitions of Verified By (ORC-11).
   */
  public XCN[] getVerifiedBy() {
    XCN[] retVal = this.getTypedField(11, new XCN[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Verified By (ORC-11).
   */
  public XCN[] getOrc11_VerifiedBy() {
    XCN[] retVal = this.getTypedField(11, new XCN[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Verified By (ORC-11).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getVerifiedByReps() {
    return this.getReps(11);
  }


  /**
   * Returns a specific repetition of
   * ORC-11: "Verified By" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XCN getVerifiedBy(int rep) {
    XCN retVal = this.getTypedField(11, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * ORC-11: "Verified By" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XCN getOrc11_VerifiedBy(int rep) {
    XCN retVal = this.getTypedField(11, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Verified By (ORC-11).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getOrc11_VerifiedByReps() {
    return this.getReps(11);
  }


  /**
   * Inserts a repetition of
   * ORC-11: "Verified By" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN insertVerifiedBy(int rep) throws HL7Exception {
    return (XCN) super.insertRepetition(11, rep);
  }


  /**
   * Inserts a repetition of
   * ORC-11: "Verified By" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN insertOrc11_VerifiedBy(int rep) throws HL7Exception {
    return (XCN) super.insertRepetition(11, rep);
  }


  /**
   * Removes a repetition of
   * ORC-11: "Verified By" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN removeVerifiedBy(int rep) throws HL7Exception {
    return (XCN) super.removeRepetition(11, rep);
  }


  /**
   * Removes a repetition of
   * ORC-11: "Verified By" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN removeOrc11_VerifiedBy(int rep) throws HL7Exception {
    return (XCN) super.removeRepetition(11, rep);
  }


  /**
   * Returns all repetitions of Ordering Provider (ORC-12).
   */
  public XCN_ELR[] getOrderingProvider() {
    XCN_ELR[] retVal = this.getTypedField(12, new XCN_ELR[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Ordering Provider (ORC-12).
   */
  public XCN_ELR[] getOrc12_OrderingProvider() {
    XCN_ELR[] retVal = this.getTypedField(12, new XCN_ELR[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Ordering Provider (ORC-12).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getOrderingProviderReps() {
    return this.getReps(12);
  }


  /**
   * Returns a specific repetition of
   * ORC-12: "Ordering Provider" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XCN_ELR getOrderingProvider(int rep) {
    XCN_ELR retVal = this.getTypedField(12, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * ORC-12: "Ordering Provider" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XCN_ELR getOrc12_OrderingProvider(int rep) {
    XCN_ELR retVal = this.getTypedField(12, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Ordering Provider (ORC-12).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getOrc12_OrderingProviderReps() {
    return this.getReps(12);
  }


  /**
   * Inserts a repetition of
   * ORC-12: "Ordering Provider" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN_ELR insertOrderingProvider(int rep) throws HL7Exception {
    return (XCN_ELR) super.insertRepetition(12, rep);
  }


  /**
   * Inserts a repetition of
   * ORC-12: "Ordering Provider" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN_ELR insertOrc12_OrderingProvider(int rep) throws HL7Exception {
    return (XCN_ELR) super.insertRepetition(12, rep);
  }


  /**
   * Removes a repetition of
   * ORC-12: "Ordering Provider" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN_ELR removeOrderingProvider(int rep) throws HL7Exception {
    return (XCN_ELR) super.removeRepetition(12, rep);
  }


  /**
   * Removes a repetition of
   * ORC-12: "Ordering Provider" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN_ELR removeOrc12_OrderingProvider(int rep) throws HL7Exception {
    return (XCN_ELR) super.removeRepetition(12, rep);
  }


  /**
   * Returns
   * ORC-13: "Enterer's Location" - creates it if necessary
   */
  public PL getEntererSLocation() {
    PL retVal = this.getTypedField(13, 0);
    return retVal;
  }

  /**
   * Returns
   * ORC-13: "Enterer's Location" - creates it if necessary
   */
  public PL getOrc13_EntererSLocation() {
    PL retVal = this.getTypedField(13, 0);
    return retVal;
  }


  /**
   * Returns all repetitions of Call Back Phone Number (ORC-14).
   */
  public XTN_ELR[] getCallBackPhoneNumber() {
    XTN_ELR[] retVal = this.getTypedField(14, new XTN_ELR[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Call Back Phone Number (ORC-14).
   */
  public XTN_ELR[] getOrc14_CallBackPhoneNumber() {
    XTN_ELR[] retVal = this.getTypedField(14, new XTN_ELR[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Call Back Phone Number (ORC-14).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getCallBackPhoneNumberReps() {
    return this.getReps(14);
  }


  /**
   * Returns a specific repetition of
   * ORC-14: "Call Back Phone Number" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XTN_ELR getCallBackPhoneNumber(int rep) {
    XTN_ELR retVal = this.getTypedField(14, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * ORC-14: "Call Back Phone Number" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XTN_ELR getOrc14_CallBackPhoneNumber(int rep) {
    XTN_ELR retVal = this.getTypedField(14, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Call Back Phone Number (ORC-14).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getOrc14_CallBackPhoneNumberReps() {
    return this.getReps(14);
  }


  /**
   * Inserts a repetition of
   * ORC-14: "Call Back Phone Number" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XTN_ELR insertCallBackPhoneNumber(int rep) throws HL7Exception {
    return (XTN_ELR) super.insertRepetition(14, rep);
  }


  /**
   * Inserts a repetition of
   * ORC-14: "Call Back Phone Number" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XTN_ELR insertOrc14_CallBackPhoneNumber(int rep) throws HL7Exception {
    return (XTN_ELR) super.insertRepetition(14, rep);
  }


  /**
   * Removes a repetition of
   * ORC-14: "Call Back Phone Number" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XTN_ELR removeCallBackPhoneNumber(int rep) throws HL7Exception {
    return (XTN_ELR) super.removeRepetition(14, rep);
  }


  /**
   * Removes a repetition of
   * ORC-14: "Call Back Phone Number" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XTN_ELR removeOrc14_CallBackPhoneNumber(int rep) throws HL7Exception {
    return (XTN_ELR) super.removeRepetition(14, rep);
  }


  /**
   * Returns
   * ORC-15: "Order Effective Date/Time" - creates it if necessary
   */
  public TS getOrderEffectiveDateTime() {
    TS retVal = this.getTypedField(15, 0);
    return retVal;
  }

  /**
   * Returns
   * ORC-15: "Order Effective Date/Time" - creates it if necessary
   */
  public TS getOrc15_OrderEffectiveDateTime() {
    TS retVal = this.getTypedField(15, 0);
    return retVal;
  }


  /**
   * Returns
   * ORC-16: "Order Control Code Reason" - creates it if necessary
   */
  public CWE getOrderControlCodeReason() {
    CWE retVal = this.getTypedField(16, 0);
    return retVal;
  }

  /**
   * Returns
   * ORC-16: "Order Control Code Reason" - creates it if necessary
   */
  public CWE getOrc16_OrderControlCodeReason() {
    CWE retVal = this.getTypedField(16, 0);
    return retVal;
  }


  /**
   * Returns
   * ORC-17: "Entering Organization" - creates it if necessary
   */
  public CWE getEnteringOrganization() {
    CWE retVal = this.getTypedField(17, 0);
    return retVal;
  }

  /**
   * Returns
   * ORC-17: "Entering Organization" - creates it if necessary
   */
  public CWE getOrc17_EnteringOrganization() {
    CWE retVal = this.getTypedField(17, 0);
    return retVal;
  }


  /**
   * Returns
   * ORC-18: "Entering Device" - creates it if necessary
   */
  public CWE getEnteringDevice() {
    CWE retVal = this.getTypedField(18, 0);
    return retVal;
  }

  /**
   * Returns
   * ORC-18: "Entering Device" - creates it if necessary
   */
  public CWE getOrc18_EnteringDevice() {
    CWE retVal = this.getTypedField(18, 0);
    return retVal;
  }


  /**
   * Returns all repetitions of Action By (ORC-19).
   */
  public XCN[] getActionBy() {
    XCN[] retVal = this.getTypedField(19, new XCN[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Action By (ORC-19).
   */
  public XCN[] getOrc19_ActionBy() {
    XCN[] retVal = this.getTypedField(19, new XCN[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Action By (ORC-19).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getActionByReps() {
    return this.getReps(19);
  }


  /**
   * Returns a specific repetition of
   * ORC-19: "Action By" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XCN getActionBy(int rep) {
    XCN retVal = this.getTypedField(19, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * ORC-19: "Action By" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XCN getOrc19_ActionBy(int rep) {
    XCN retVal = this.getTypedField(19, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Action By (ORC-19).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getOrc19_ActionByReps() {
    return this.getReps(19);
  }


  /**
   * Inserts a repetition of
   * ORC-19: "Action By" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN insertActionBy(int rep) throws HL7Exception {
    return (XCN) super.insertRepetition(19, rep);
  }


  /**
   * Inserts a repetition of
   * ORC-19: "Action By" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN insertOrc19_ActionBy(int rep) throws HL7Exception {
    return (XCN) super.insertRepetition(19, rep);
  }


  /**
   * Removes a repetition of
   * ORC-19: "Action By" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN removeActionBy(int rep) throws HL7Exception {
    return (XCN) super.removeRepetition(19, rep);
  }


  /**
   * Removes a repetition of
   * ORC-19: "Action By" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XCN removeOrc19_ActionBy(int rep) throws HL7Exception {
    return (XCN) super.removeRepetition(19, rep);
  }


  /**
   * Returns all repetitions of Advanced Beneficiary Notice Code (ORC-20).
   */
  public CWE[] getAdvancedBeneficiaryNoticeCode() {
    CWE[] retVal = this.getTypedField(20, new CWE[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Advanced Beneficiary Notice Code (ORC-20).
   */
  public CWE[] getOrc20_AdvancedBeneficiaryNoticeCode() {
    CWE[] retVal = this.getTypedField(20, new CWE[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Advanced Beneficiary Notice Code (ORC-20).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getAdvancedBeneficiaryNoticeCodeReps() {
    return this.getReps(20);
  }


  /**
   * Returns a specific repetition of
   * ORC-20: "Advanced Beneficiary Notice Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getAdvancedBeneficiaryNoticeCode(int rep) {
    CWE retVal = this.getTypedField(20, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * ORC-20: "Advanced Beneficiary Notice Code" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getOrc20_AdvancedBeneficiaryNoticeCode(int rep) {
    CWE retVal = this.getTypedField(20, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Advanced Beneficiary Notice Code (ORC-20).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getOrc20_AdvancedBeneficiaryNoticeCodeReps() {
    return this.getReps(20);
  }


  /**
   * Inserts a repetition of
   * ORC-20: "Advanced Beneficiary Notice Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertAdvancedBeneficiaryNoticeCode(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(20, rep);
  }


  /**
   * Inserts a repetition of
   * ORC-20: "Advanced Beneficiary Notice Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertOrc20_AdvancedBeneficiaryNoticeCode(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(20, rep);
  }


  /**
   * Removes a repetition of
   * ORC-20: "Advanced Beneficiary Notice Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeAdvancedBeneficiaryNoticeCode(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(20, rep);
  }


  /**
   * Removes a repetition of
   * ORC-20: "Advanced Beneficiary Notice Code" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeOrc20_AdvancedBeneficiaryNoticeCode(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(20, rep);
  }


  /**
   * Returns
   * ORC-21: "Ordering Facility Name" - creates it if necessary
   */
  public XON_ELR getOrderingFacilityName() {
    XON_ELR retVal = this.getTypedField(21, 0);
    return retVal;
  }

  /**
   * Returns
   * ORC-21: "Ordering Facility Name" - creates it if necessary
   */
  public XON_ELR getOrc21_OrderingFacilityName() {
    XON_ELR retVal = this.getTypedField(21, 0);
    return retVal;
  }


  /**
   * Returns
   * ORC-22: "Ordering Facility Address" - creates it if necessary
   */
  public XAD_ELR getOrderingFacilityAddress() {
    XAD_ELR retVal = this.getTypedField(22, 0);
    return retVal;
  }

  /**
   * Returns
   * ORC-22: "Ordering Facility Address" - creates it if necessary
   */
  public XAD_ELR getOrc22_OrderingFacilityAddress() {
    XAD_ELR retVal = this.getTypedField(22, 0);
    return retVal;
  }


  /**
   * Returns all repetitions of Ordering Facility Phone Number (ORC-23).
   */
  public XTN_ELR[] getOrderingFacilityPhoneNumber() {
    XTN_ELR[] retVal = this.getTypedField(23, new XTN_ELR[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Ordering Facility Phone Number (ORC-23).
   */
  public XTN_ELR[] getOrc23_OrderingFacilityPhoneNumber() {
    XTN_ELR[] retVal = this.getTypedField(23, new XTN_ELR[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Ordering Facility Phone Number (ORC-23).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getOrderingFacilityPhoneNumberReps() {
    return this.getReps(23);
  }


  /**
   * Returns a specific repetition of
   * ORC-23: "Ordering Facility Phone Number" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XTN_ELR getOrderingFacilityPhoneNumber(int rep) {
    XTN_ELR retVal = this.getTypedField(23, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * ORC-23: "Ordering Facility Phone Number" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XTN_ELR getOrc23_OrderingFacilityPhoneNumber(int rep) {
    XTN_ELR retVal = this.getTypedField(23, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Ordering Facility Phone Number (ORC-23).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getOrc23_OrderingFacilityPhoneNumberReps() {
    return this.getReps(23);
  }


  /**
   * Inserts a repetition of
   * ORC-23: "Ordering Facility Phone Number" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XTN_ELR insertOrderingFacilityPhoneNumber(int rep) throws HL7Exception {
    return (XTN_ELR) super.insertRepetition(23, rep);
  }


  /**
   * Inserts a repetition of
   * ORC-23: "Ordering Facility Phone Number" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XTN_ELR insertOrc23_OrderingFacilityPhoneNumber(int rep) throws HL7Exception {
    return (XTN_ELR) super.insertRepetition(23, rep);
  }


  /**
   * Removes a repetition of
   * ORC-23: "Ordering Facility Phone Number" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XTN_ELR removeOrderingFacilityPhoneNumber(int rep) throws HL7Exception {
    return (XTN_ELR) super.removeRepetition(23, rep);
  }


  /**
   * Removes a repetition of
   * ORC-23: "Ordering Facility Phone Number" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XTN_ELR removeOrc23_OrderingFacilityPhoneNumber(int rep) throws HL7Exception {
    return (XTN_ELR) super.removeRepetition(23, rep);
  }


  /**
   * Returns all repetitions of Ordering Provider Address (ORC-24).
   */
  public XAD_ELR[] getOrderingProviderAddress() {
    XAD_ELR[] retVal = this.getTypedField(24, new XAD_ELR[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Ordering Provider Address (ORC-24).
   */
  public XAD_ELR[] getOrc24_OrderingProviderAddress() {
    XAD_ELR[] retVal = this.getTypedField(24, new XAD_ELR[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Ordering Provider Address (ORC-24).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getOrderingProviderAddressReps() {
    return this.getReps(24);
  }


  /**
   * Returns a specific repetition of
   * ORC-24: "Ordering Provider Address" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XAD_ELR getOrderingProviderAddress(int rep) {
    XAD_ELR retVal = this.getTypedField(24, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * ORC-24: "Ordering Provider Address" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public XAD_ELR getOrc24_OrderingProviderAddress(int rep) {
    XAD_ELR retVal = this.getTypedField(24, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Ordering Provider Address (ORC-24).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getOrc24_OrderingProviderAddressReps() {
    return this.getReps(24);
  }


  /**
   * Inserts a repetition of
   * ORC-24: "Ordering Provider Address" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XAD_ELR insertOrderingProviderAddress(int rep) throws HL7Exception {
    return (XAD_ELR) super.insertRepetition(24, rep);
  }


  /**
   * Inserts a repetition of
   * ORC-24: "Ordering Provider Address" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XAD_ELR insertOrc24_OrderingProviderAddress(int rep) throws HL7Exception {
    return (XAD_ELR) super.insertRepetition(24, rep);
  }


  /**
   * Removes a repetition of
   * ORC-24: "Ordering Provider Address" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XAD_ELR removeOrderingProviderAddress(int rep) throws HL7Exception {
    return (XAD_ELR) super.removeRepetition(24, rep);
  }


  /**
   * Removes a repetition of
   * ORC-24: "Ordering Provider Address" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public XAD_ELR removeOrc24_OrderingProviderAddress(int rep) throws HL7Exception {
    return (XAD_ELR) super.removeRepetition(24, rep);
  }


  /**
   * Returns
   * ORC-25: "Order Status Modifier" - creates it if necessary
   */
  public CWE getOrderStatusModifier() {
    CWE retVal = this.getTypedField(25, 0);
    return retVal;
  }

  /**
   * Returns
   * ORC-25: "Order Status Modifier" - creates it if necessary
   */
  public CWE getOrc25_OrderStatusModifier() {
    CWE retVal = this.getTypedField(25, 0);
    return retVal;
  }


  /**
   * Returns all repetitions of Advanced Beneficiary Notice Override Reason (ORC-26).
   */
  public CWE[] getAdvancedBeneficiaryNoticeOverrideReason() {
    CWE[] retVal = this.getTypedField(26, new CWE[0]);
    return retVal;
  }


  /**
   * Returns all repetitions of Advanced Beneficiary Notice Override Reason (ORC-26).
   */
  public CWE[] getOrc26_AdvancedBeneficiaryNoticeOverrideReason() {
    CWE[] retVal = this.getTypedField(26, new CWE[0]);
    return retVal;
  }


  /**
   * Returns a count of the current number of repetitions of Advanced Beneficiary Notice Override Reason (ORC-26).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getAdvancedBeneficiaryNoticeOverrideReasonReps() {
    return this.getReps(26);
  }


  /**
   * Returns a specific repetition of
   * ORC-26: "Advanced Beneficiary Notice Override Reason" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getAdvancedBeneficiaryNoticeOverrideReason(int rep) {
    CWE retVal = this.getTypedField(26, rep);
    return retVal;
  }

  /**
   * Returns a specific repetition of
   * ORC-26: "Advanced Beneficiary Notice Override Reason" - creates it if necessary
   *
   * @param rep The repetition index (0-indexed)
   */
  public CWE getOrc26_AdvancedBeneficiaryNoticeOverrideReason(int rep) {
    CWE retVal = this.getTypedField(26, rep);
    return retVal;
  }

  /**
   * Returns a count of the current number of repetitions of Advanced Beneficiary Notice Override Reason (ORC-26).
   * This method does not create a repetition, so if no repetitions have currently been defined or accessed,
   * it will return zero.
   */
  public int getOrc26_AdvancedBeneficiaryNoticeOverrideReasonReps() {
    return this.getReps(26);
  }


  /**
   * Inserts a repetition of
   * ORC-26: "Advanced Beneficiary Notice Override Reason" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertAdvancedBeneficiaryNoticeOverrideReason(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(26, rep);
  }


  /**
   * Inserts a repetition of
   * ORC-26: "Advanced Beneficiary Notice Override Reason" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE insertOrc26_AdvancedBeneficiaryNoticeOverrideReason(int rep) throws HL7Exception {
    return (CWE) super.insertRepetition(26, rep);
  }


  /**
   * Removes a repetition of
   * ORC-26: "Advanced Beneficiary Notice Override Reason" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeAdvancedBeneficiaryNoticeOverrideReason(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(26, rep);
  }


  /**
   * Removes a repetition of
   * ORC-26: "Advanced Beneficiary Notice Override Reason" at a specific index
   *
   * @param rep The repetition index (0-indexed)
   * @throws HL7Exception If the rep is invalid (below 0, or too high for the allowable repetitions)
   */
  public CWE removeOrc26_AdvancedBeneficiaryNoticeOverrideReason(int rep) throws HL7Exception {
    return (CWE) super.removeRepetition(26, rep);
  }


  /**
   * Returns
   * ORC-27: "Filler's Expected Availability Date/Time" - creates it if necessary
   */
  public TS getFillerSExpectedAvailabilityDateTime() {
    TS retVal = this.getTypedField(27, 0);
    return retVal;
  }

  /**
   * Returns
   * ORC-27: "Filler's Expected Availability Date/Time" - creates it if necessary
   */
  public TS getOrc27_FillerSExpectedAvailabilityDateTime() {
    TS retVal = this.getTypedField(27, 0);
    return retVal;
  }


  /**
   * Returns
   * ORC-28: "Confidentiality Code" - creates it if necessary
   */
  public CWE getConfidentialityCode() {
    CWE retVal = this.getTypedField(28, 0);
    return retVal;
  }

  /**
   * Returns
   * ORC-28: "Confidentiality Code" - creates it if necessary
   */
  public CWE getOrc28_ConfidentialityCode() {
    CWE retVal = this.getTypedField(28, 0);
    return retVal;
  }


  /**
   * Returns
   * ORC-29: "Order Type" - creates it if necessary
   */
  public CWE getOrderType() {
    CWE retVal = this.getTypedField(29, 0);
    return retVal;
  }

  /**
   * Returns
   * ORC-29: "Order Type" - creates it if necessary
   */
  public CWE getOrc29_OrderType() {
    CWE retVal = this.getTypedField(29, 0);
    return retVal;
  }


  /**
   * Returns
   * ORC-30: "Enterer Authorization Mode" - creates it if necessary
   */
  public CNE getEntererAuthorizationMode() {
    CNE retVal = this.getTypedField(30, 0);
    return retVal;
  }

  /**
   * Returns
   * ORC-30: "Enterer Authorization Mode" - creates it if necessary
   */
  public CNE getOrc30_EntererAuthorizationMode() {
    CNE retVal = this.getTypedField(30, 0);
    return retVal;
  }


  /**
   * Returns
   * ORC-31: "Parent Universal Service Identifier" - creates it if necessary
   */
  public CWE getParentUniversalServiceIdentifier() {
    CWE retVal = this.getTypedField(31, 0);
    return retVal;
  }

  /**
   * Returns
   * ORC-31: "Parent Universal Service Identifier" - creates it if necessary
   */
  public CWE getOrc31_ParentUniversalServiceIdentifier() {
    CWE retVal = this.getTypedField(31, 0);
    return retVal;
  }


  /**
   * {@inheritDoc}
   */
  protected Type createNewTypeWithoutReflection(int field) {
    switch (field) {
      case 0:
        return new ID(getMessage(), 119);
      case 1:
        return new EI_ELR(getMessage());
      case 2:
        return new EI_ELR(getMessage());
      case 3:
        return new EI_ELR(getMessage());
      case 4:
        return new ID(getMessage(), 38);
      case 5:
        return new ID(getMessage(), 121);
      case 6:
        return new TQ(getMessage());
      case 7:
        return new EIP(getMessage());
      case 8:
        return new TS(getMessage());
      case 9:
        return new XCN(getMessage());
      case 10:
        return new XCN(getMessage());
      case 11:
        return new XCN_ELR(getMessage());
      case 12:
        return new PL(getMessage());
      case 13:
        return new XTN_ELR(getMessage());
      case 14:
        return new TS(getMessage());
      case 15:
        return new CWE(getMessage());
      case 16:
        return new CWE(getMessage());
      case 17:
        return new CWE(getMessage());
      case 18:
        return new XCN(getMessage());
      case 19:
        return new CWE(getMessage());
      case 20:
        return new XON_ELR(getMessage());
      case 21:
        return new XAD_ELR(getMessage());
      case 22:
        return new XTN_ELR(getMessage());
      case 23:
        return new XAD_ELR(getMessage());
      case 24:
        return new CWE(getMessage());
      case 25:
        return new CWE(getMessage());
      case 26:
        return new TS(getMessage());
      case 27:
        return new CWE(getMessage());
      case 28:
        return new CWE(getMessage());
      case 29:
        return new CNE(getMessage());
      case 30:
        return new CWE(getMessage());
      default:
        return null;
    }
  }


}

