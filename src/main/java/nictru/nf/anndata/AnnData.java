package nictru.nf.anndata;

import java.nio.file.Path;
import io.jhdf.HdfFile;
import io.jhdf.GroupImpl;
import java.util.Arrays;
import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.lang.ref.Cleaner;

public class AnnData extends HdfFile implements AutoCloseable {
    private static final Cleaner CLEANER = Cleaner.create();
    private final Cleaner.Cleanable cleanable;
    private volatile boolean closed = false;
    final DataFrame obs;
    final DataFrame var;

    final String[] obs_names;
    final String[] var_names;

    final int n_obs;
    final int n_vars;

    final Set<String> layers;
    final Set<String> obsm;
    final Set<String> varm;
    final Set<String> obsp;
    final Set<String> varp;
    final Set<String> uns;

    private final String[] expectedFields = { "X", "layers", "obs", "var", "obsm", "varm", "obsp", "varp", "uns" };

    private final Map<String, Set<String>> fieldObjects;

    public AnnData(Path path) {
        super(path);

        Set<String> fields = this.getFields();
        List<String> missingFields = Arrays.stream(expectedFields).filter(field -> !fields.contains(field))
                .collect(Collectors.toList());
        if (!missingFields.isEmpty()) {
            throw new IllegalArgumentException("Missing fields: " + missingFields);
        }

        this.obs = new DataFrame((GroupImpl) this.getChild("obs"));
        this.var = new DataFrame((GroupImpl) this.getChild("var"));

        this.obs_names = this.obs.rownames;
        this.var_names = this.var.rownames;

        this.n_obs = this.obs.size;
        this.n_vars = this.var.size;

        this.layers = this.getFields("layers");
        this.obsm = this.getFields("obsm");
        this.varm = this.getFields("varm");
        this.obsp = this.getFields("obsp");
        this.varp = this.getFields("varp");
        this.uns = this.getFields("uns");

        this.fieldObjects = Map.of(
                "layers", this.layers,
                "obsm", this.obsm,
                "varm", this.varm,
                "obsp", this.obsp,
                "varp", this.varp,
                "uns", this.uns,
                "obs", new HashSet<>(Arrays.asList(this.obs.colnames)),
                "var", new HashSet<>(Arrays.asList(this.var.colnames))
            );

        // Register automatic cleanup when object is garbage collected
        this.cleanable = CLEANER.register(this, new ResourceCleanup(this));
    }

    /**
     * Cleanup action that will be called when the AnnData object is garbage collected.
     * This ensures the HDF5 file is closed even if the user forgets to call close().
     */
    private static class ResourceCleanup implements Runnable {
        private final AnnData annData;

        ResourceCleanup(AnnData annData) {
            // Store a reference to AnnData to check closed flag
            this.annData = annData;
        }

        @Override
        public void run() {
            // Only close if not already closed (to avoid double-closing)
            // This check is safe because closed is volatile
            if (!annData.closed) {
                // Use the internal doClose() method to close the HdfFile
                // without going through AnnData.close() to avoid recursion
                annData.doClose();
            }
        }
    }

    private Set<String> getFields() {
        return this.getChildren().keySet();
    }

    private Set<String> getFields(String name) {
        GroupImpl group = (GroupImpl) this.getChild(name);
        return group.getChildren().keySet();
    }

    /**
     * Internal method to actually close the HDF5 file.
     * This is called by both explicit close() and automatic cleanup.
     */
    private void doClose() {
        super.close();
    }

    /**
     * Closes the HDF5 file handle.
     * This method can be called explicitly, or the file will be automatically
     * closed when the object is garbage collected.
     */
    @Override
    public void close() {
        if (!closed) {
            closed = true;
            // Unregister from cleaner to prevent automatic cleanup from running
            if (cleanable != null) {
                cleanable.clean();
            }
            doClose();
        }
    }

    /**
     * Checks if the AnnData object has been closed.
     * 
     * @return true if the object has been closed, false otherwise
     */
    public boolean isClosed() {
        return closed;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AnnData object with n_obs × n_vars = ");
        sb.append(this.n_obs).append(" × ").append(this.n_vars).append("\n");

        List<String> fieldStrings = this.fieldObjects.entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .map(entry -> "\t" + entry.getKey() + ": "
                        + String.join(", ",
                                entry.getValue().stream().map(v -> "'" + v + "'").collect(Collectors.toList())))
                .collect(Collectors.toList());

        sb.append(String.join("\n", fieldStrings));

        return sb.toString();
    }

    public static void main(String[] args) {
        AnnData annData = new AnnData(Path.of("tests/pbmc3k_processed.h5ad"));
        System.out.println(annData);
        annData.close();
    }
}

