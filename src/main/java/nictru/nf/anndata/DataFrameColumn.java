package nictru.nf.anndata;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import io.jhdf.api.Dataset;
import io.jhdf.api.Group;

public class DataFrameColumn {
    final Object[] data;

    public DataFrameColumn(Group group, String name) {
        // Can be Dataset or Group
        Object child = group.getChild(name);
        if (child == null) {
            throw new IllegalArgumentException("Column '" + name + "' not found in group: " + group.getName());
        }
        if (child instanceof Dataset) {
            this.data = convertDatasetToObjectArray((Dataset) child);
        } else if (child instanceof Group) {
            Group dataGroup = (Group) child;
            this.data = handleGroupData(dataGroup);
        } else {
            throw new IllegalArgumentException("Invalid data type for column '" + name + "': " + child.getClass());
        }
    }

    private Object[] convertDatasetToObjectArray(Dataset dataset) {
        Object currentData = dataset.getData();
        if (currentData == null) {
            return new Object[0];
        }
        if (currentData instanceof String[]) {
            return (String[]) currentData;
        } else if (currentData instanceof Object[]) {
            return (Object[]) currentData;
        } else if (currentData.getClass().isArray()) {
            // Handle primitive arrays by converting them to Object arrays
            if (currentData instanceof int[]) {
                return Arrays.stream((int[]) currentData).boxed().toArray(Object[]::new);
            } else if (currentData instanceof float[]) {
                float[] arr = (float[]) currentData;
                return IntStream.range(0, arr.length)
                        .mapToDouble(i -> arr[i]).boxed().toArray(Object[]::new);
            } else if (currentData instanceof double[]) {
                return Arrays.stream((double[]) currentData).boxed().toArray(Object[]::new);
            } else if (currentData instanceof long[]) {
                return Arrays.stream((long[]) currentData).boxed().toArray(Object[]::new);
            } else if (currentData instanceof byte[]) {
                byte[] arr = (byte[]) currentData;
                Object[] result = new Object[arr.length];
                for (int i = 0; i < arr.length; i++) {
                    result[i] = arr[i];
                }
                return result;
            } else if (currentData instanceof short[]) {
                short[] arr = (short[]) currentData;
                Object[] result = new Object[arr.length];
                for (int i = 0; i < arr.length; i++) {
                    result[i] = (int) arr[i];
                }
                return result;
            } else if (currentData instanceof boolean[]) {
                boolean[] arr = (boolean[]) currentData;
                Object[] result = new Object[arr.length];
                for (int i = 0; i < arr.length; i++) {
                    result[i] = arr[i];
                }
                return result;
            } else {
                throw new IllegalArgumentException("Unsupported array type: " + currentData.getClass());
            }
        } else {
            throw new IllegalArgumentException("Expected array type, got: " + currentData.getClass());
        }
    }

    private Object[] handleGroupData(Group dataGroup) {
        // Check if this is a categorical column (has categories and codes)
        if (dataGroup.getChild("categories") != null && dataGroup.getChild("codes") != null) {
            return handleCategoricalData(dataGroup);
        }
        // Check if this is a nullable column (has mask and values)
        else if (dataGroup.getChild("mask") != null && dataGroup.getChild("values") != null) {
            return handleNullableData(dataGroup);
        }
        else {
            throw new IllegalArgumentException("Unknown group structure with keys: " + dataGroup.getChildren().keySet());
        }
    }

    private Object[] handleCategoricalData(Group dataGroup) {
        Dataset categories = (Dataset) dataGroup.getChild("categories");
        Object[] categoriesArray = (Object[]) categories.getData();

        Dataset codes = (Dataset) dataGroup.getChild("codes");
        Object codesData = codes.getData();
        
        // Handle different code types (byte, short, int, long)
        int[] codesArray = convertToIntArray(codesData);
        
        return decodeCategoriesInt(categoriesArray, codesArray);
    }

