package nictru.nf.anndata.store;

import java.util.Set;

/**
 * Backend-neutral view of an AnnData group (e.g. obs, var, layers).
 */
public interface StoreGroup extends StoreNode {

    String getName();

    Set<String> getChildKeys();

    Object getAttribute(String name);

    StoreNode getChild(String name);

    @Override
    default boolean isGroup() {
        return true;
    }

    @Override
    default boolean isArray() {
        return false;
    }

    @Override
    default StoreGroup asGroup() {
        return this;
    }

    @Override
    default StoreArray asArray() {
        throw new UnsupportedOperationException("Node is not an array: " + getName());
    }
}
