package nictru.nf.anndata.store;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import dev.zarr.zarrjava.ZarrException;
import dev.zarr.zarrjava.core.Attributes;
import dev.zarr.zarrjava.store.StoreHandle;
import dev.zarr.zarrjava.v2.GroupMetadata;
import ucar.ma2.Array;

final class ZarrAnnDataStore implements AnnDataStore {

    private final dev.zarr.zarrjava.core.Group root;

    ZarrAnnDataStore(Path path) {
        try {
            this.root = dev.zarr.zarrjava.core.Group.open(path);
        } catch (IOException | ZarrException e) {
            throw new IllegalArgumentException("Failed to open Zarr group: " + path, e);
        }
    }

    @Override
    public Set<String> getRootFieldNames() {
        return listChildKeys(root);
    }

    @Override
    public StoreGroup getGroup(String name) {
        try {
            dev.zarr.zarrjava.core.Node node = root.get(name);
            if (!(node instanceof dev.zarr.zarrjava.core.Group group)) {
                throw new IllegalArgumentException("Expected group at '" + name + "'");
            }
            return new ZarrJavaStoreGroup(group);
        } catch (IOException | ZarrException e) {
            throw new IllegalArgumentException("Failed to open Zarr group '" + name + "'", e);
        }
    }

    @Override
    public void close() {
        // Filesystem-backed zarr-java groups do not hold open resources.
    }

    static Set<String> listChildKeys(dev.zarr.zarrjava.core.Group group) {
        StoreHandle groupHandle = group.storeHandle;
        return groupHandle.listChildren()
                .filter(name -> !name.startsWith("."))
                .filter(name -> isZarrNode(groupHandle.resolve(name)))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static boolean isZarrNode(StoreHandle nodeHandle) {
        return nodeHandle.resolve(dev.zarr.zarrjava.core.Node.ZGROUP).exists()
                || nodeHandle.resolve(dev.zarr.zarrjava.core.Node.ZARRAY).exists();
    }

    static Object readGroupAttribute(dev.zarr.zarrjava.core.Group group, String name) {
        dev.zarr.zarrjava.core.GroupMetadata metadata = group.metadata();
        if (metadata instanceof GroupMetadata v2Metadata) {
            Attributes attributes = v2Metadata.attributes;
            return attributes != null ? attributes.get(name) : null;
        }
        try {
            return metadata.attributes().get(name);
        } catch (ZarrException e) {
            return null;
        }
    }

    static StoreNode wrapNode(dev.zarr.zarrjava.core.Node node) {
        if (node instanceof dev.zarr.zarrjava.core.Group group) {
            return new ZarrJavaStoreGroup(group);
        }
        if (node instanceof dev.zarr.zarrjava.core.Array array) {
            return new ZarrJavaStoreArray(array);
        }
        return null;
    }
}

final class ZarrJavaStoreGroup implements StoreGroup {

    private final dev.zarr.zarrjava.core.Group group;

    ZarrJavaStoreGroup(dev.zarr.zarrjava.core.Group group) {
        this.group = group;
    }

    @Override
    public String getName() {
        return group.storeHandle.toString();
    }

    @Override
    public Set<String> getChildKeys() {
        return ZarrAnnDataStore.listChildKeys(group);
    }

    @Override
    public Object getAttribute(String name) {
        return ZarrAnnDataStore.readGroupAttribute(group, name);
    }

    @Override
    public StoreNode getChild(String name) {
        try {
            return ZarrAnnDataStore.wrapNode(group.get(name));
        } catch (IOException | ZarrException e) {
            throw new IllegalArgumentException("Failed to open Zarr child '" + name + "'", e);
        }
    }
}

final class ZarrJavaStoreArray implements StoreArray {

    private final dev.zarr.zarrjava.core.Array array;

    ZarrJavaStoreArray(dev.zarr.zarrjava.core.Array array) {
        this.array = array;
    }

    @Override
    public Object readData() {
        try {
            Array data = array.read();
            return ArrayDataUtils.convertFromUcarArray(data);
        } catch (ZarrException e) {
            throw new IllegalArgumentException("Failed to read Zarr array: " + array.storeHandle, e);
        }
    }

    @Override
    public int[] getDimensions() {
        long[] shape = array.metadata().shape;
        int[] dimensions = new int[shape.length];
        for (int i = 0; i < shape.length; i++) {
            dimensions[i] = Math.toIntExact(shape[i]);
        }
        return dimensions;
    }
}
