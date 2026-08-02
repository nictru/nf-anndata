# nf-anndata

Nextflow plugin for reading and accessing properties from AnnData stores in `.h5ad` and `.zarr` formats.

## Supported formats

| Format | Path | Notes |
|--------|------|-------|
| `.h5ad` | Single HDF5 file | Remote paths (S3, GCS, Azure, HTTP) are staged locally before reading |
| `.zarr` | Local directory store | AnnData-on-Zarr v2 via patched [zarr-java](deps/zarr-java) (including vlen-utf8 string columns); remote Zarr stores are not supported yet |

The backend is selected automatically from the path extension (`.h5ad` file or `.zarr` directory).

## Features

- Get `n_vars` and `n_obs` - dimensions of the AnnData object
- Dataframes (`var` and `obs`)
    - Get column names (`colnames`)
    - Get row names (`rownames`)
    - Access index with `index` property (similar to pandas)
        - Get unique index values (`index.unique()`)
        - Get number of unique index values (`index.n_unique()`)
        - Check if value exists (`index.contains(value)`)
        - Supports string, categorical, and nullable-string-array (`values` + `mask`) index encodings
    - Get unique values per column (`column.unique()`)
    - Get number of unique values per column (`column.n_unique()`)
    - Access column data (`get(columnName)`) — including nullable integer/boolean/string columns
- Get names of available fields in `layers`, `obsm`, `varm`, `obsp`, `varp`, `uns`

## Installation

From Nextflow Plugin Registry

```bash
nextflow plugin install nf-anndata
```

alternatively, you can reference the plugin in the pipeline config:

```
plugins {
    id 'nf-anndata'
}
```

<details>
<summary>From Source</summary>


1. Clone this repository:
```bash
git clone https://github.com/nictru/nf-anndata.git
cd nf-anndata
```

2. Build the plugin:
```bash
make assemble
```

3. Install the plugin:
```bash
make install
```

</details>

## Usage

### Basic Usage

Import the `anndata` function in your Nextflow script:

```nextflow
include { anndata } from 'plugin/nf-anndata'

workflow {
    // Load an AnnData store (.h5ad file or .zarr directory)
    def testFile = file('path/to/your/anndata/file.h5ad', checkIfExists: true)
    ch_adata = channel.of(testFile).map { file -> anndata(file) }

    // Zarr stores are local directories
    def zarrFile = file('path/to/your/anndata/file.zarr', checkIfExists: true)
    ch_zarr = channel.of(zarrFile).map { file -> anndata(file) }

    // Alternatively, you can also load from a string
    ch_adata = channel.of(anndata('path/to/your.h5ad'))

    ch_adata.map { ad ->
            println "n_obs: ${ad.n_obs}"
            println "n_var: ${ad.n_var}"
        }
}
```

### Working with fields in layers, obsm, varm, obsp, varp, uns

```nextflow
// Keep only objects that have a layer called 'counts'
ch_adata_with_counts = ch_adata.filter { ad ->
    ad.layers.contains('counts')
}

// Branch based on presence of 'X_pca' in obsm
ch_has_pca = ch_adata.branch { ad ->
    yes: ad.obsm.contains('X_pca')
    no: true
}
```

### Working with obs/var columns

```nextflow
// Fail if the 'batch' column is missing
ch_adata.map { ad ->
    if (!ad.obs.columns) {
        error 'Column \'batch\' is missing'
    }
}

// Fail if there is not more than one unique value in the 'louvain' column
ch_adata.map { ad ->
    if (ad.obs.get('louvain').n_unique() < 2) {
        error 'Column \'louvain\' has less than 2 unique values'
    }
}

// Fail if a certain value does not exist in a column
ch_adata.map { ad ->
    if (!ad.obs.get('louvain').unique().contains('Dendritic cells')) {
        error 'Column \'louvain\' is missing Dendritic cells'
    }
}
```

### Working with DataFrame indices

```nextflow
// Check if all observation names are unique
ch_adata.map { ad ->
    if (ad.obs.index.n_unique() != ad.n_obs) {
        error 'Observation names are not unique'
    }
}

// Get all unique cell IDs
ch_adata.map { ad ->
    def uniqueCells = ad.obs.index.unique()
    println "Found ${uniqueCells.size()} unique cells"
}

// Verify a specific cell exists in the index
ch_adata.map { ad ->
    if (!ad.obs.index.contains('cell_001')) {
        error 'Expected cell_001 not found'
    }
}

// Get the name of the index column
ch_adata.map { ad ->
    println "Index column name: ${ad.obs.index.name}"
}
```

