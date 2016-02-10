package framework.services.custom_attribute;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

import framework.services.configuration.II18nMessagesPlugin;
import framework.services.session.IUserSessionManagerPlugin;
import framework.services.storage.IAttachmentManagerPlugin;
import models.framework_models.common.CustomAttributeDefinition;
import models.framework_models.common.ICustomAttributeValue;
import play.Configuration;
import play.Logger;
import play.data.Form;
import play.inject.ApplicationLifecycle;
import play.libs.F.Promise;

/**
 * The custom attribute manager service.
 * 
 * @author Johann Kohler
 */
@Singleton
public class CustomAttributeManagerServiceImpl implements ICustomAttributeManagerService {

    private static Logger.ALogger log = Logger.of(CustomAttributeManagerServiceImpl.class);

    private II18nMessagesPlugin i18nMessagesPlugin;
    private IUserSessionManagerPlugin userSessionManagerPlugin;
    private IAttachmentManagerPlugin attachmentManagerPlugin;

    /**
     * Creates a new CustomAttributeManagerServiceImpl.
     * 
     * @param configuration
     *            the play configuration service
     * @param lifecycle
     *            the play application lifecyle listener
     * @param i18nMessagesPlugin
     *            the i18n messages service
     * @param userSessionManagerPlugin
     *            the user session manager service
     * @param attachmentManagerPlugin
     *            the attachment manager service
     */
    @Inject
    public CustomAttributeManagerServiceImpl(Configuration configuration, ApplicationLifecycle lifecycle, II18nMessagesPlugin i18nMessagesPlugin,
            IUserSessionManagerPlugin userSessionManagerPlugin, IAttachmentManagerPlugin attachmentManagerPlugin) {
        this.i18nMessagesPlugin = i18nMessagesPlugin;
        this.userSessionManagerPlugin = userSessionManagerPlugin;
        this.attachmentManagerPlugin = attachmentManagerPlugin;
        log.info("SERVICE>>> CustomAttributeManagerServiceImpl starting...");
        lifecycle.addStopHook(() -> {
            log.info("SERVICE>>> CustomAttributeManagerServiceImpl stopping...");
            log.info("SERVICE>>> CustomAttributeManagerServiceImpl stopped");
            return Promise.pure(null);
        });
        log.info("SERVICE>>> CustomAttributeManagerServiceImpl started");
    }

    @Override
    public String getFieldNameFromDefinitionUuid(String attributeDefinitionUuid) {
        return CUSTOM_ATTRIBUTE_FORM_FIELD_NAME_EXTENSION + attributeDefinitionUuid;
    }

    @Override
    public String getDefinitionUuidFromFieldName(String fieldName) {
        if (fieldName.startsWith(CUSTOM_ATTRIBUTE_FORM_FIELD_NAME_EXTENSION) && fieldName.length() > CUSTOM_ATTRIBUTE_FORM_FIELD_NAME_EXTENSION.length()) {
            return fieldName.substring(CUSTOM_ATTRIBUTE_FORM_FIELD_NAME_EXTENSION.length());
        }
        return null;
    }

    @Override
    public boolean hasCustomAttributes(Class<?> clazz) {
        List<CustomAttributeDefinition> customAttributeDefinitions = CustomAttributeDefinition.getOrderedCustomAttributeDefinitions(clazz);
        return customAttributeDefinitions != null && customAttributeDefinitions.size() > 0;
    }

    @Override
    public boolean hasCustomAttributes(Class<?> clazz, String filter) {
        List<CustomAttributeDefinition> customAttributeDefinitions = CustomAttributeDefinition.getOrderedCustomAttributeDefinitions(clazz, filter);
        return customAttributeDefinitions != null && customAttributeDefinitions.size() > 0;
    }

    @Override
    public <T> void fillWithValues(Form<T> form, Class<?> clazz, Long objectId) {
        fillWithValues(form, clazz, null, objectId, "");
    }

    @Override
    public <T> void fillWithValues(Form<T> form, Class<?> clazz, String filter, Long objectId) {
        fillWithValues(form, clazz, filter, objectId, "");
    }

