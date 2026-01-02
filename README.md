# nf-anndata

Nextflow plugin for reading and accessing properties from AnnData (.h5ad) files.

## Features

- Get `n_vars` and `n_obs` - dimensions of the AnnData object
- Dataframes (`var` and `obs`)
    - Get column names (`colnames`)
    - Get row names (`rownames`)
    - Get unique values per column (`unique`)
    - Get number of unique values per column (`n_unique()`)
    - Access column data (`get(columnName)`)
- Get names of available fields in `layers`, `obsm`, `varm`, `obsp`, `varp`, `uns`

## Installation

### From Nextflow Plugin Registry

```bash
nextflow plugin install nf-anndata
```

### From Source

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

## Usage

### Basic Usage

Import the `anndata` function in your Nextflow script:

```nextflow
include { anndata } from 'plugin/nf-anndata'

workflow {
    // Load an AnnData file
    def ad = anndata('path/to/your/file.h5ad')
    
    // Access dimensions
    println "Dimensions: ${ad.n_obs} × ${ad.n_vars}"
    
    // Access obs dataframe
    println "Obs columns: ${ad.obs.colnames.join(', ')}"
    println "Obs row names: ${ad.obs.rownames.join(', ')}"
    
    // Access var dataframe
    println "Var columns: ${ad.var.colnames.join(', ')}"
    println "Var row names: ${ad.var.rownames.join(', ')}"
    
    // Access available fields
    println "Layers: ${ad.layers.join(', ')}"
    println "obsm: ${ad.obsm.join(', ')}"
    println "varm: ${ad.varm.join(', ')}"
    
    // Close the file when done
    ad.close()
}
```

### Working with Columns

```nextflow
include { anndata } from 'plugin/nf-anndata'

workflow {
    def ad = anndata('path/to/your/file.h5ad')
    
    // Get a column from obs
    def column = ad.obs.get('louvain')
    
    // Get unique values
    def uniqueValues = column.unique
    println "Unique values: ${uniqueValues.join(', ')}"
    
    // Get number of unique values
    println "Number of unique values: ${column.n_unique()}"
    
    // Access raw data
    def data = column.data
    println "First 5 values: ${data.take(5).join(', ')}"
    
    ad.close()
}
```

### Using with Channels

```nextflow
include { anndata } from 'plugin/nf-anndata'

workflow {
    Channel.fromPath('*.h5ad')
        .map { file ->
            def ad = anndata(file.toString())
            def result = [
                file: file,
                n_obs: ad.n_obs,
                n_vars: ad.n_vars,
                obs_cols: ad.obs.colnames.join(', ')
            ]
            ad.close()
            return result
        }
        .view()
}
```

### Using with Path Objects

The `anndata` function also accepts `Path` objects:

```nextflow
include { anndata } from 'plugin/nf-anndata'

workflow {
    def filePath = file('path/to/your/file.h5ad')
    def ad = anndata(filePath)
    
    // Use the AnnData object...
    
    ad.close()
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

- `close()` - Close the HDF5 file handle

### DataFrame Object

The DataFrame object (for `obs` and `var`) provides:

#### Properties

- `colnames` (String[]) - Column names
- `rownames` (String[]) - Row names
- `size` (int) - Number of rows

#### Methods

- `get(String columnName)` - Get a column by name, returns `DataFrameColumn`

### DataFrameColumn Object

The DataFrameColumn object provides:

#### Properties

- `data` (Object[]) - Array of column values
- `unique` (Set<Object>) - Set of unique values in the column

#### Methods

- `n_unique()` (int) - Number of unique values in the column

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
