package nictru.nf.anndata.store;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

import io.jhdf.HdfFile;
import io.jhdf.api.Attribute;
import io.jhdf.api.Dataset;
import io.jhdf.api.Group;

final class H5adAnnDataStore implements AnnDataStore {

    private final HdfFile file;

    H5adAnnDataStore(Path path) {
        this.file = new HdfFile(path);
    }

    @Override
    public Set<String> getRootFieldNames() {
        return file.getChildren().keySet();
    }

    @Override
    public StoreGroup getGroup(String name) {
        Object child = file.getChild(name);
        if (!(child instanceof Group)) {
            throw new IllegalArgumentException("Expected group at '" + name + "'");
        }
        return new H5adStoreGroup((Group) child);
    }

    @Override
    public void close() throws IOException {
        file.close();
    }
}

final class H5adStoreGroup implements StoreGroup {

    private final Group group;

    H5adStoreGroup(Group group) {
        this.group = group;
    }

    @Override
    public String getName() {
        return group.getName();
    }

    @Override
    public Set<String> getChildKeys() {
        return group.getChildren().keySet();
    }

    @Override
    public Object getAttribute(String name) {
        Attribute attribute = group.getAttribute(name);
        return attribute != null ? attribute.getData() : null;
    }

    @Override
    public StoreNode getChild(String name) {
        Object child = group.getChild(name);
        return H5adStoreNode.wrap(child);
    }
}

final class H5adStoreArray implements StoreArray {

    private final Dataset dataset;

    H5adStoreArray(Dataset dataset) {
        this.dataset = dataset;
    }

    @Override
    public Object readData() {
        return dataset.getData();
    }

    @Override
    public int[] getDimensions() {
        return dataset.getDimensions();
    }
}

final class H5adStoreNode {

    private H5adStoreNode() {
    }

    static StoreNode wrap(Object child) {
        if (child == null) {
            return null;
        }
        if (child instanceof Group) {
            return new H5adStoreGroup((Group) child);
        }
        if (child instanceof Dataset) {
            return new H5adStoreArray((Dataset) child);
        }
        throw new IllegalArgumentException("Unsupported HDF5 node type: " + child.getClass());
    }
}
