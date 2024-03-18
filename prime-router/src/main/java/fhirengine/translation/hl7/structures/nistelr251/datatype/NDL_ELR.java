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
 * The Original Code is "NDL.java".  Description:
 * "Composite class NDL"
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
import ca.uhn.hl7v2.model.primitive.NULLDT;


/**
 * <p>Represents an HL7 NDL (Name with Date and Location) data type.
 * This type consists of the following components:</p>
 * <ul>
 * <li>Name (CNN)
 * <li>Start Date/time (TS)
 * <li>End Date/time (TS)
 * <li>Point of Care (IS)
 * <li>Room (IS)
 * <li>Bed (IS)
 * <li>Facility (HD)
 * <li>Location Status (IS)
 * <li>Patient Location Type (IS)
 * <li>Building (IS)
 * <li>Floor (IS)
 * </ul>
 */
@SuppressWarnings("unused")
public class NDL_ELR extends AbstractComposite {

  private Type[] data;

  /**
   * Creates a new NDL type
   */
  public NDL_ELR(Message message) {
    super(message);
    init();
  }

  private void init() {
    data = new Type[11];
    data[0] = new CNN_ELR(getMessage());
    data[1] = new NULLDT(getMessage());
    data[2] = new NULLDT(getMessage());
    data[3] = new NULLDT(getMessage());
    data[4] = new NULLDT(getMessage());
    data[5] = new NULLDT(getMessage());
    data[6] = new NULLDT(getMessage());
    data[7] = new NULLDT(getMessage());
    data[8] = new NULLDT(getMessage());
    data[9] = new NULLDT(getMessage());
    data[10] = new NULLDT(getMessage());
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
   * Returns Name (component 1).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public CNN_ELR getNDLName() {
    return getTyped(0, CNN_ELR.class);
  }


  /**
   * Returns Name (component 1).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public CNN_ELR getNdl1_Name() {
    return getTyped(0, CNN_ELR.class);
  }


  /**
   * Returns Start Date/time (component 2).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NULLDT getStartDateTime() {
    return getTyped(1, NULLDT.class);
  }


  /**
   * Returns Start Date/time (component 2).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NULLDT getNdl2_StartDateTime() {
    return getTyped(1, NULLDT.class);
  }


  /**
   * Returns End Date/time (component 3).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NULLDT getEndDateTime() {
    return getTyped(2, NULLDT.class);
  }


  /**
   * Returns End Date/time (component 3).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NULLDT getNdl3_EndDateTime() {
    return getTyped(2, NULLDT.class);
  }


  /**
   * Returns Point of Care (component 4).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NULLDT getPointOfCare() {
    return getTyped(3, NULLDT.class);
  }


  /**
   * Returns Point of Care (component 4).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NULLDT getNdl4_PointOfCare() {
    return getTyped(3, NULLDT.class);
  }


  /**
   * Returns Room (component 5).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NULLDT getRoom() {
    return getTyped(4, NULLDT.class);
  }


  /**
   * Returns Room (component 5).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NULLDT getNdl5_Room() {
    return getTyped(4, NULLDT.class);
  }


  /**
   * Returns Bed (component 6).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NULLDT getBed() {
    return getTyped(5, NULLDT.class);
  }


  /**
   * Returns Bed (component 6).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NULLDT getNdl6_Bed() {
    return getTyped(5, NULLDT.class);
  }


  /**
   * Returns Facility (component 7).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NULLDT getFacility() {
    return getTyped(6, NULLDT.class);
  }


  /**
   * Returns Facility (component 7).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NULLDT getNdl7_Facility() {
    return getTyped(6, NULLDT.class);
  }


  /**
   * Returns Location Status (component 8).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NULLDT getLocationStatus() {
    return getTyped(7, NULLDT.class);
  }


  /**
   * Returns Location Status (component 8).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NULLDT getNdl8_LocationStatus() {
    return getTyped(7, NULLDT.class);
  }


  /**
   * Returns Patient Location Type (component 9).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NULLDT getPatientLocationType() {
    return getTyped(8, NULLDT.class);
  }


  /**
   * Returns Patient Location Type (component 9).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NULLDT getNdl9_PatientLocationType() {
    return getTyped(8, NULLDT.class);
  }


  /**
   * Returns Building (component 10).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NULLDT getBuilding() {
    return getTyped(9, NULLDT.class);
  }


  /**
   * Returns Building (component 10).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NULLDT getNdl10_Building() {
    return getTyped(9, NULLDT.class);
  }


  /**
   * Returns Floor (component 11).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NULLDT getFloor() {
    return getTyped(10, NULLDT.class);
  }


  /**
   * Returns Floor (component 11).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public NULLDT getNdl11_Floor() {
    return getTyped(10, NULLDT.class);
  }


}

