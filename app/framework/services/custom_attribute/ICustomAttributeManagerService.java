package framework.services.custom_attribute;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wordnik.swagger.annotations.ApiModelProperty;

import models.framework_models.common.CustomAttributeDefinition;
import play.data.Form;

/**
 * The custom attribute manager service.
 * 
 * @author Johann Kohler
 */
public interface ICustomAttributeManagerService {

    public static String CUSTOM_ATTRIBUTE_FORM_FIELD_NAME_EXTENSION = "_custattr_";

    /**
     * The extended attribute form field names are derivated from the
     * {@link CustomAttributeDefinition} uuid.<br/>
     * A standard extension is added.
     * 
     * @param attributeDefinitionUuid
     *            the custom attribute definition uuid
     * @return a form field name
     */
    String getFieldNameFromDefinitionUuid(String attributeDefinitionUuid);

    /**
     * The extended attribute form field names are derivated from the
     * {@link CustomAttributeDefinition} uuid.<br/>
     * A standard extension is added.
     * 
     * @param fieldName
     *            a form field name
     * @return the corresponding custom attribute definition uuid
     */
    String getDefinitionUuidFromFieldName(String fieldName);

    /**
     * Return true if the specified type of object has one or more associated
     * custom attributes
     * 
     * @param clazz
     *            the same class
     * @return
     */
    boolean hasCustomAttributes(Class<?> clazz);

    /**
     * Return true if the specified type of object has one or more associated
     * custom attributes
     * 
     * @param clazz
     *            the same class
     * @param filter
     *            a filtering value
     * @return
     */
    boolean hasCustomAttributes(Class<?> clazz, String filter);

    /**
     * Fill the specified form with the found custom attribute values.
     * 
     * @param <T>
     *            The class of the form
     * @param form
     *            The form
     * @param clazz
     *            The class of the custom attribute object
     * @param objectId
     *            The id of the object
     */
    <T> void fillWithValues(Form<T> form, Class<?> clazz, Long objectId);

    /**
     * Fill the specified form with the found custom attribute values.
     * 
     * @param <T>
     *            The class of the form
     * @param form
     *            The form
     * @param clazz
     *            The class of the custom attribute object
     * @param filter
     *            the filter
     * @param objectId
     *            The id of the object
     */
    <T> void fillWithValues(Form<T> form, Class<?> clazz, String filter, Long objectId);

    /**
     * Fill a specific list field the specified form with the found custom
     * attribute values.
     * 
     * @param <T>
     *            The class of the form
     * @param form
     *            The form
     * @param clazz
     *            The class of the custom attribute object
     * @param filter
     *            the filter
     * @param listFieldName
     *            The name of the list field
     * @param objectIds
     *            the list of object ids, the indexes of the ids should
     *            correspond to field indexes
     */
    <T> void fillWithValues(Form<T> form, Class<?> clazz, String filter, String listFieldName, List<Long> objectIds);

    /**
     * Check the values of the custom attributes associated with the specified
     * class.<br/>
     * The values are expected to be stored in the form.
     * 
     * @param form
     *            a form of a certain class
     * @param clazz
     *            the same class
     */
    <T> boolean validateValues(Form<T> form, Class<?> clazz);

    <T> boolean validateValues(Form<T> form, Class<?> clazz, String filter);

    <T> boolean validateValues(Form<T> form, Class<?> clazz, String filter, String listFieldName, Boolean onlyDisplayedOrRequired);

    /**
     * Check the values of the custom attributes associated with the specified
     * class AND (if they are valid) save them into the database.<br/>
     * The values are expected to be stored in the form.
     * <p>
     * <b>Please wrap this method within a database transaction</b>
     * </p>
     * 
     * @param form
     *            a form of a certain class
     * @param clazz
     *            the same class
     * @param objectId
     *            an object Id
     */
    <T> boolean validateAndSaveValues(Form<T> form, Class<?> clazz, Long objectId);

    <T> boolean validateAndSaveValues(Form<T> form, Class<?> clazz, String filter, Long objectId);

    <T> boolean validateAndSaveValues(Form<T> form, Class<?> clazz, String filter, String listFieldName, List<Long> objectIds,
            Boolean onlyDisplayedOrRequired);

    /**
     * Return a list of custom attribute values which are serializable
     * 
     * @param clazz
     *            a model class
     * @param objectId
     *            the unique id of an instance of the specified class
     * @return
     */
    List<CustomAttributeValueObject> getSerializableValues(Class<?> clazz, Long objectId);

    /**
     * Return a list of custom attribute values which are serializable
     * 
     * @param clazz
     *            a model class
     * @param objectId
     *            the unique id of an instance of the specified class
     * @param filter
     *            a filtering condition
     * @return
     */
    List<CustomAttributeValueObject> getSerializableValues(Class<?> clazz, String filter, Long objectId);

    /**
     * Class use for representing the serializable values for a custom
     * attribute.
     */
    @JsonAutoDetect(fieldVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE,
            isGetterVisibility = Visibility.NONE, creatorVisibility = Visibility.NONE)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(Include.ALWAYS)
    public static class CustomAttributeValueObject {
        @JsonProperty
        public String uuid;

        @JsonProperty
        public String name;

        @JsonProperty
        public String type;

        @JsonProperty
        @ApiModelProperty(required = true)
        public Object value;

        /**
         * Default constructor.
         */
        public CustomAttributeValueObject() {
        }
    }

}