    @Override
    public <T> void fillWithValues(Form<T> form, Class<?> clazz, String filter, String listFieldName, List<Long> objectIds) {
        for (int i = 0; i < objectIds.size(); i++) {
            fillWithValues(form, clazz, filter, objectIds.get(i), listFieldName + "[" + i + "].");
        }
    }

    private <T> void fillWithValues(Form<T> form, Class<?> clazz, String filter, Long objectId, String prefix) {

        List<ICustomAttributeValue> values = null;
        if (filter != null) {
            values = CustomAttributeDefinition.getOrderedCustomAttributeValues(clazz, filter, objectId);
        } else {
            values = CustomAttributeDefinition.getOrderedCustomAttributeValues(clazz, objectId);
        }
        if (values != null) {
            for (ICustomAttributeValue value : values) {
                if (value.isNotReadFromDb()) {
                    value.defaults();
                }
                if (value.getAttributeType().isMultiValued()) {
                    // If multi-valued the print value is a comma separated list
                    // of ids
                    String[] stringValues = StringUtils.split(value.print(), ',');
                    int count = 0;
                    for (String stringValue : stringValues) {
                        form.data().put(prefix + getFieldNameFromDefinitionUuid(value.getDefinition().uuid) + "[" + count + "]", stringValue);
                        count++;
                    }
                } else {
                    form.data().put(prefix + getFieldNameFromDefinitionUuid(value.getDefinition().uuid), value.print());
                }
            }
        }
    }

    @Override
    public <T> boolean validateValues(Form<T> form, Class<?> clazz) {
        return validateOrSaveValues(form, clazz, -1L, false, "", false);
    }

    @Override
    public <T> boolean validateValues(Form<T> form, Class<?> clazz, String filter) {
        return validateOrSaveValues(form, clazz, filter, -1L, false, "", false);
    }

    @Override
    public <T> boolean validateValues(Form<T> form, Class<?> clazz, String filter, String listFieldName, Boolean onlyDisplayedOrRequired) {

        // get the indexes to validate
        Map<String, String> data = form.data();
        Set<Integer> indexes = new HashSet<>();
        for (String key : data.keySet()) {
            if (key.startsWith(listFieldName + "[")) {
                final Pattern pattern = Pattern.compile(listFieldName + "\\[(.+?)\\](.*)");
                final Matcher matcher = pattern.matcher(key);
                matcher.find();
                indexes.add(Integer.valueOf(matcher.group(1)));
            }
        }

        boolean result = false;
        for (Integer index : indexes) {
            result = validateOrSaveValues(form, clazz, filter, -1L, false, listFieldName + "[" + index + "].", onlyDisplayedOrRequired) || result;
        }
        return result;

    }

    @Override
    public <T> boolean validateAndSaveValues(Form<T> form, Class<?> clazz, Long objectId) {
        return validateOrSaveValues(form, clazz, objectId, true, "", false);
    }

    @Override
    public <T> boolean validateAndSaveValues(Form<T> form, Class<?> clazz, String filter, Long objectId) {
        return validateOrSaveValues(form, clazz, filter, objectId, true, "", false);
    }

    @Override
    public <T> boolean validateAndSaveValues(Form<T> form, Class<?> clazz, String filter, String listFieldName, List<Long> objectIds,
            Boolean onlyDisplayedOrRequired) {
        boolean result = false;
        for (int i = 0; i < objectIds.size(); i++) {
            result = validateOrSaveValues(form, clazz, filter, objectIds.get(i), true, listFieldName + "[" + i + "].", onlyDisplayedOrRequired) || result;
        }
        return result;
    }

    private <T> boolean validateOrSaveValues(Form<T> form, Class<?> clazz, Long objectId, boolean saveValues, String prefix,
            Boolean onlyDisplayedOrRequired) {
        return validateOrSaveValues(form, clazz, null, objectId, saveValues, prefix, onlyDisplayedOrRequired);
    }

