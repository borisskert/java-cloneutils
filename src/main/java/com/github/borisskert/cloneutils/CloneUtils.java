package com.github.borisskert.cloneutils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Utility class to clone Plain Old Java Objects (POJOs).
 * Attention: this class uses jackson's {@link ObjectMapper}
 */
public class CloneUtils {
    private static ObjectMapper nonNullMapper;
    private static ObjectMapper nonFailingMapper;

    /**
     * Prevent instance creation
     */
    private CloneUtils() {
        throw new IllegalStateException();
    }

    static {
        nonNullMapper = new ObjectMapper();
        nonNullMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        nonNullMapper.setDefaultMergeable(true);

        nonFailingMapper = new ObjectMapper();
        nonFailingMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Creates an deep clone of the specified object which will be returned as a new instance of the specified {@link Class}
     *
     * @param object            the specified object to be cloned (may be null)
     * @param targetClass       the target {@link Class}
     * @param ignoredProperties the property names which will be ignored during cloning
     * @param <T>               the target class type
     * @param <S>               the source class type
     * @return a new instance of the cloned object or null if the specified object is null
     * @throws CloneException if something fails
     */
    public static <T, S> T deepClone(S object, Class<T> targetClass, String... ignoredProperties) throws CloneException {
        if (object == null) return null;

        Map<String, Object> objectAsMap = toMap(object, ignoredProperties);
        return fromMap(objectAsMap, targetClass);
    }

    /**
     * Creates an deep clone of the specified object which will be returned as new instance same type
     *
     * @param object            the specified object to be cloned (may be null)
     * @param ignoredProperties the property names which will be ignored during cloning
     * @param <T>               the source and target type
     * @return a new instance of the cloned object or null if the specified object is null
     * @throws CloneException if something fails
     */
    public static <T> T deepClone(T object, String... ignoredProperties) throws CloneException {
        if (object == null) return null;

        Map<String, Object> objectAsMap = toMap(object, ignoredProperties);
        return (T) fromMap(objectAsMap, object.getClass());
    }

    /**
     * Clones and patches a specified object and return a new instance same type
     *
     * @param origin            the object to be cloned (may be null)
     * @param patch             the patch which will be applied
     * @param ignoredProperties the property names to be ignored during cloning
     * @param <T>               the patch type
     * @param <S>               the source object type
     * @return a new instance of the cloned and patched object
     * @throws CloneException if something fails
     */
    public static <T, S> S deepPatch(S origin, T patch, String... ignoredProperties) throws CloneException {
        if (origin == null) return null;

        Map<String, Object> patchAsMap = toMap(patch, ignoredProperties);
        return patchFromMap(origin, patchAsMap);
    }

    /**
     * Clones and patches a specified object and return a new instance of the specified {@link Class}
     *
     * @param origin            the source object to be cloned (may be null)
     * @param patch             the patch which will be applied
     * @param targetClass       the target type as {@link Class}
     * @param ignoredProperties the property names which will be ignored during cloning
     * @param <T>               the patch type
     * @param <S>               the source class type
     * @param <C>               the target class type
     * @return a new instance of the cloned object or null if the specified object is null
     * @throws CloneException if something fails
     */
    public static <T, S, C> C deepPatch(S origin, T patch, Class<C> targetClass, String... ignoredProperties) throws CloneException {
        if (origin == null) return null;

        Map<String, Object> patchAsMap = toMap(patch, ignoredProperties);
        return patchFromMap(origin, patchAsMap, targetClass);
    }

    /**
     * This patch will not merge list properties
     */
    public static <T, S, C> C patch(S origin, T patch, Class<C> targetClass, String... ignoredProperties) throws CloneException {
        if (origin == null) return null;

        S clonedOrigin = CloneUtils.deepClone(origin);
        T patchWithoutIgnoredProperties = CloneUtils.deepClone(patch, ignoredProperties);

        return patch(clonedOrigin, patchWithoutIgnoredProperties, targetClass);
    }

    /**
     * This patch will not merge list properties
     */
    public static <T, S> S patch(S origin, T patch, String... ignoredProperties) throws CloneException {
        if (origin == null) return null;

        S clonedOrigin = CloneUtils.deepClone(origin);
        T patchWithoutIgnoredProperties = CloneUtils.deepClone(patch, ignoredProperties);

        return (S) patch(clonedOrigin, patchWithoutIgnoredProperties, origin.getClass());
    }

    /**
     * Clones and patches fields only of a specified object and return a new instance same type
     *
     * @param origin         the object to be cloned (may be null)
     * @param patch          the patch to be applied
     * @param onlyThisFields property names which will be patched
     * @param <T>            the patch type
     * @param <S>            the source and target type
     * @return a new instance of the cloned object or null if the specified object is null
     * @throws CloneException if something fails
     */
    public static <T, S> S deepPatchFieldsOnly(S origin, T patch, String... onlyThisFields) throws CloneException {
        if (origin == null) return null;

        Map<String, Object> patchAsMap = toMapFilteredBy(patch, onlyThisFields);
        return patchFromMap(origin, patchAsMap);
    }

    /**
     * Clones and patches fields only of a specified object and return a new instance of the specified {@link Class}
     *
     * @param origin         the object to be cloned (may be null)
     * @param patch          the patch to be applied
     * @param targetClass    the target type as {@link Class}
     * @param onlyThisFields property names which will be patched
     * @param <T>            the patch type
     * @param <S>            the source object type
     * @param <C>            the target object type
     * @return a new instance of the cloned object or null if the specified object is null
     * @throws CloneException if something fails
     */
    public static <T, S, C> C deepPatchFieldsOnly(S origin, T patch, Class<C> targetClass, String... onlyThisFields) throws CloneException {
        if (origin == null) return null;

        Map<String, Object> patchAsMap = toMapFilteredBy(patch, onlyThisFields);
        return patchFromMap(origin, patchAsMap, targetClass);
    }

    /**
     * Indicates if two specified objects equal deep
     *
     * @param right             the left object
     * @param left              the right object
     * @param ignoredProperties the property names which will be ignored during the check
     * @param <S>               the left item type
     * @param <T>               the right item type
     * @return true if the properties are equal (except the ignored ones), false if not
     * @throws CloneException if something fails
     */
    public static <S, T> boolean deepEquals(S right, T left, String... ignoredProperties) throws CloneException {
        Map<String, Object> originAsMap = toMap(right, ignoredProperties);
        Map<String, Object> otherAsMap = toMap(left, ignoredProperties);

        return Objects.equals(originAsMap, otherAsMap);
    }

    private static <S> Map<String, Object> toMap(S object, String... ignoredProperties) throws CloneException {
        JsonNode objectAsNode = toJsonNode(object, ignoredProperties);
        Map objectAsMap;

        try {
            objectAsMap = nonFailingMapper.treeToValue(objectAsNode, Map.class);
        } catch (JsonProcessingException e) {
            throw new CloneException(e);
        }

        return objectAsMap;
    }

    private static <S> JsonNode toJsonNode(S object, String... ignoredProperties) {
        JsonNode objectAsNode = nonNullMapper.valueToTree(object);

        removeIgnoredProperties(objectAsNode, ignoredProperties);

        return objectAsNode;
    }

    private static <S> Map<String, Object> toMapFilteredBy(S object, String... allowedKeys) throws CloneException {
        JsonNode objectAsNode = nonNullMapper.valueToTree(object);
        Map<String, Object> objectAsMap;

        try {
            objectAsMap = nonFailingMapper.treeToValue(objectAsNode, Map.class);
        } catch (JsonProcessingException e) {
            throw new CloneException(e);
        }

        Map<String, Object> filteredMap = new HashMap<>();
        for (String allowedKey : allowedKeys) {
            Object allowedValue = objectAsMap.get(allowedKey);
            filteredMap.put(allowedKey, allowedValue);
        }

        return filteredMap;
    }

    private static <T> T fromMap(Map<String, Object> map, Class<T> targetClass) throws CloneException {
        JsonNode mapAsNode = nonNullMapper.valueToTree(map);
        try {
            return nonFailingMapper.treeToValue(mapAsNode, targetClass);
        } catch (JsonProcessingException e) {
            throw new CloneException(e);
        }
    }

    private static <T> T patchFromMap(T origin, Map<String, Object> patchAsMap) {
        JsonNode patchAsNode = nonNullMapper.valueToTree(patchAsMap);
        JsonNode originAsNode = nonNullMapper.valueToTree(origin);

        try {
            nonNullMapper.readerForUpdating(originAsNode).readValue(patchAsNode);
            return (T) nonFailingMapper.treeToValue(originAsNode, origin.getClass());
        } catch (IOException e) {
            throw new CloneException(e);
        }
    }

    private static <T, C> C patchFromMap(T origin, Map<String, Object> patchAsMap, Class<C> targetClass) {
        JsonNode patchAsNode = nonNullMapper.valueToTree(patchAsMap);
        JsonNode originAsNode = nonNullMapper.valueToTree(origin);

        try {
            nonNullMapper.readerForUpdating(originAsNode).readValue(patchAsNode);
            return nonFailingMapper.treeToValue(originAsNode, targetClass);
        } catch (IOException e) {
            throw new CloneException(e);
        }
    }

    private static <T, P, C> C patch(T origin, P patch, Class<C> targetClass) {
        JsonNode patchAsNode = nonNullMapper.valueToTree(patch);

        try {
            nonFailingMapper.readerForUpdating(origin).readValue(patchAsNode);
            JsonNode originAsNode = nonNullMapper.valueToTree(origin);

            return nonFailingMapper.treeToValue(originAsNode, targetClass);
        } catch (IOException e) {
            throw new CloneException(e);
        }
    }

    private static void removeIgnoredProperties(JsonNode jsonNode, String[] ignoredProperties) {
        if (jsonNode.isObject()) {
            removeIgnoredProperties((ObjectNode) jsonNode, ignoredProperties);
        } else if (jsonNode.isArray()) {
            removeIgnoredProperties((ArrayNode) jsonNode, ignoredProperties);
        }
    }

    private static void removeIgnoredProperties(ArrayNode arrayNode, String[] ignoredProperties) {
        for (JsonNode childNode : arrayNode) {
            removeIgnoredProperties(childNode, ignoredProperties);
        }
    }

    private static void removeIgnoredProperties(ObjectNode objectNode, String[] ignoredProperties) {
        for (String ignoredProperty : ignoredProperties) {
            if (objectNode.has(ignoredProperty)) {
                objectNode.remove(ignoredProperty);
            } else {
                removeDeepPropertiesIfAny(objectNode, ignoredProperty);
            }
        }
    }

    private static void removeDeepPropertiesIfAny(JsonNode jsonNode, String ignoredProperty) {
        ObjectNode objectNode = (ObjectNode) jsonNode;
        String[] splitProperty = ignoredProperty.split("\\.");

        if (splitProperty.length > 1) {
            removeDeepProperties(objectNode, splitProperty);
        }
    }

    private static void removeDeepProperties(ObjectNode objectNode, String[] splitProperty) {
        String propertyKey = splitProperty[0];
        Object originPropertyValue = objectNode.get(propertyKey);
        String deepIgnoredProperties = buildDeepIgnoredProperties(splitProperty);

        JsonNode propertyValue = toJsonNode(originPropertyValue, deepIgnoredProperties);

        objectNode.set(propertyKey, propertyValue);
    }

    private static String buildDeepIgnoredProperties(String[] array) {
        StringJoiner joiner = new StringJoiner(".");

        for (String element : cutFirstElement(array)) {
            joiner.add(element);
        }

        return joiner.toString();
    }

    private static String[] cutFirstElement(String[] array) {
        String[] arrayWithoutFirst = new String[array.length - 1];
        System.arraycopy(array, 1, arrayWithoutFirst, 0, array.length - 1);

        return arrayWithoutFirst;
    }
}

