package nictru.nf.anndata;

import java.util.Arrays;

import io.jhdf.GroupImpl;
import io.jhdf.dataset.ContiguousDatasetImpl;

public class DataFrame {
    final String[] colnames;
    final String[] rownames;
    final int size;
    final GroupImpl group;

    public DataFrame(GroupImpl group) {
        this.colnames = getColumnNames(group);
        this.rownames = getRowNames(group);
        this.size = this.rownames.length;
        this.group = group;
    }

    private String[] getColumnNames(GroupImpl group) {
        return (String[]) group.getChildren().keySet().stream().filter(key -> !key.equals("_index"))
                .toArray(String[]::new);
    }

    private String[] getRowNames(GroupImpl group) {
        ContiguousDatasetImpl index = (ContiguousDatasetImpl) group.getChild("_index");
        if (index == null) {
            throw new IllegalArgumentException("Index not found in group: " + group.getName()
                    + ". This is probably because the AnnData object has a named index, which is not currently supported by nf-anndata.");
        }
        return (String[]) index.getData();
    }

    public DataFrameColumn get(String name) {
        return new DataFrameColumn(this.group, name);
    }
}

