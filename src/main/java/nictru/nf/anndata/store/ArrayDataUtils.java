package nictru.nf.anndata.store;

import java.util.Arrays;
import java.util.stream.IntStream;

import ucar.ma2.Array;

/**
 * Shared helpers for converting stored array payloads into Java values.
 */
public final class ArrayDataUtils {

    private ArrayDataUtils() {
    }

    public static Object readArrayData(StoreArray array) {
        return array.readData();
    }

    public static Object convertFromUcarArray(Array array) {
        ucar.ma2.DataType dataType = array.getDataType();
        if (dataType == ucar.ma2.DataType.OBJECT) {
            Object[] objects = (Object[]) array.get1DJavaArray(ucar.ma2.DataType.OBJECT);
            String[] strings = new String[objects.length];
            for (int i = 0; i < objects.length; i++) {
                strings[i] = objects[i] != null ? objects[i].toString() : null;
            }
            return strings;
        }
        return array.get1DJavaArray(dataType);
    }

    public static Object[] toObjectArray(Object currentData) {
        if (currentData == null) {
            return new Object[0];
        }
        if (currentData instanceof String[]) {
            return (String[]) currentData;
        }
        if (currentData instanceof Object[]) {
            return (Object[]) currentData;
        }
        if (currentData.getClass().isArray()) {
            if (currentData instanceof int[]) {
                return Arrays.stream((int[]) currentData).boxed().toArray(Object[]::new);
            }
            if (currentData instanceof float[]) {
                float[] arr = (float[]) currentData;
                return IntStream.range(0, arr.length)
                        .mapToDouble(i -> arr[i])
                        .boxed()
                        .toArray(Object[]::new);
            }
            if (currentData instanceof double[]) {
                return Arrays.stream((double[]) currentData).boxed().toArray(Object[]::new);
            }
            if (currentData instanceof long[]) {
                return Arrays.stream((long[]) currentData).boxed().toArray(Object[]::new);
            }
            if (currentData instanceof byte[]) {
                byte[] arr = (byte[]) currentData;
                Object[] result = new Object[arr.length];
                for (int i = 0; i < arr.length; i++) {
                    result[i] = arr[i];
                }
                return result;
            }
            if (currentData instanceof short[]) {
                short[] arr = (short[]) currentData;
                Object[] result = new Object[arr.length];
                for (int i = 0; i < arr.length; i++) {
                    result[i] = (int) arr[i];
                }
                return result;
            }
            if (currentData instanceof boolean[]) {
                boolean[] arr = (boolean[]) currentData;
                Object[] result = new Object[arr.length];
                for (int i = 0; i < arr.length; i++) {
                    result[i] = arr[i];
                }
                return result;
            }
            throw new IllegalArgumentException("Unsupported array type: " + currentData.getClass());
        }
        throw new IllegalArgumentException("Expected array type, got: " + currentData.getClass());
    }

    public static String[] toStringArray(Object[] arr) {
        String[] result = new String[arr.length];
        for (int i = 0; i < arr.length; i++) {
            result[i] = arr[i] != null ? arr[i].toString() : "";
        }
        return result;
    }

    public static String[] toStringArrayFromData(Object data) {
        if (data == null) {
            return new String[0];
        }
        if (data instanceof String[]) {
            return (String[]) data;
        }
        if (data instanceof Object[]) {
            return toStringArray((Object[]) data);
        }
        if (data instanceof long[]) {
            long[] longData = (long[]) data;
            String[] result = new String[longData.length];
            for (int i = 0; i < longData.length; i++) {
                result[i] = String.valueOf(longData[i]);
            }
            return result;
        }
        if (data instanceof int[]) {
            int[] intData = (int[]) data;
            String[] result = new String[intData.length];
            for (int i = 0; i < intData.length; i++) {
                result[i] = String.valueOf(intData[i]);
            }
            return result;
        }
        throw new IllegalArgumentException("Unsupported index data type: " + data.getClass());
    }

    public static int[] convertCodesToIntArray(Object codesData) {
        if (codesData instanceof byte[]) {
            byte[] arr = (byte[]) codesData;
            int[] result = new int[arr.length];
            for (int i = 0; i < arr.length; i++) {
                result[i] = arr[i];
            }
            return result;
        }
        if (codesData instanceof short[]) {
            short[] arr = (short[]) codesData;
            int[] result = new int[arr.length];
            for (int i = 0; i < arr.length; i++) {
                result[i] = arr[i];
            }
            return result;
        }
        if (codesData instanceof int[]) {
            return (int[]) codesData;
        }
        if (codesData instanceof long[]) {
            long[] arr = (long[]) codesData;
            int[] result = new int[arr.length];
            for (int i = 0; i < arr.length; i++) {
                result[i] = (int) arr[i];
            }
            return result;
        }
        throw new IllegalArgumentException("Unsupported codes type: " + codesData.getClass());
    }

    public static boolean[] convertToBooleanArray(Object data) {
        if (data instanceof boolean[]) {
            return (boolean[]) data;
        }
        if (data instanceof byte[]) {
            byte[] arr = (byte[]) data;
            boolean[] result = new boolean[arr.length];
            for (int i = 0; i < arr.length; i++) {
                result[i] = arr[i] != 0;
            }
            return result;
        }
        if (data instanceof int[]) {
            int[] arr = (int[]) data;
            boolean[] result = new boolean[arr.length];
            for (int i = 0; i < arr.length; i++) {
                result[i] = arr[i] != 0;
            }
            return result;
        }
        if (data instanceof String[]) {
            String[] arr = (String[]) data;
            boolean[] result = new boolean[arr.length];
            for (int i = 0; i < arr.length; i++) {
                result[i] = "TRUE".equalsIgnoreCase(arr[i]) || "1".equals(arr[i]);
            }
            return result;
        }
        throw new IllegalArgumentException("Unsupported mask type: " + data.getClass());
    }

    public static Object[] decodeCategoriesInt(Object[] categories, int[] codes) {
        return IntStream.range(0, codes.length)
                .mapToObj(i -> {
                    int code = codes[i];
                    if (code < 0) {
                        return null;
                    }
                    if (code >= categories.length) {
                        throw new IllegalArgumentException(
                                "Invalid category code " + code + " at index " + i
                                        + " (max valid code: " + (categories.length - 1) + ")");
                    }
                    return categories[code];
                })
                .toArray();
    }
}