    private <T> boolean validateOrSaveValues(Form<T> form, Class<?> clazz, String filter, Long objectId, boolean saveValues, String prefix,
            Boolean onlyDisplayedOrRequired) {
        boolean hasErrors = false;
        Map<String, String> data = form.data();
        if (data != null) {
            List<ICustomAttributeValue> customAttributeValues = null;
            if (filter != null) {
                customAttributeValues = CustomAttributeDefinition.getOrderedCustomAttributeValues(clazz, filter, objectId);
            } else {
                customAttributeValues = CustomAttributeDefinition.getOrderedCustomAttributeValues(clazz, objectId);
            }
            if (customAttributeValues != null) {
                for (ICustomAttributeValue customAttributeValue : customAttributeValues) {

                    if (!onlyDisplayedOrRequired || customAttributeValue.getDefinition().isDisplayed || customAttributeValue.getDefinition().isRequired()) {

                        String fieldName = this.getFieldNameFromDefinitionUuid(customAttributeValue.getDefinition().uuid);
                        if (customAttributeValue.getAttributeType().isMultiValued()) {
                            // Find the multiple values and create a comma
                            // separated
                            // list of values
                            List<String> stringValues = new ArrayList<String>();
                            for (String key : data.keySet()) {
                                if (key.startsWith(prefix + fieldName + "[")) {
                                    String stringValue = data.get(prefix + key);
                                    stringValues.add(stringValue);
                                }
                            }
                            customAttributeValue.parse(this.getI18nMessagesPlugin(),
                                    StringUtils.join(stringValues, ICustomAttributeValue.MULTI_VALUE_SEPARATOR));
                        } else {
                            customAttributeValue.parse(this.getI18nMessagesPlugin(), data.get(prefix + fieldName));
                        }
                        if (customAttributeValue.hasError()) {
                            hasErrors = true;
                            form.reject(prefix + fieldName, customAttributeValue.getErrorMessage());
                        } else {
                            if (saveValues) {
                                customAttributeValue.performSave(this.getUserSessionManagerPlugin(), this.getAttachmentManagerPlugin(), fieldName);
                            }
                        }

                    }
                }
            }
        }
        return hasErrors;
    }

    @Override
    public List<CustomAttributeValueObject> getSerializableValues(Class<?> clazz, Long objectId) {
        List<CustomAttributeValueObject> customAtttributeApiValues = new ArrayList<>();

        for (ICustomAttributeValue iCustomAttributeValue : CustomAttributeDefinition.getOrderedCustomAttributeValues(clazz, objectId)) {

            CustomAttributeValueObject customAttributeApiValue = new CustomAttributeValueObject();

            customAttributeApiValue.uuid = iCustomAttributeValue.getDefinition().uuid;
            customAttributeApiValue.name = iCustomAttributeValue.getDefinition().getNameLabel();
            customAttributeApiValue.type = iCustomAttributeValue.getDefinition().attributeType;
            customAttributeApiValue.value = iCustomAttributeValue.getAsSerializableValue();

            customAtttributeApiValues.add(customAttributeApiValue);

        }
        return customAtttributeApiValues;
    }

    @Override
    public List<CustomAttributeValueObject> getSerializableValues(Class<?> clazz, String filter, Long objectId) {

        List<CustomAttributeValueObject> customAtttributeApiValues = new ArrayList<>();

        for (ICustomAttributeValue iCustomAttributeValue : CustomAttributeDefinition.getOrderedCustomAttributeValues(clazz, filter, objectId)) {

            CustomAttributeValueObject customAttributeApiValue = new CustomAttributeValueObject();

            customAttributeApiValue.uuid = iCustomAttributeValue.getDefinition().uuid;
            customAttributeApiValue.name = iCustomAttributeValue.getDefinition().getNameLabel();
            customAttributeApiValue.type = iCustomAttributeValue.getDefinition().attributeType;
            customAttributeApiValue.value = iCustomAttributeValue.getAsSerializableValue();

            customAtttributeApiValues.add(customAttributeApiValue);

        }
        return customAtttributeApiValues;
    }

    /**
     * Get the i18n messages service.
     */
    private II18nMessagesPlugin getI18nMessagesPlugin() {
        return this.i18nMessagesPlugin;
    }

    /**
     * Get the user session manager service.
     */
    private IUserSessionManagerPlugin getUserSessionManagerPlugin() {
        return this.userSessionManagerPlugin;
    }

    /**
     * Get the attachment manager service.
     */
    private IAttachmentManagerPlugin getAttachmentManagerPlugin() {
        return this.attachmentManagerPlugin;
    }

}
