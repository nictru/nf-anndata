# nf-anndata

Nextflow plugin for reading and accessing properties from AnnData (.h5ad) files.

## Features

- Get `n_vars` and `n_obs` - dimensions of the AnnData object
- Dataframes (`var` and `obs`)
    - Get column names (`colnames`)
    - Get row names (`rownames`)
    - Access index with `index` property (similar to pandas)
        - Get unique index values (`index.unique()`)
        - Get number of unique index values (`index.n_unique()`)
        - Check if value exists (`index.contains(value)`)
    - Get unique values per column (`column.unique()`)
    - Get number of unique values per column (`column.n_unique()`)
    - Access column data (`get(columnName)`)
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
    // Load an AnnData from a file
    def testFile = file('path/to/your/anndata/file.h5ad', checkIfExists: true)
    ch_adata = channel.of(testFile).map { file -> anndata(file) }

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

## API Reference

### `anndata(String path)` / `anndata(Path path)`

Loads an AnnData object from a file path.

**Parameters:**
- `path` - Path to the .h5ad file (String or Path)

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

#### Methods

- `close()` - Close the HDF5 file handle (inherited from HdfFile)

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
