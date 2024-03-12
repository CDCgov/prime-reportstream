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
 * The Original Code is "ERL.java".  Description:
 * "Composite class ERL"
 *
 * The Initial Developer of the Original Code is University Health Network. Copyright (C)
 * 2013.  All Rights Reserved.
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

package fhirengine.translation.hl7.structures.nistelr251.datatype;

import ca.uhn.hl7v2.model.AbstractComposite;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.Type;
import ca.uhn.hl7v2.model.v251.datatype.NM;
import ca.uhn.hl7v2.model.v251.datatype.ST;

/**
 * <p>Represents an HL7 ERL (Error Location) data type.
 * This type consists of the following components:</p>
 * <ul>
 * <li>Segment ID (ST)
 * <li>Segment Sequence (NM)
 * <li>Field Position (NM)
 * <li>Field Repetition (NM)
 * <li>Component Number (NM)
 * <li>Sub-Component Number (NM)
 * </ul>
 */
@SuppressWarnings("unused")
public class ERL_ELR extends AbstractComposite {

  private Type[] data;

  /**
   * Creates a new ERL type
   */
  public ERL_ELR(Message message) {
    super(message);
    init();
  }

  private void init() {
    data = new Type[6];
    data[0] = new ST(getMessage());
    data[1] = new NM(getMessage());
    data[2] = new NM(getMessage());
    data[3] = new NM(getMessage());
    data[4] = new NM(getMessage());
    data[5] = new NM(getMessage());
  }


  /**
   * Returns an array containing the data elements.
   */
  public Type[] getComponents() {
    return this.data;
  }

  /**
   * Returns an individual data component.
   *
   * @param number The component number (0-indexed)
   * @throws DataTypeException if the given element number is out of range.
   */
  public Type getComponent(int number) throws DataTypeException {

    try {
      return this.data[number];
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new DataTypeException("Element " + number + " doesn't exist (Type " + getClass().getName() + " has only " + this.data.length + " components)");
    }
  }


  /**
   * Returns Segment ID (component 1).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public ST getSegmentID() {
    return getTyped(0, ST.class);
  }


  /**
   * Returns Segment ID (component 1).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public ST getErl1_SegmentID() {
    return getTyped(0, ST.class);
  }


  /**
   * Returns Segment Sequence (component 2).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NM getSegmentSequence() {
    return getTyped(1, NM.class);
  }


  /**
   * Returns Segment Sequence (component 2).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NM getErl2_SegmentSequence() {
    return getTyped(1, NM.class);
  }


  /**
   * Returns Field Position (component 3).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NM getFieldPosition() {
    return getTyped(2, NM.class);
  }


  /**
   * Returns Field Position (component 3).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NM getErl3_FieldPosition() {
    return getTyped(2, NM.class);
  }


  /**
   * Returns Field Repetition (component 4).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NM getFieldRepetition() {
    return getTyped(3, NM.class);
  }


  /**
   * Returns Field Repetition (component 4).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NM getErl4_FieldRepetition() {
    return getTyped(3, NM.class);
  }


  /**
   * Returns Component Number (component 5).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NM getComponentNumber() {
    return getTyped(4, NM.class);
  }


  /**
   * Returns Component Number (component 5).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NM getErl5_ComponentNumber() {
    return getTyped(4, NM.class);
  }


  /**
   * Returns Sub-Component Number (component 6).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NM getSubComponentNumber() {
    return getTyped(5, NM.class);
  }


  /**
   * Returns Sub-Component Number (component 6).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NM getErl6_SubComponentNumber() {
    return getTyped(5, NM.class);
  }


}

