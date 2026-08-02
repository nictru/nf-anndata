package nictru.nf.anndata;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import nictru.nf.anndata.store.ArrayDataUtils;
import nictru.nf.anndata.store.StoreArray;
import nictru.nf.anndata.store.StoreGroup;
import nictru.nf.anndata.store.StoreNode;

public class DataFrame {
    final String[] colnames;
    final String[] rownames;
    final int size;
    final StoreGroup group;
    final DataFrameIndex index;

    public DataFrame(StoreGroup group) {
        String indexName = getIndexName(group);
        this.colnames = getColumnNames(group, indexName);
        this.rownames = getRowNames(group, indexName);
        this.size = this.rownames.length;
        this.group = group;
        this.index = new DataFrameIndex(this.rownames, indexName);
    }

    private String getIndexName(StoreGroup group) {
        Object indexAttr = group.getAttribute("_index");
        if (indexAttr instanceof String) {
            return (String) indexAttr;
        }
        return "_index";
    }

    private String[] getColumnNames(StoreGroup group, String indexName) {
        Object columnOrder = group.getAttribute("column-order");
        if (columnOrder instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> columns = (List<Object>) columnOrder;
            return columns.stream().map(Object::toString).toArray(String[]::new);
        }
        if (columnOrder instanceof String[]) {
            return (String[]) columnOrder;
        }
        if (columnOrder instanceof Object[]) {
            return Arrays.stream((Object[]) columnOrder).map(Object::toString).toArray(String[]::new);
        }
        return group.getChildKeys().stream()
                .filter(key -> !key.equals(indexName))
                .sorted()
                .toArray(String[]::new);
    }

    private String[] getRowNames(StoreGroup group, String indexName) {
        StoreNode child = group.getChild(indexName);
        if (child == null) {
            throw new IllegalArgumentException("Index '" + indexName + "' not found in group: " + group.getName());
        }

        if (child.isGroup()) {
            return decodeGroupIndex(child.asGroup());
        }

        StoreArray index = child.asArray();
        int[] dimensions = index.getDimensions();
        if (dimensions.length == 0 || dimensions[0] == 0) {
            return new String[0];
        }

        Object data = ArrayDataUtils.readArrayData(index);
        if (data == null) {
            return new String[0];
        }
        return ArrayDataUtils.toStringArrayFromData(data);
    }

    private String[] decodeGroupIndex(StoreGroup indexGroup) {
        if (indexGroup.getChild("categories") != null && indexGroup.getChild("codes") != null) {
            return decodeCategoricalIndex(indexGroup);
        }
        if (indexGroup.getChild("values") != null && indexGroup.getChild("mask") != null) {
            return ArrayDataUtils.decodeNullableStringIndex(indexGroup);
        }
        throw new IllegalArgumentException(
                "Unknown index group structure with keys: " + indexGroup.getChildKeys());
    }

    private String[] decodeCategoricalIndex(StoreGroup indexGroup) {
        StoreNode categoriesNode = indexGroup.getChild("categories");
        StoreNode codesNode = indexGroup.getChild("codes");
        if (categoriesNode == null || codesNode == null || !categoriesNode.isArray() || !codesNode.isArray()) {
            throw new IllegalArgumentException("Invalid categorical index in group: " + indexGroup.getName());
        }

        Object catData = ArrayDataUtils.readArrayData(categoriesNode.asArray());
        String[] categories = ArrayDataUtils.toStringArrayFromData(catData);

        int[] codes = ArrayDataUtils.convertCodesToIntArray(
                ArrayDataUtils.readArrayData(codesNode.asArray()));
        final String[] cats = categories;
        return IntStream.range(0, codes.length)
                .mapToObj(i -> codes[i] < 0 ? "" : cats[codes[i]])
                .toArray(String[]::new);
    }

    public DataFrameColumn get(String name) {
        return new DataFrameColumn(this.group, name);
    }
}
