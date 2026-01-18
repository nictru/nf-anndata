package nictru.nf.anndata;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.jhdf.HdfFile;
import io.jhdf.api.Group;

public class AnnData extends HdfFile {
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

    // X is optional in h5ad files (can be None)
    private static final String[] REQUIRED_FIELDS = { "layers", "obs", "var", "obsm", "varm", "obsp", "varp", "uns" };

    private final Map<String, Set<String>> fieldObjects;

    public AnnData(Path path) {
        super(path);

        Set<String> fields = this.getFields();
        List<String> missingFields = Arrays.stream(REQUIRED_FIELDS)
                .filter(field -> !fields.contains(field))
                .collect(Collectors.toList());
        if (!missingFields.isEmpty()) {
            throw new IllegalArgumentException("Missing fields: " + missingFields);
        }

        this.obs = new DataFrame((Group) this.getChild("obs"));
        this.var = new DataFrame((Group) this.getChild("var"));

        this.obs_names = this.obs.rownames;
        this.var_names = this.var.rownames;

        this.n_obs = this.obs.size;
        this.n_vars = this.var.size;

        this.layers = this.getGroupChildKeys("layers");
        this.obsm = this.getGroupChildKeys("obsm");
        this.varm = this.getGroupChildKeys("varm");
        this.obsp = this.getGroupChildKeys("obsp");
        this.varp = this.getGroupChildKeys("varp");
        this.uns = this.getGroupChildKeys("uns");

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

    private Set<String> getFields() {
        return this.getChildren().keySet();
    }

    private Set<String> getGroupChildKeys(String name) {
        Group group = (Group) this.getChild(name);
        return group.getChildren().keySet();
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
}
