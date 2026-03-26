package nictru.nf.anndata;

import io.jhdf.api.Attribute;
import io.jhdf.api.Dataset;
import io.jhdf.api.Group;
import java.util.stream.IntStream;

public class DataFrame {
    final String[] colnames;
    final String[] rownames;
    final int size;
    final Group group;
    final DataFrameIndex index;

    public DataFrame(Group group) {
        String indexName = getIndexName(group);
        this.colnames = getColumnNames(group, indexName);
        this.rownames = getRowNames(group, indexName);
        this.size = this.rownames.length;
        this.group = group;
        this.index = new DataFrameIndex(this.rownames, indexName);
    }

    private String getIndexName(Group group) {
        // The _index attribute tells us which dataset contains the index
        Attribute indexAttr = group.getAttribute("_index");
        if (indexAttr != null) {
            Object data = indexAttr.getData();
            if (data instanceof String) {
                return (String) data;
            }
        }
        // Default to _index if attribute not found or not a string
        return "_index";
    }

    private String[] getColumnNames(Group group, String indexName) {
        return (String[]) group.getChildren().keySet().stream()
                .filter(key -> !key.equals(indexName))
                .toArray(String[]::new);
    }

    private String[] getRowNames(Group group, String indexName) {
        Object child = group.getChild(indexName);
        if (child == null) {
            throw new IllegalArgumentException("Index '" + indexName + "' not found in group: " + group.getName());
        }

        // Categorical index: stored as a group with categories + codes children
        if (child instanceof Group) {
            return decodeCategoricalIndex((Group) child);
        }

        Dataset index = (Dataset) child;

        // Check for empty dataset (0 dimensions)
        int[] dimensions = index.getDimensions();
        if (dimensions.length == 0 || dimensions[0] == 0) {
            return new String[0];
        }

        Object data = index.getData();
        if (data == null) {
            return new String[0];
        }
        if (data instanceof String[]) {
            return (String[]) data;
        } else if (data instanceof Object[]) {
            // Handle Object[] (which may contain strings or byte[] from R-generated files)
            return toStringArray((Object[]) data);
        } else if (data instanceof long[]) {
            // Handle integer indices
            long[] longData = (long[]) data;
            String[] result = new String[longData.length];
            for (int i = 0; i < longData.length; i++) {
                result[i] = String.valueOf(longData[i]);
            }
            return result;
        } else if (data instanceof int[]) {
            int[] intData = (int[]) data;
            String[] result = new String[intData.length];
            for (int i = 0; i < intData.length; i++) {
                result[i] = String.valueOf(intData[i]);
            }
            return result;
        } else {
            throw new IllegalArgumentException("Unsupported index data type: " + data.getClass());
        }
    }

    private String[] decodeCategoricalIndex(Group indexGroup) {
        Dataset categoriesDs = (Dataset) indexGroup.getChild("categories");
        Dataset codesDs = (Dataset) indexGroup.getChild("codes");

        Object catData = categoriesDs.getData();
        String[] categories;
        if (catData instanceof String[]) {
            categories = (String[]) catData;
        } else if (catData instanceof Object[]) {
            categories = toStringArray((Object[]) catData);
        } else {
            throw new IllegalArgumentException("Unsupported categories type in index: " + catData.getClass());
        }

        int[] codes = convertCodesToIntArray(codesDs.getData());
        final String[] cats = categories;
        return IntStream.range(0, codes.length)
                .mapToObj(i -> codes[i] < 0 ? "" : cats[codes[i]])
                .toArray(String[]::new);
    }

    private static String[] toStringArray(Object[] arr) {
        String[] result = new String[arr.length];
        for (int i = 0; i < arr.length; i++) {
            result[i] = arr[i] != null ? arr[i].toString() : "";
        }
        return result;
    }

    private static int[] convertCodesToIntArray(Object codesData) {
        if (codesData instanceof byte[]) {
            byte[] arr = (byte[]) codesData;
            int[] result = new int[arr.length];
            for (int i = 0; i < arr.length; i++) result[i] = arr[i];
            return result;
        } else if (codesData instanceof short[]) {
            short[] arr = (short[]) codesData;
            int[] result = new int[arr.length];
            for (int i = 0; i < arr.length; i++) result[i] = arr[i];
            return result;
        } else if (codesData instanceof int[]) {
            return (int[]) codesData;
        } else if (codesData instanceof long[]) {
            long[] arr = (long[]) codesData;
            int[] result = new int[arr.length];
            for (int i = 0; i < arr.length; i++) result[i] = (int) arr[i];
            return result;
        } else {
            throw new IllegalArgumentException("Unsupported index codes type: " + codesData.getClass());
        }
    }

    public DataFrameColumn get(String name) {
        return new DataFrameColumn(this.group, name);
    }
}
