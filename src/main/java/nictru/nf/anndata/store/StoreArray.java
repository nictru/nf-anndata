package nictru.nf.anndata.store;

/**
 * Backend-neutral view of a stored array dataset.
 */
public interface StoreArray extends StoreNode {

    Object readData();

    int[] getDimensions();

    @Override
    default boolean isGroup() {
        return false;
    }

    @Override
    default boolean isArray() {
        return true;
    }

    @Override
    default StoreGroup asGroup() {
        throw new UnsupportedOperationException("Node is not a group");
    }

    @Override
    default StoreArray asArray() {
        return this;
    }
}
