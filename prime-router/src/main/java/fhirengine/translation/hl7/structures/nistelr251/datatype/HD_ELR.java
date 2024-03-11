package fhirengine.translation.hl7.structures.nistelr251.datatype;

import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.Type;
import ca.uhn.hl7v2.model.AbstractComposite;


/**
 * <p>Represents an HL7 HD (Hierarchic Designator) data type.
 * This type consists of the following components:</p>
 * <ul>
 * <li>Namespace ID (IS)
 * <li>Universal ID (ST)
 * <li>Universal ID Type (ID)
 * </ul>
 */
@SuppressWarnings("unused")
public class HD_ELR extends AbstractComposite {

  private Type[] data;
  private boolean RP;

  /**
   * Creates a new HD type
   */
  public HD_ELR(Message message) {
    super(message);
    RP = false;
    init();
  }

  /**
   * Creates a new HD type
   */
  public HD_ELR(Message message, boolean isRP) {
    super(message);
    RP = isRP;
    init();
  }

  private void init() {
    data = new Type[3];
    data[0] = new IS(getMessage());
    data[1] = new ST(getMessage());
    data[2] = new ID(getMessage(), 301);
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
   * Returns Namespace ID (component 1).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public IS getNamespaceID() {
    return getTyped(0, IS.class);
  }


  /**
   * Returns Namespace ID (component 1).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public IS getHd1_NamespaceID() {
    return getTyped(0, IS.class);
  }


  /**
   * Returns Universal ID (component 2).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public ST getUniversalID() {
    return getTyped(1, ST.class);
  }


  /**
   * Returns Universal ID (component 2).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public ST getHd2_UniversalID() {
    return getTyped(1, ST.class);
  }


  /**
   * Returns Universal ID Type (component 3).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public ID getUniversalIDType() {
    return getTyped(2, ID.class);
  }


  /**
   * Returns Universal ID Type (component 3).  This is a convenience method that saves you from
   * casting and handling an exception.
   */
  public ID getHd3_UniversalIDType() {
    return getTyped(2, ID.class);
  }


}


