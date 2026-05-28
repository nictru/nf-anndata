package nictru.nf.anndata.store;

/**
 * A node in an AnnData storage tree (group or array).
 */
public interface StoreNode {

    boolean isGroup();

    boolean isArray();

    StoreGroup asGroup();

    StoreArray asArray();
}