### YAML representation

The `yaml` property returns a `Map` representation of the AnnData object. Despite the name, the data is structured — nf-test serialises it as pretty-printed JSON, so each field appears on its own line in the snapshot file:

```nextflow
ch_adata.map { ad ->
    println ad.yaml
}
```

Example snapshot output (as stored in `.nf-test.snap`):

```json
{
  "n_obs": 2638,
  "n_vars": 1838,
  "obs": {
    "index": "_index",
    "columns": ["louvain", "n_counts", "n_genes", "percent_mito"]
  },
  "var": {
    "index": "_index",
    "columns": ["n_cells"]
  },
  "layers": ["counts"],
  "obsm": ["X_draw_graph_fr", "X_pca", "X_tsne", "X_umap"],
  "varm": ["PCs"],
  "obsp": ["connectivities", "distances"],
  "varp": [],
  "uns": ["louvain", "neighbors", "pca"]
}
```

In nf-test, this enables clean snapshot testing of the entire AnnData structure:

```groovy
then {
    assert path(output).anndata().yaml == snapshot.match()
}
```

## API Reference

### `anndata(String path)` / `anndata(Path path)`

Loads an AnnData object from a file or directory path.

**Parameters:**
- `path` - Path to a `.h5ad` file or local `.zarr` directory (String or Path)

**Returns:** `AnnData` object

### AnnData Object

The AnnData object provides the following properties and methods:

#### Properties

- `n_obs` (int) - Number of observations
- `n_vars` (int) - Number of variables
- `obs` (DataFrame) - Observations dataframe
- `var` (DataFrame) - Variables dataframe
- `obs_names` (String[]) - Observation names
- `var_names` (String[]) - Variable names
- `layers` (Set<String>) - Available layer names
- `obsm` (Set<String>) - Available obsm field names
- `varm` (Set<String>) - Available varm field names
- `obsp` (Set<String>) - Available obsp field names
- `varp` (Set<String>) - Available varp field names
- `uns` (Set<String>) - Available uns field names
- `yaml` (Map) - Structured map of the AnnData object (dimensions, obs/var index name and column names, available field keys). nf-test serialises it as pretty-printed JSON, making it suitable for snapshot assertions.

#### Methods

- `close()` - Release backend resources for the AnnData store

### DataFrame Object

The DataFrame object (for `obs` and `var`) provides:

#### Properties

- `colnames` (String[]) - Column names
- `rownames` (String[]) - Row names (index values)
- `size` (int) - Number of rows
- `index` (DataFrameIndex) - Index object for accessing row names

#### Methods

- `get(String columnName)` - Get a column by name, returns `DataFrameColumn`

### DataFrameIndex Object

The DataFrameIndex object (accessed via `obs.index` or `var.index`) provides:

#### Properties

- `values` (String[]) - Array of index values
- `name` (String) - Name of the index column

#### Methods

- `unique()` (Set<String>) - Get unique values in the index
- `n_unique()` (int) - Get number of unique values in the index
- `contains(String value)` (boolean) - Check if a value exists in the index
- `size()` (int) - Get the number of elements in the index

### DataFrameColumn Object

The DataFrameColumn object provides:

#### Properties

- `data` (Object[]) - Array of column values

#### Methods

- `unique()` (Set<Object>) - Get unique values in the column
- `n_unique()` (int) - Get number of unique values in the column

## Building

To build the plugin:

```bash
make assemble
```

The Zarr backend uses a patched copy of [zarr-java](deps/zarr-java) wired in via Gradle composite build (`includeBuild 'deps/zarr-java'`). After changing the submodule, run `./gradlew test` from the repository root; Gradle builds the local `dev.zarr:zarr-java:0.2.1-SNAPSHOT` artifact automatically.

To run zarr-java tests directly:

```bash
cd deps/zarr-java && ../../gradlew test
```

## Testing

The plugin can be tested with:

```bash
make test
```

To test with Nextflow:

1. Install the plugin: `make install`
2. Run the validation pipeline: `nextflow run validation/ -plugins nf-anndata@0.1.0`

## Publishing

Plugins can be published to the Nextflow Plugin Registry:

1. Create `$HOME/.gradle/gradle.properties` with:
   - `npr.apiKey`: Your Nextflow Plugin Registry access token
2. Publish: `make release`

## Requirements

- Nextflow 24.10.0 or later
- Java 11 or later

## License

See [COPYING](COPYING) file for license information.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.
