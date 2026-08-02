package nictru.nf.anndata;

import java.util.HashSet;
import java.util.Set;

import nictru.nf.anndata.store.ArrayDataUtils;
import nictru.nf.anndata.store.StoreArray;
import nictru.nf.anndata.store.StoreGroup;
import nictru.nf.anndata.store.StoreNode;

public class DataFrameColumn {
    final Object[] data;

    public DataFrameColumn(StoreGroup group, String name) {
        StoreNode child = group.getChild(name);
        if (child == null) {
            throw new IllegalArgumentException("Column '" + name + "' not found in group: " + group.getName());
        }
        if (child.isArray()) {
            this.data = ArrayDataUtils.toObjectArray(ArrayDataUtils.readArrayData(child.asArray()));
        } else if (child.isGroup()) {
            this.data = handleGroupData(child.asGroup());
        } else {
            throw new IllegalArgumentException("Invalid data type for column '" + name + "'");
        }
    }

    private Object[] handleGroupData(StoreGroup dataGroup) {
        if (dataGroup.getChild("categories") != null && dataGroup.getChild("codes") != null) {
            return handleCategoricalData(dataGroup);
        }
        if (dataGroup.getChild("mask") != null && dataGroup.getChild("values") != null) {
            return handleNullableData(dataGroup);
        }
        throw new IllegalArgumentException("Unknown group structure with keys: " + dataGroup.getChildKeys());
    }

    private Object[] handleCategoricalData(StoreGroup dataGroup) {
        StoreNode categoriesNode = dataGroup.getChild("categories");
        StoreNode codesNode = dataGroup.getChild("codes");
        if (categoriesNode == null || codesNode == null || !categoriesNode.isArray() || !codesNode.isArray()) {
            throw new IllegalArgumentException("Invalid categorical column in group: " + dataGroup.getName());
        }

        Object[] categoriesArray = ArrayDataUtils.toObjectArray(
                ArrayDataUtils.readArrayData(categoriesNode.asArray()));
        int[] codesArray = ArrayDataUtils.convertCodesToIntArray(
                ArrayDataUtils.readArrayData(codesNode.asArray()));

        return ArrayDataUtils.decodeCategoriesInt(categoriesArray, codesArray);
    }

    private Object[] handleNullableData(StoreGroup dataGroup) {
        return ArrayDataUtils.decodeNullableGroup(dataGroup);
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
