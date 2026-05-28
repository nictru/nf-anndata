package nictru.nf.anndata.store;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * Backend-neutral access to an AnnData object on disk.
 */
public interface AnnDataStore extends AutoCloseable {

    Set<String> getRootFieldNames();

    StoreGroup getGroup(String name);

    @Override
    void close() throws IOException;

    static AnnDataStore open(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("Path must not be null");
        }
        Path fileName = path.getFileName();
        if (fileName == null) {
            throw new IllegalArgumentException("Path must include a file or directory name: " + path);
        }

        String name = fileName.toString();
        if (name.endsWith(".h5ad")) {
            return new H5adAnnDataStore(path);
        }
        if (name.endsWith(".zarr") || Files.isDirectory(path.resolve(".zgroup"))) {
            return new ZarrAnnDataStore(path);
        }

        throw new IllegalArgumentException(
                "Unsupported AnnData storage format (expected .h5ad file or .zarr directory): " + path);
    }
}
