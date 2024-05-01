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
 * The Original Code is "CWE.java".  Description:
 * "Composite class CWE"
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

package fhirengine.translation.hl7.structures.radxmars251.datatype;


import ca.uhn.hl7v2.model.AbstractComposite;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.Type;
import ca.uhn.hl7v2.model.primitive.NULLDT;
import ca.uhn.hl7v2.model.v251.datatype.ID;
import ca.uhn.hl7v2.model.v251.datatype.ST;

/**
 * <p>Represents an HL7 CWE (Coded with Exceptions) data type.
 * This type consists of the following components:</p>
 * <ul>
 * <li>Identifier (ST)
 * <li>Text (ST)
 * <li>Name of Coding System (ID)
 * <li>Alternate Identifier (ST)
 * <li>Alternate Text (ST)
 * <li>Name of Alternate Coding System (ID)
 * <li>Coding System Version ID (ST)
 * <li>Alternate Coding System Version ID (ST)
 * <li>Original Text (ST)
 * </ul>
 */
@SuppressWarnings("unused")
public class CWE_OBX_15 extends AbstractComposite {

  private Type[] data;

  /**
   * Creates a new CWE type
   */
  public CWE_OBX_15(Message message) {
    super(message);
    init();
  }

  private void init() {
    data = new Type[22];
    data[0] = new ST(getMessage());
    data[1] = new NULLDT(getMessage());
    data[2] = new NULLDT(getMessage());
    data[3] = new NULLDT(getMessage());
    data[4] = new NULLDT(getMessage());
    data[5] = new NULLDT(getMessage());
    data[6] = new NULLDT(getMessage());
    data[7] = new NULLDT(getMessage());
    data[8] = new NULLDT(getMessage());
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
   * Returns Identifier (component 1).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public ST getIdentifier() {
    return getTyped(0, ST.class);
  }


  /**
   * Returns Identifier (component 1).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public ST getCwe1_Identifier() {
    return getTyped(0, ST.class);
  }


  /**
   * Returns Text (component 2).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NULLDT getText() {
    return getTyped(1, NULLDT.class);
  }


  /**
   * Returns Text (component 2).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NULLDT getCwe2_Text() {
    return getTyped(1, NULLDT.class);
  }


  /**
   * Returns Name of Coding System (component 3).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NULLDT getNameOfCodingSystem() {
    return getTyped(2, NULLDT.class);
  }


  /**
   * Returns Name of Coding System (component 3).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NULLDT getCwe3_NameOfCodingSystem() {
    return getTyped(2, NULLDT.class);
  }


  /**
   * Returns Alternate Identifier (component 4).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NULLDT getAlternateIdentifier() {
    return getTyped(3, NULLDT.class);
  }


  /**
   * Returns Alternate Identifier (component 4).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NULLDT getCwe4_AlternateIdentifier() {
    return getTyped(3, NULLDT.class);
  }


  /**
   * Returns Alternate Text (component 5).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NULLDT getAlternateText() {
    return getTyped(4, NULLDT.class);
  }


  /**
   * Returns Alternate Text (component 5).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NULLDT getCwe5_AlternateText() {
    return getTyped(4, NULLDT.class);
  }


  /**
   * Returns Name of Alternate Coding System (component 6).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NULLDT getNameOfAlternateCodingSystem() {
    return getTyped(5, NULLDT.class);
  }


  /**
   * Returns Name of Alternate Coding System (component 6).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NULLDT getCwe6_NameOfAlternateCodingSystem() {
    return getTyped(5, NULLDT.class);
  }


  /**
   * Returns Coding System Version ID (component 7).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NULLDT getCodingSystemVersionID() {
    return getTyped(6, NULLDT.class);
  }


  /**
   * Returns Coding System Version ID (component 7).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NULLDT getCwe7_CodingSystemVersionID() {
    return getTyped(6, NULLDT.class);
  }


  /**
   * Returns Alternate Coding System Version ID (component 8).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NULLDT getAlternateCodingSystemVersionID() {
    return getTyped(7, NULLDT.class);
  }


  /**
   * Returns Alternate Coding System Version ID (component 8).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NULLDT getCwe8_AlternateCodingSystemVersionID() {
    return getTyped(7, NULLDT.class);
  }


  /**
   * Returns Original Text (component 9).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NULLDT getOriginalText() {
    return getTyped(8, NULLDT.class);
  }


  /**
   * Returns Original Text (component 9).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NULLDT getCwe9_OriginalText() {
    return getTyped(8, NULLDT.class);
  }


}