    private Object[] handleNullableData(Group dataGroup) {
        Dataset values = (Dataset) dataGroup.getChild("values");
        Dataset mask = (Dataset) dataGroup.getChild("mask");
        
        Object[] valuesArray = convertDatasetToObjectArray(values);
        boolean[] maskArray = convertToBooleanArray(mask.getData());
        
        // Apply mask: where mask is true, the value is null/NA
        Object[] result = new Object[valuesArray.length];
        for (int i = 0; i < valuesArray.length; i++) {
            result[i] = maskArray[i] ? null : valuesArray[i];
        }
        return result;
    }

    private boolean[] convertToBooleanArray(Object data) {
        if (data instanceof boolean[]) {
            return (boolean[]) data;
        } else if (data instanceof byte[]) {
            // HDF5 often stores booleans as bytes (0 = false, non-zero = true)
            byte[] arr = (byte[]) data;
            boolean[] result = new boolean[arr.length];
            for (int i = 0; i < arr.length; i++) {
                result[i] = arr[i] != 0;
            }
            return result;
        } else if (data instanceof int[]) {
            int[] arr = (int[]) data;
            boolean[] result = new boolean[arr.length];
            for (int i = 0; i < arr.length; i++) {
                result[i] = arr[i] != 0;
            }
            return result;
        } else if (data instanceof String[]) {
            // jhdf might decode enum booleans as strings like "TRUE"/"FALSE"
            String[] arr = (String[]) data;
            boolean[] result = new boolean[arr.length];
            for (int i = 0; i < arr.length; i++) {
                result[i] = "TRUE".equalsIgnoreCase(arr[i]) || "1".equals(arr[i]);
            }
            return result;
        } else {
            throw new IllegalArgumentException("Unsupported mask type: " + data.getClass());
        }
    }

    private int[] convertToIntArray(Object codesData) {
        if (codesData instanceof byte[]) {
            byte[] arr = (byte[]) codesData;
            int[] result = new int[arr.length];
            for (int i = 0; i < arr.length; i++) {
                // Keep signed interpretation: -1 means NA in pandas categoricals
                result[i] = arr[i];
            }
            return result;
        } else if (codesData instanceof short[]) {
            short[] arr = (short[]) codesData;
            int[] result = new int[arr.length];
            for (int i = 0; i < arr.length; i++) {
                result[i] = arr[i];
            }
            return result;
        } else if (codesData instanceof int[]) {
            return (int[]) codesData;
        } else if (codesData instanceof long[]) {
            long[] arr = (long[]) codesData;
            int[] result = new int[arr.length];
            for (int i = 0; i < arr.length; i++) {
                result[i] = (int) arr[i];
            }
            return result;
        } else {
            throw new IllegalArgumentException("Unsupported codes type: " + codesData.getClass());
        }
    }

    private Object[] decodeCategoriesInt(Object[] categories, int[] codes) {
        return IntStream.range(0, codes.length)
                .mapToObj(i -> {
                    int code = codes[i];
                    // Handle -1 as NA/null for categoricals
                    if (code < 0) {
                        return null;
                    }
                    // Bounds check for safety
                    if (code >= categories.length) {
                        throw new IllegalArgumentException(
                            "Invalid category code " + code + " at index " + i + 
                            " (max valid code: " + (categories.length - 1) + ")");
                    }
                    return categories[code];
                })
                .toArray();
    }

    /**
     * Get unique values in the column.
     * Similar to pandas Series.unique()
     * 
     * @return Set of unique values
     */
    public Set<Object> unique() {
        if (data == null || data.length == 0) {
            return new HashSet<>();
        }

        Set<Object> uniqueValues = new HashSet<>();
        for (Object value : data) {
            uniqueValues.add(value);
        }

        return uniqueValues;
    }

    /**
     * Get the number of unique values in the column.
     * Similar to pandas Series.nunique()
     * 
     * @return Number of unique values
     */
    public int n_unique() {
        return this.unique().size();
    }
}
