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
 * The Original Code is "SN.java".  Description:
 * "Composite class SN"
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
 * <p>Represents an HL7 SN (Structured Numeric) data type.
 * This type consists of the following components:</p>
 * <ul>
 * <li>Comparator (ST)
 * <li>Num1 (NM)
 * <li>Separator/Suffix (ST)
 * <li>Num2 (NM)
 * </ul>
 */
@SuppressWarnings("unused")
public class SN_ELR extends AbstractComposite {

  private Type[] data;

  /**
   * Creates a new SN type
   */
  public SN_ELR(Message message) {
    super(message);
    init();
  }

  private void init() {
    data = new Type[4];
    data[0] = new ST(getMessage());
    data[1] = new NM(getMessage());
    data[2] = new ST(getMessage());
    data[3] = new NM(getMessage());
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
   * Returns Comparator (component 1).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public ST getComparator() {
    return getTyped(0, ST.class);
  }


  /**
   * Returns Comparator (component 1).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public ST getSn1_Comparator() {
    return getTyped(0, ST.class);
  }


  /**
   * Returns Num1 (component 2).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NM getNum1() {
    return getTyped(1, NM.class);
  }


  /**
   * Returns Num1 (component 2).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NM getSn2_Num1() {
    return getTyped(1, NM.class);
  }


  /**
   * Returns Separator/Suffix (component 3).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public ST getSeparatorSuffix() {
    return getTyped(2, ST.class);
  }


  /**
   * Returns Separator/Suffix (component 3).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public ST getSn3_SeparatorSuffix() {
    return getTyped(2, ST.class);
  }


  /**
   * Returns Num2 (component 4).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NM getNum2() {
    return getTyped(3, NM.class);
  }


  /**
   * Returns Num2 (component 4).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NM getSn4_Num2() {
    return getTyped(3, NM.class);
  }


}

