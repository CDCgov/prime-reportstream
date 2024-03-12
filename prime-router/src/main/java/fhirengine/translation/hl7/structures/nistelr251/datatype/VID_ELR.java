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
 * The Original Code is "VID.java".  Description:
 * "Composite class VID"
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
import ca.uhn.hl7v2.model.v251.datatype.CWE;
import ca.uhn.hl7v2.model.v251.datatype.ID;

/**
 * <p>Represents an HL7 VID (Version Identifier) data type.
 * This type consists of the following components:</p>
 * <ul>
 * <li>Version ID (ID)
 * <li>Internationalization Code (CE)
 * <li>International Version ID (CE)
 * </ul>
 */
@SuppressWarnings("unused")
public class VID_ELR extends AbstractComposite {

  private Type[] data;

  /**
   * Creates a new VID type
   */
  public VID_ELR(Message message) {
    super(message);
    init();
  }

  private void init() {
    data = new Type[3];
    data[0] = new ID(getMessage(), 104);
    data[1] = new CWE(getMessage());
    data[2] = new CWE(getMessage());
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
   * Returns Version ID (component 1).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public ID getVersionID() {
    return getTyped(0, ID.class);
  }


  /**
   * Returns Version ID (component 1).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public ID getVid1_VersionID() {
    return getTyped(0, ID.class);
  }


  /**
   * Returns Internationalization Code (component 2).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public CWE getInternationalizationCode() {
    return getTyped(1, CWE.class);
  }


  /**
   * Returns Internationalization Code (component 2).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public CWE getVid2_InternationalizationCode() {
    return getTyped(1, CWE.class);
  }


  /**
   * Returns International Version ID (component 3).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public CWE getInternationalVersionID() {
    return getTyped(2, CWE.class);
  }


  /**
   * Returns International Version ID (component 3).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public CWE getVid3_InternationalVersionID() {
    return getTyped(2, CWE.class);
  }


}


