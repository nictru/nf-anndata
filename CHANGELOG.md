# Changelog

All notable changes to nf-anndata will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.3.2] - 2026-01-29

### Fixed

- **Fixed SLF4J classloader conflict warning** when using nf-anndata in Nextflow pipelines
  - Excluded `slf4j-api` from runtime classpath to prevent conflict with Nextflow's logging implementation
  - The warning "SLF4J(E): A service provider failed to instantiate... LogbackServiceProvider not a subtype" no longer appears

## [0.3.1] - 2026-01-27

### Fixed

- **Fixed OutOfMemoryError for large S3 files** ([#6](https://github.com/nictru/nf-anndata/issues/6))
  - S3-backed files were being loaded entirely into memory, causing `OutOfMemoryError` for files larger than ~2GB
  - Root cause: When `workDir` is remote (e.g., S3), the staging directory was also remote, triggering Nextflow's `S3FileSystemProvider.newByteChannel()` which buffers the entire file in memory
  - Now stages remote files to a local temporary directory (`/tmp/nf-anndata-cache/`) using efficient streaming download via AWS TransferManager

### Changed

- **Improved remote file staging**
  - Staging now uses `FilesEx.copyTo()` which leverages provider-specific optimizations (e.g., AWS TransferManager for S3)
  - Cache is based on file path hash only, allowing reuse across pipeline runs on the same machine
  - Staged files are automatically cleaned up when the Nextflow session ends
  - Thread-safe concurrent access handling with proper lock management

## [0.3.0] - 2026-01-19

### Added

- **Automatic remote file staging** - Remote files (S3, GCS, Azure, HTTP, HTTPS, FTP) are now automatically staged to local storage before reading
  - Uses Nextflow's built-in `FileHelper.getLocalCachePath()` for efficient caching
  - Cached files are reused across tasks within the same session
  - No need to manually stage files or use dummy processes

### Fixed

- Fixed `UnsupportedOperationException: Position operation not supported` error when reading h5ad files from remote storage

## [0.2.3] - 2026-01-18

### Changed

- Test release to verify automated release workflow

## [0.2.2] - 2026-01-18

### Changed

- Refactored CI workflows to eliminate redundancy using reusable workflows

## [0.2.1] - 2026-01-18

### Changed

- Test release to verify automated release workflow

## [0.2.0] - 2026-01-18

### Added

- **DataFrame Index API** - New `index` property on DataFrames (similar to pandas)
  - `index.unique()` - Get unique values in the index
  - `index.n_unique()` - Get number of unique index values
  - `index.contains(value)` - Check if a value exists in the index
  - `index.name` - Get the name of the index column
  - `index.values` - Get all index values as array
  - `index.size()` - Get the number of elements in the index

- **Support for all h5ad data types**
  - Categorical columns (ordered and unordered)
  - Nullable integers and booleans with proper null handling
  - All numeric types (int8, int16, int32, int64, uint8, float32, float64)
  - Boolean columns
  - String columns
  - Unicode characters in indices and values

- **Support for various h5ad structures**
  - Named indices (custom index column names)
  - Integer indices (stored as strings)
  - Empty observations/variables
  - Optional X matrix (X can be None)
  - Sparse matrices (CSR, CSC)
  - Dense matrices (float32, float64)

- **Comprehensive test suite**
  - 175+ test cases covering all supported features
  - Tests against real-world data (pbmc3k_processed.h5ad)

### Changed

- `DataFrameColumn.unique()` is now a method instead of a property (use `column.unique()` instead of `column.unique`)
- Improved error messages for missing columns and invalid data types

### Fixed

- Fixed handling of categorical codes stored as different integer types (byte, short, int, long)
- Fixed boolean mask handling for nullable columns
- Fixed index reading for named indices (reading `_index` attribute)

## [0.1.0] - 2025-01-01

### Added

- Initial release
- Basic AnnData file reading
- Access to `n_obs`, `n_vars` dimensions
- Access to `obs` and `var` DataFrames
- Access to `obs_names` and `var_names`
- Access to keys of `layers`, `obsm`, `varm`, `obsp`, `varp`, `uns`
- DataFrame column access with `get(columnName)`
- Column unique values with `n_unique()`
- Nextflow plugin integration with `anndata()` function

[0.3.2]: https://github.com/nictru/nf-anndata/compare/v0.3.1...v0.3.2
[0.3.1]: https://github.com/nictru/nf-anndata/compare/v0.3.0...v0.3.1
[0.3.0]: https://github.com/nictru/nf-anndata/compare/v0.2.3...v0.3.0
[0.2.3]: https://github.com/nictru/nf-anndata/compare/v0.2.2...v0.2.3
[0.2.2]: https://github.com/nictru/nf-anndata/compare/v0.2.1...v0.2.2
[0.2.1]: https://github.com/nictru/nf-anndata/compare/v0.2.0...v0.2.1
[0.2.0]: https://github.com/nictru/nf-anndata/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/nictru/nf-anndata/releases/tag/v0.1.0
