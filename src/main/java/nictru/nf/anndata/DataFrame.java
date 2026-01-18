package nictru.nf.anndata;

import io.jhdf.api.Attribute;
import io.jhdf.api.Dataset;
import io.jhdf.api.Group;

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
        Dataset index = (Dataset) group.getChild(indexName);
        if (index == null) {
            throw new IllegalArgumentException("Index '" + indexName + "' not found in group: " + group.getName());
        }
        
        // Check for empty dataset (0 dimensions)
        int[] dimensions = index.getDimensions();
        if (dimensions.length == 0 || dimensions[0] == 0) {
            return new String[0];
        }
        
        Object data = index.getData();
        if (data == null) {
            // Empty dataset
            return new String[0];
        }
        if (data instanceof String[]) {
            return (String[]) data;
        } else if (data instanceof Object[]) {
            // Handle Object[] (which may contain strings)
            Object[] objData = (Object[]) data;
            String[] result = new String[objData.length];
            for (int i = 0; i < objData.length; i++) {
                result[i] = objData[i] != null ? objData[i].toString() : "";
            }
            return result;
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

    public DataFrameColumn get(String name) {
        return new DataFrameColumn(this.group, name);
    }
}
