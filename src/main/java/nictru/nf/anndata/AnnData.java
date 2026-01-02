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
    }

    private Set<String> getFields() {
        return this.getChildren().keySet();
    }

    private Set<String> getFields(String name) {
        GroupImpl group = (GroupImpl) this.getChild(name);
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

    public static void main(String[] args) {
        AnnData annData = new AnnData(Path.of("tests/pbmc3k_processed.h5ad"));
        System.out.println(annData);
        annData.close();
    }
}

