package nictru.nf.anndata;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import nictru.nf.anndata.store.AnnDataStore;

public class AnnData implements AutoCloseable {
    private final AnnDataStore store;

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

    // X is optional in AnnData files (can be None)
    private static final String[] REQUIRED_FIELDS = { "layers", "obs", "var", "obsm", "varm", "obsp", "varp", "uns" };

    private final Map<String, Set<String>> fieldObjects;

    public AnnData(Path path) {
        this.store = AnnDataStore.open(path);

        Set<String> fields = store.getRootFieldNames();
        List<String> missingFields = Arrays.stream(REQUIRED_FIELDS)
                .filter(field -> !fields.contains(field))
                .collect(Collectors.toList());
        if (!missingFields.isEmpty()) {
            closeQuietly();
            throw new IllegalArgumentException("Missing fields: " + missingFields);
        }

        this.obs = new DataFrame(store.getGroup("obs"));
        this.var = new DataFrame(store.getGroup("var"));

        this.obs_names = this.obs.rownames;
        this.var_names = this.var.rownames;

        this.n_obs = this.obs.size;
        this.n_vars = this.var.size;

        this.layers = getGroupChildKeys("layers");
        this.obsm = getGroupChildKeys("obsm");
        this.varm = getGroupChildKeys("varm");
        this.obsp = getGroupChildKeys("obsp");
        this.varp = getGroupChildKeys("varp");
        this.uns = getGroupChildKeys("uns");

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
    }

    private Set<String> getGroupChildKeys(String name) {
        return store.getGroup(name).getChildKeys();
    }

    private void closeQuietly() {
        try {
            store.close();
        } catch (Exception ignored) {
            // Ignore cleanup errors during failed construction.
        }
    }

    @Override
    public void close() throws java.io.IOException {
        store.close();
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

    /**
     * Returns the AnnData structure as a Map.
     * Suitable for nf-test snapshot assertions via path(file).anndata().yaml
     * nf-test serialises Maps as pretty-printed JSON, so each field renders on its own line.
     */
    public Map<String, Object> getYaml() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("n_obs", n_obs);
        map.put("n_vars", n_vars);
        map.put("obs", dataFrameMap(obs));
        map.put("var", dataFrameMap(var));
        map.put("layers", sortedList(layers));
        map.put("obsm", sortedList(obsm));
        map.put("varm", sortedList(varm));
        map.put("obsp", sortedList(obsp));
        map.put("varp", sortedList(varp));
        map.put("uns", sortedList(uns));
        return map;
    }

    private Map<String, Object> dataFrameMap(DataFrame df) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("index", df.index.getName());
        map.put("columns", Arrays.asList(df.colnames));
        return map;
    }

    private List<String> sortedList(Set<String> values) {
        List<String> list = new ArrayList<>(values);
        Collections.sort(list);
        return list;
    }
}
