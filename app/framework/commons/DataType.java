package framework.commons;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import play.Logger;

/**
 * A data type from BizDock.<br/>
 * A data type can have different attributes:
 * <ul>
 * <li>dataName : the name of the data object</li>
 * <li>dataTypeClassName : the java class name associated with this data object
 * </li>
 * <li>isAuditable : true if the data object is "auditable"</li>
 * <li>isCustomAttribute : true if the data object can have custom attributes
 * </li>
 * </ul>
 * 
 * @author Pierre-Yves Cloux
 */
public class DataType {
    private static Logger.ALogger log = Logger.of(DataType.class);
    /**
     * A repository of DataTypes.<br/>
     * This one must be feed at system startup
     */
    private static Map<String, DataType> dataTypes = Collections.synchronizedMap(new HashMap<String, DataType>());

    private String dataName;
    private String dataTypeClassName;
    private boolean isAuditable;
    private boolean isCustomAttribute;

    /**
     * Return a data type associated with the specified name
     * 
     * @param dataTypeName
     *            the name of a DataType
     * @return an object structure
     */
    public static DataType getDataType(String dataTypeName) {
        return dataTypes.get(dataTypeName);
    }

    /**
     * Add a new data type to the registry
     * 
     * @param dataName
     * @param dataTypeClassName
     * @param isAuditable
     * @param isCustomAttribute
     */
    public static void add(String dataName, String dataTypeClassName, boolean isAuditable, boolean isCustomAttribute) {
        if (!dataTypes.containsKey(dataName) && dataTypes.containsKey(dataName.toLowerCase())) {
            throw new IllegalArgumentException(
                    "WARNING: ambiguous data name attempt to register " + dataName + " while " + dataName.toLowerCase() + " is already registered");
        }
        dataTypes.put(dataName, new DataType(dataName, dataTypeClassName, isAuditable, isCustomAttribute));
    }

    public DataType() {
    }

    public DataType(String dataName, String dataTypeClassName, boolean isAuditable, boolean isCustomAttribute) {
        super();
        this.dataName = dataName;
        this.dataTypeClassName = dataTypeClassName;
        this.isAuditable = isAuditable;
        this.isCustomAttribute = isCustomAttribute;
    }

    public String getDataName() {
        return dataName;
    }

    public String getDataTypeClassName() {
        return dataTypeClassName;
    }

    public boolean isAuditable() {
        return isAuditable;
    }

    public boolean isCustomAttribute() {
        return isCustomAttribute;
    }

    public String getLabel() {
        return "data_type." + this.getDataName() + ".label";
    }

    /**
     * The list of data types which can be audited.
     */
    public static List<DataType> getAllAuditableDataTypes() {
        List<DataType> auditableDataTypes = new ArrayList<DataType>();
        for (DataType dataType : getDataTypes().values()) {
            if (dataType.isAuditable()) {
                auditableDataTypes.add(dataType);
            }
        }
        return auditableDataTypes;
    }

    /**
     * The list of data types which supports the custom attributes.
     */
    public static List<DataType> getAllCustomAttributeDataTypes() {
        List<DataType> auditableDataTypes = new ArrayList<DataType>();
        for (DataType dataType : getDataTypes().values()) {
            if (dataType.isCustomAttribute()) {
                auditableDataTypes.add(dataType);
            }
        }
        return auditableDataTypes;
    }

    /**
     * Returns the fully qualified java class name from the name of the data
     * type.
     * 
     * @param dataTypeName
     *            the data type as String
     * @return
     */
    public static String getClassNameFromDataTypeName(String dataTypeName) {
        try {
            return getDataType(dataTypeName).getDataTypeClassName();
        } catch (Exception e) {
            log.error("Attempt to find a DataType which does not exists", e);
            return null;
        }
    }

    /**
     * Return the data type associated with the specified fully qualified class
     * name.
     * 
     * @param className
     *            a fully qualified java class name
     * @return
     */
    public static DataType getDataTypeFromClassName(String className) {
        for (DataType dataType : getDataTypes().values()) {
            if (dataType.getDataTypeClassName().equals(className)) {
                return dataType;
            }
        }
        return null;
    }

    private static Map<String, DataType> getDataTypes() {
        return dataTypes;
    }

    public static DataType getUser() {
        return DataType.getDataType(IFrameworkConstants.User);
    }

    public static DataType getSystemLevelRoleType() {
        return DataType.getDataType(IFrameworkConstants.SystemLevelRoleType);
    }

    @Override
    public String toString() {
        return "DataType [dataName=" + dataName + "]";
    }
}
