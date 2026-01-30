#!/usr/bin/env python
"""
Generate comprehensive test h5ad files covering all edge cases in the anndata format.

This script generates a suite of h5ad files for testing nf-anndata's ability
to read various structural variations possible in h5ad files.
"""

import numpy as np
import pandas as pd
from scipy import sparse
import anndata as ad
from pathlib import Path
from tqdm import tqdm

# Get the directory where this script is located
SCRIPT_DIR = Path(__file__).parent.resolve()

# Create output directory for all h5ad files relative to script location
output_dir = SCRIPT_DIR / "test_cases"
output_dir.mkdir(exist_ok=True)

# Standard dimensions for test files
N_OBS = 20
N_VARS = 10

# Set random seed for reproducible output
RANDOM_SEED = 42
rng = np.random.default_rng(RANDOM_SEED)
# For scipy sparse.random, we need a RandomState or integer seed
sparse_rng = np.random.RandomState(RANDOM_SEED)


def generate_index_unnamed():
    """Generate h5ad with unnamed index (default, stored as _index)."""
    obs_names = [f"cell_{i}" for i in range(N_OBS)]
    var_names = [f"gene_{i}" for i in range(N_VARS)]
    
    adata = ad.AnnData(
        X=rng.random((N_OBS, N_VARS), dtype=np.float32),
        obs=pd.DataFrame({"cluster": rng.choice(["A", "B", "C"], N_OBS)}, index=obs_names),
        var=pd.DataFrame({"gene_type": rng.choice(["protein", "rna"], N_VARS)}, index=var_names)
    )
    # Ensure index is unnamed
    adata.obs.index.name = None
    adata.var.index.name = None
    
    adata.write_h5ad(output_dir / "index_unnamed.h5ad")


def generate_index_named():
    """Generate h5ad with named index."""
    obs_names = [f"cell_{i}" for i in range(N_OBS)]
    var_names = [f"gene_{i}" for i in range(N_VARS)]
    
    obs_df = pd.DataFrame({"cluster": rng.choice(["A", "B", "C"], N_OBS)}, index=obs_names)
    obs_df.index.name = "cell_id"
    
    var_df = pd.DataFrame({"gene_type": rng.choice(["protein", "rna"], N_VARS)}, index=var_names)
    var_df.index.name = "gene_id"
    
    adata = ad.AnnData(
        X=rng.random((N_OBS, N_VARS), dtype=np.float32),
        obs=obs_df,
        var=var_df
    )
    
    adata.write_h5ad(output_dir / "index_named.h5ad")


def generate_index_integer():
    """Generate h5ad with integer-based index."""
    obs_names = pd.Index(range(N_OBS), dtype="int64")
    var_names = pd.Index(range(N_VARS), dtype="int64")
    
    adata = ad.AnnData(
        X=rng.random((N_OBS, N_VARS), dtype=np.float32),
        obs=pd.DataFrame({"cluster": rng.choice(["A", "B", "C"], N_OBS)}, index=obs_names),
        var=pd.DataFrame({"gene_type": rng.choice(["protein", "rna"], N_VARS)}, index=var_names)
    )
    adata.obs.index.name = None
    adata.var.index.name = None
    
    adata.write_h5ad(output_dir / "index_integer.h5ad")


def generate_dtypes_numeric():
    """Generate h5ad with various numeric column types."""
    obs_names = [f"cell_{i}" for i in range(N_OBS)]
    var_names = [f"gene_{i}" for i in range(N_VARS)]
    
    obs = pd.DataFrame({
        "int8": rng.integers(-128, 127, N_OBS, dtype=np.int8),
        "int16": rng.integers(-32768, 32767, N_OBS, dtype=np.int16),
        "int32": rng.integers(-2147483648, 2147483647, N_OBS, dtype=np.int32),
        "int64": rng.integers(-100, 100, N_OBS, dtype=np.int64),
        "uint8": rng.integers(0, 255, N_OBS, dtype=np.uint8),
        "float32": rng.random(N_OBS, dtype=np.float32),
        "float64": rng.random(N_OBS, dtype=np.float64),
    }, index=obs_names)
    
    var = pd.DataFrame({
        "int32": rng.integers(0, 100, N_VARS, dtype=np.int32),
        "float32": rng.random(N_VARS, dtype=np.float32),
    }, index=var_names)
    
    adata = ad.AnnData(
        X=rng.random((N_OBS, N_VARS), dtype=np.float32),
        obs=obs,
        var=var
    )
    adata.obs.index.name = None
    adata.var.index.name = None
    
    adata.write_h5ad(output_dir / "dtypes_numeric.h5ad")


def generate_dtypes_categorical():
    """Generate h5ad with categorical columns (ordered and unordered)."""
    obs_names = [f"cell_{i}" for i in range(N_OBS)]
    var_names = [f"gene_{i}" for i in range(N_VARS)]
    
    obs = pd.DataFrame({
        "cat_unordered": pd.Categorical(rng.choice(["A", "B", "C"], N_OBS), ordered=False),
        "cat_ordered": pd.Categorical(rng.choice(["low", "medium", "high"], N_OBS), 
                                        categories=["low", "medium", "high"], ordered=True),
    }, index=obs_names)
    
    var = pd.DataFrame({
        "cat_unordered": pd.Categorical(rng.choice(["type1", "type2"], N_VARS), ordered=False),
    }, index=var_names)
    
    adata = ad.AnnData(
        X=rng.random((N_OBS, N_VARS), dtype=np.float32),
        obs=obs,
        var=var
    )
    adata.obs.index.name = None
    adata.var.index.name = None
    
    adata.write_h5ad(output_dir / "dtypes_categorical.h5ad")


def generate_dtypes_boolean():
    """Generate h5ad with boolean columns."""
    obs_names = [f"cell_{i}" for i in range(N_OBS)]
    var_names = [f"gene_{i}" for i in range(N_VARS)]
    
    obs = pd.DataFrame({
        "is_selected": rng.choice([True, False], N_OBS).astype(bool),
        "is_valid": rng.choice([True, False], N_OBS).astype(bool),
    }, index=obs_names)
    
    var = pd.DataFrame({
        "is_marker": rng.choice([True, False], N_VARS).astype(bool),
    }, index=var_names)
    
    adata = ad.AnnData(
        X=rng.random((N_OBS, N_VARS), dtype=np.float32),
        obs=obs,
        var=var
    )
    adata.obs.index.name = None
    adata.var.index.name = None
    
    adata.write_h5ad(output_dir / "dtypes_boolean.h5ad")


def generate_dtypes_nullable():
    """Generate h5ad with nullable integer and boolean types."""
    obs_names = [f"cell_{i}" for i in range(N_OBS)]
    var_names = [f"gene_{i}" for i in range(N_VARS)]
    
    # Create nullable integer array
    int_values = rng.integers(0, 100, N_OBS, dtype=np.int32)
    int_mask = rng.choice([True, False], N_OBS)
    nullable_int = pd.arrays.IntegerArray(int_values, mask=int_mask)
    
    # Create nullable boolean array
    bool_values = rng.choice([True, False], N_OBS).astype(bool)
    bool_mask = rng.choice([True, False], N_OBS).astype(bool)
    nullable_bool = pd.arrays.BooleanArray(bool_values, mask=bool_mask)
    
    obs = pd.DataFrame({
        "nullable_int": nullable_int,
        "nullable_bool": nullable_bool,
    }, index=obs_names)
    
    adata = ad.AnnData(
        X=rng.random((N_OBS, N_VARS), dtype=np.float32),
        obs=obs,
        var=pd.DataFrame(index=var_names)
    )
    adata.obs.index.name = None
    adata.var.index.name = None
    
    adata.write_h5ad(output_dir / "dtypes_nullable.h5ad")


def generate_dtypes_string():
    """Generate h5ad with string columns."""
    obs_names = [f"cell_{i}" for i in range(N_OBS)]
    var_names = [f"gene_{i}" for i in range(N_VARS)]
    
    obs = pd.DataFrame({
        "sample_id": [f"sample_{i}" for i in range(N_OBS)],
        "batch": rng.choice(["batch1", "batch2", "batch3"], N_OBS),
    }, index=obs_names)
    
    var = pd.DataFrame({
        "gene_symbol": [f"GENE_{i}" for i in range(N_VARS)],
    }, index=var_names)
    
    adata = ad.AnnData(
        X=rng.random((N_OBS, N_VARS), dtype=np.float32),
        obs=obs,
        var=var
    )
    adata.obs.index.name = None
    adata.var.index.name = None
    
    adata.write_h5ad(output_dir / "dtypes_string.h5ad")


def generate_x_dense_float32():
    """Generate h5ad with dense float32 X matrix."""
    obs_names = [f"cell_{i}" for i in range(N_OBS)]
    var_names = [f"gene_{i}" for i in range(N_VARS)]
    
    adata = ad.AnnData(
        X=rng.random((N_OBS, N_VARS), dtype=np.float32),
        obs=pd.DataFrame(index=obs_names),
        var=pd.DataFrame(index=var_names)
    )
    adata.obs.index.name = None
    adata.var.index.name = None
    
    adata.write_h5ad(output_dir / "x_dense_float32.h5ad")


def generate_x_dense_float64():
    """Generate h5ad with dense float64 X matrix."""
    obs_names = [f"cell_{i}" for i in range(N_OBS)]
    var_names = [f"gene_{i}" for i in range(N_VARS)]
    
    adata = ad.AnnData(
        X=rng.random((N_OBS, N_VARS), dtype=np.float64),
        obs=pd.DataFrame(index=obs_names),
        var=pd.DataFrame(index=var_names)
    )
    adata.obs.index.name = None
    adata.var.index.name = None
    
    adata.write_h5ad(output_dir / "x_dense_float64.h5ad")


def generate_x_sparse_csr():
    """Generate h5ad with CSR sparse X matrix."""
    obs_names = [f"cell_{i}" for i in range(N_OBS)]
    var_names = [f"gene_{i}" for i in range(N_VARS)]
    
    # Create sparse matrix with ~10% density
    X = sparse.random(N_OBS, N_VARS, density=0.1, format='csr', dtype=np.float32, random_state=sparse_rng)
    
    adata = ad.AnnData(
        X=X,
        obs=pd.DataFrame(index=obs_names),
        var=pd.DataFrame(index=var_names)
    )
    adata.obs.index.name = None
    adata.var.index.name = None
    
    adata.write_h5ad(output_dir / "x_sparse_csr.h5ad")


def generate_x_sparse_csc():
    """Generate h5ad with CSC sparse X matrix."""
    obs_names = [f"cell_{i}" for i in range(N_OBS)]
    var_names = [f"gene_{i}" for i in range(N_VARS)]
    
    # Create sparse matrix with ~10% density
    X = sparse.random(N_OBS, N_VARS, density=0.1, format='csc', dtype=np.float32, random_state=sparse_rng)
    
    adata = ad.AnnData(
        X=X,
        obs=pd.DataFrame(index=obs_names),
        var=pd.DataFrame(index=var_names)
    )
    adata.obs.index.name = None
    adata.var.index.name = None
    
    adata.write_h5ad(output_dir / "x_sparse_csc.h5ad")


def generate_x_none():
    """Generate h5ad with no X matrix (shape only)."""
    obs_names = [f"cell_{i}" for i in range(N_OBS)]
    var_names = [f"gene_{i}" for i in range(N_VARS)]
    
    adata = ad.AnnData(
        X=None,
        shape=(N_OBS, N_VARS),
        obs=pd.DataFrame({"cluster": rng.choice(["A", "B"], N_OBS)}, index=obs_names),
        var=pd.DataFrame({"type": rng.choice(["type1", "type2"], N_VARS)}, index=var_names)
    )
    adata.obs.index.name = None
    adata.var.index.name = None
    
    adata.write_h5ad(output_dir / "x_none.h5ad")


def generate_obsm_dense():
    """Generate h5ad with dense numpy arrays in obsm/varm."""
    obs_names = [f"cell_{i}" for i in range(N_OBS)]
    var_names = [f"gene_{i}" for i in range(N_VARS)]
    
    adata = ad.AnnData(
        X=rng.random((N_OBS, N_VARS), dtype=np.float32),
        obs=pd.DataFrame(index=obs_names),
        var=pd.DataFrame(index=var_names),
        obsm={
            "X_pca": rng.random((N_OBS, 10), dtype=np.float32),
            "X_umap": rng.random((N_OBS, 2), dtype=np.float32),
        },
        varm={
            "PCs": rng.random((N_VARS, 10), dtype=np.float32),
        }
    )
    adata.obs.index.name = None
    adata.var.index.name = None
    
    adata.write_h5ad(output_dir / "obsm_dense.h5ad")


def generate_obsm_sparse():
    """Generate h5ad with sparse matrices in obsm/varm."""
    obs_names = [f"cell_{i}" for i in range(N_OBS)]
    var_names = [f"gene_{i}" for i in range(N_VARS)]
    
    adata = ad.AnnData(
        X=rng.random((N_OBS, N_VARS), dtype=np.float32),
        obs=pd.DataFrame(index=obs_names),
        var=pd.DataFrame(index=var_names),
        obsm={
            "X_sparse": sparse.random(N_OBS, 50, density=0.1, format='csr', dtype=np.float32, random_state=sparse_rng),
        },
        varm={
            "Y_sparse": sparse.random(N_VARS, 30, density=0.1, format='csc', dtype=np.float32, random_state=sparse_rng),
        }
    )
    adata.obs.index.name = None
    adata.var.index.name = None
    
    adata.write_h5ad(output_dir / "obsm_sparse.h5ad")


def generate_obsm_dataframe():
    """Generate h5ad with DataFrames in obsm/varm."""
    obs_names = [f"cell_{i}" for i in range(N_OBS)]
    var_names = [f"gene_{i}" for i in range(N_VARS)]
    
    adata = ad.AnnData(
        X=rng.random((N_OBS, N_VARS), dtype=np.float32),
        obs=pd.DataFrame(index=obs_names),
        var=pd.DataFrame(index=var_names),
        obsm={
            "X_df": pd.DataFrame(
                rng.random((N_OBS, 5), dtype=np.float64),
                columns=[f"PC{i}" for i in range(5)],
                index=obs_names
            ),
        },
        varm={
            "Y_df": pd.DataFrame(
                rng.random((N_VARS, 3), dtype=np.float64),
                columns=[f"comp{i}" for i in range(3)],
                index=var_names
            ),
        }
    )
    adata.obs.index.name = None
    adata.var.index.name = None
    
    adata.write_h5ad(output_dir / "obsm_dataframe.h5ad")


def generate_obsp_dense():
    """Generate h5ad with dense square matrices in obsp/varp."""
    obs_names = [f"cell_{i}" for i in range(N_OBS)]
    var_names = [f"gene_{i}" for i in range(N_VARS)]
    
    adata = ad.AnnData(
        X=rng.random((N_OBS, N_VARS), dtype=np.float32),
        obs=pd.DataFrame(index=obs_names),
        var=pd.DataFrame(index=var_names),
        obsp={
            "connectivities": rng.random((N_OBS, N_OBS), dtype=np.float32),
            "distances": rng.random((N_OBS, N_OBS), dtype=np.float32),
        },
        varp={
            "correlations": rng.random((N_VARS, N_VARS), dtype=np.float32),
        }
    )
    adata.obs.index.name = None
    adata.var.index.name = None
    
    adata.write_h5ad(output_dir / "obsp_dense.h5ad")


def generate_obsp_sparse():
    """Generate h5ad with sparse square matrices in obsp/varp."""
    obs_names = [f"cell_{i}" for i in range(N_OBS)]
    var_names = [f"gene_{i}" for i in range(N_VARS)]
    
    adata = ad.AnnData(
        X=rng.random((N_OBS, N_VARS), dtype=np.float32),
        obs=pd.DataFrame(index=obs_names),
        var=pd.DataFrame(index=var_names),
        obsp={
            "connectivities": sparse.random(N_OBS, N_OBS, density=0.2, format='csr', dtype=np.float32, random_state=sparse_rng),
        },
        varp={
            "correlations": sparse.random(N_VARS, N_VARS, density=0.2, format='csc', dtype=np.float32, random_state=sparse_rng),
        }
    )
    adata.obs.index.name = None
    adata.var.index.name = None
    
    adata.write_h5ad(output_dir / "obsp_sparse.h5ad")


def generate_layers_mixed():
    """Generate h5ad with multiple layers (dense + sparse)."""
    obs_names = [f"cell_{i}" for i in range(N_OBS)]
    var_names = [f"gene_{i}" for i in range(N_VARS)]
    
    adata = ad.AnnData(
        X=rng.random((N_OBS, N_VARS), dtype=np.float32),
        obs=pd.DataFrame(index=obs_names),
        var=pd.DataFrame(index=var_names),
        layers={
            "counts": rng.random((N_OBS, N_VARS), dtype=np.float32),
            "normalized": sparse.random(N_OBS, N_VARS, density=0.1, format='csr', dtype=np.float32, random_state=sparse_rng),
            "log": rng.random((N_OBS, N_VARS), dtype=np.float64),
        }
    )
    adata.obs.index.name = None
    adata.var.index.name = None
    
    adata.write_h5ad(output_dir / "layers_mixed.h5ad")


def generate_uns_nested():
    """Generate h5ad with nested uns structures."""
    obs_names = [f"cell_{i}" for i in range(N_OBS)]
    var_names = [f"gene_{i}" for i in range(N_VARS)]
    
    adata = ad.AnnData(
        X=rng.random((N_OBS, N_VARS), dtype=np.float32),
        obs=pd.DataFrame(index=obs_names),
        var=pd.DataFrame(index=var_names),
        uns={
            "scalar_int": 42,
            "scalar_float": 3.14,
            "scalar_str": "test_string",
            "scalar_bool": True,
            "array_1d": np.array([1, 2, 3, 4, 5]),
            "array_2d": rng.random((5, 3), dtype=np.float64),
            "nested": {
                "level1": {
                    "level2": "deep_value",
                    "array": np.array([10, 20, 30]),
                },
                "scalar": 100,
            },
            "recarray": np.recarray(
                (5,),
                dtype=[("name", "U10"), ("value", "f4"), ("count", "i4")]
            ),
        }
    )
    # Fill recarray
    for i in range(5):
        adata.uns["recarray"][i] = (f"item_{i}", float(i * 2), i)
    
    adata.obs.index.name = None
    adata.var.index.name = None
    
    adata.write_h5ad(output_dir / "uns_nested.h5ad")


def generate_edge_minimal():
    """Generate minimal valid AnnData (just X)."""
    obs_names = [f"cell_{i}" for i in range(N_OBS)]
    var_names = [f"gene_{i}" for i in range(N_VARS)]
    
    adata = ad.AnnData(
        X=rng.random((N_OBS, N_VARS), dtype=np.float32),
        obs=pd.DataFrame(index=obs_names),
        var=pd.DataFrame(index=var_names)
    )
    adata.obs.index.name = None
    adata.var.index.name = None
    
    adata.write_h5ad(output_dir / "edge_minimal.h5ad")


def generate_edge_empty_obs():
    """Generate h5ad with zero observations."""
    var_names = [f"gene_{i}" for i in range(N_VARS)]
    
    adata = ad.AnnData(
        X=None,
        shape=(0, N_VARS),
        obs=pd.DataFrame(index=pd.Index([], dtype="str")),
        var=pd.DataFrame({"type": rng.choice(["type1", "type2"], N_VARS)}, index=var_names)
    )
    adata.obs.index.name = None
    adata.var.index.name = None
    
    adata.write_h5ad(output_dir / "edge_empty_obs.h5ad")


def generate_edge_unicode():
    """Generate h5ad with unicode in indices and values."""
    obs_names = [f"cell_{i}_αβγ" for i in range(N_OBS)]
    var_names = [f"gene_{i}_日本語" for i in range(N_VARS)]
    
    obs = pd.DataFrame({
        "cluster": rng.choice(["A", "B", "C"], N_OBS),
        "description": [f"Sample {i} with émojis 🧬" for i in range(N_OBS)],
    }, index=obs_names)
    
    var = pd.DataFrame({
        "symbol": [f"GENE_{i}_🎯" for i in range(N_VARS)],
    }, index=var_names)
    
    adata = ad.AnnData(
        X=rng.random((N_OBS, N_VARS), dtype=np.float32),
        obs=obs,
        var=var
    )
    adata.obs.index.name = None
    adata.var.index.name = None
    
    adata.write_h5ad(output_dir / "edge_unicode.h5ad")


def generate_compression_gzip():
    """Generate h5ad with internal gzip compression on datasets.
    
    This tests that nf-anndata can read h5ad files where the HDF5 datasets
    are internally compressed using gzip (deflate) compression. This is
    different from externally gzipping the entire file (.h5ad.gz).
    """
    obs_names = [f"cell_{i}" for i in range(N_OBS)]
    var_names = [f"gene_{i}" for i in range(N_VARS)]
    
    obs = pd.DataFrame({
        "cluster": pd.Categorical(rng.choice(["A", "B", "C"], N_OBS), ordered=False),
        "n_genes": rng.integers(100, 1000, N_OBS, dtype=np.int32),
        "total_counts": rng.random(N_OBS, dtype=np.float32),
    }, index=obs_names)
    
    var = pd.DataFrame({
        "gene_type": pd.Categorical(rng.choice(["protein", "rna"], N_VARS), ordered=False),
        "mean_counts": rng.random(N_VARS, dtype=np.float32),
    }, index=var_names)
    
    adata = ad.AnnData(
        X=rng.random((N_OBS, N_VARS), dtype=np.float32),
        obs=obs,
        var=var,
        obsm={
            "X_pca": rng.random((N_OBS, 10), dtype=np.float32),
        },
        layers={
            "counts": rng.random((N_OBS, N_VARS), dtype=np.float32),
        },
        uns={
            "description": "Test file with gzip compression",
            "compression_level": 4,
        }
    )
    adata.obs.index.name = None
    adata.var.index.name = None
    
    # Write with gzip compression enabled
    adata.write_h5ad(output_dir / "compression_gzip.h5ad", compression="gzip")


def generate_compression_gzip_high():
    """Generate h5ad with high-level gzip compression (level 9).
    
    Tests maximum gzip compression level to ensure nf-anndata handles
    heavily compressed datasets correctly.
    """
    obs_names = [f"cell_{i}" for i in range(N_OBS)]
    var_names = [f"gene_{i}" for i in range(N_VARS)]
    
    # Create sparse matrix - compresses well
    X = sparse.random(N_OBS, N_VARS, density=0.1, format='csr', dtype=np.float32, random_state=sparse_rng)
    
    obs = pd.DataFrame({
        "cluster": pd.Categorical(rng.choice(["A", "B", "C"], N_OBS), ordered=False),
        "batch": rng.choice(["batch1", "batch2"], N_OBS),
    }, index=obs_names)
    
    var = pd.DataFrame({
        "gene_symbol": [f"GENE_{i}" for i in range(N_VARS)],
    }, index=var_names)
    
    adata = ad.AnnData(
        X=X,
        obs=obs,
        var=var,
        layers={
            "normalized": sparse.random(N_OBS, N_VARS, density=0.1, format='csr', dtype=np.float32, random_state=sparse_rng),
        },
    )
    adata.obs.index.name = None
    adata.var.index.name = None
    
    # Write with maximum gzip compression
    adata.write_h5ad(output_dir / "compression_gzip_high.h5ad", compression="gzip", compression_opts=9)


def generate_full_featured():
    """Generate h5ad with all features combined."""
    obs_names = [f"cell_{i}" for i in range(N_OBS)]
    var_names = [f"gene_{i}" for i in range(N_VARS)]
    
    obs = pd.DataFrame({
        "cluster": pd.Categorical(rng.choice(["A", "B", "C"], N_OBS), ordered=False),
        "n_genes": rng.integers(100, 1000, N_OBS, dtype=np.int32),
        "total_counts": rng.random(N_OBS, dtype=np.float32),
        "is_selected": rng.choice([True, False], N_OBS).astype(bool),
    }, index=obs_names)
    
    var = pd.DataFrame({
        "gene_type": pd.Categorical(rng.choice(["protein", "rna"], N_VARS), ordered=False),
        "n_cells": rng.integers(0, N_OBS, N_VARS, dtype=np.int32),
        "mean_counts": rng.random(N_VARS, dtype=np.float32),
    }, index=var_names)
    
    adata = ad.AnnData(
        X=sparse.random(N_OBS, N_VARS, density=0.1, format='csr', dtype=np.float32, random_state=sparse_rng),
        obs=obs,
        var=var,
        obsm={
            "X_pca": rng.random((N_OBS, 10), dtype=np.float32),
            "X_umap": rng.random((N_OBS, 2), dtype=np.float32),
        },
        varm={
            "PCs": rng.random((N_VARS, 10), dtype=np.float32),
        },
        obsp={
            "connectivities": sparse.random(N_OBS, N_OBS, density=0.2, format='csr', dtype=np.float32, random_state=sparse_rng),
        },
        varp={
            "correlations": sparse.random(N_VARS, N_VARS, density=0.2, format='csc', dtype=np.float32, random_state=sparse_rng),
        },
        layers={
            "counts": rng.random((N_OBS, N_VARS), dtype=np.float32),
            "normalized": sparse.random(N_OBS, N_VARS, density=0.1, format='csr', dtype=np.float32, random_state=sparse_rng),
        },
        uns={
            "neighbors": {
                "params": {"n_neighbors": 15, "method": "umap"},
            },
            "pca": {
                "variance": rng.random(10, dtype=np.float32),
            },
            "scalar": 42,
        }
    )
    adata.obs.index.name = None
    adata.var.index.name = None
    
    adata.write_h5ad(output_dir / "full_featured.h5ad")


def generate_pbmc3k():
    """Generate the original pbmc3k_processed.h5ad file (for backwards compatibility)."""
    import scanpy as sc
    import tempfile

    # Use a temporary directory for scanpy's dataset cache to avoid creating
    # a 'data' subdirectory in the script's location
    with tempfile.TemporaryDirectory() as tmpdir:
        sc.settings.datasetdir = tmpdir
        adata = sc.datasets.pbmc3k_processed()
        adata.obs.index.name = None
        adata.var.index.name = None
        adata.layers["counts"] = adata.X
        adata.write_h5ad(output_dir / "pbmc3k_processed.h5ad")


if __name__ == "__main__":
    # List of all generation functions with their display names
    test_generators = [
        ("index_unnamed.h5ad", generate_index_unnamed),
        ("index_named.h5ad", generate_index_named),
        ("index_integer.h5ad", generate_index_integer),
        ("dtypes_numeric.h5ad", generate_dtypes_numeric),
        ("dtypes_categorical.h5ad", generate_dtypes_categorical),
        ("dtypes_boolean.h5ad", generate_dtypes_boolean),
        ("dtypes_nullable.h5ad", generate_dtypes_nullable),
        ("dtypes_string.h5ad", generate_dtypes_string),
        ("x_dense_float32.h5ad", generate_x_dense_float32),
        ("x_dense_float64.h5ad", generate_x_dense_float64),
        ("x_sparse_csr.h5ad", generate_x_sparse_csr),
        ("x_sparse_csc.h5ad", generate_x_sparse_csc),
        ("x_none.h5ad", generate_x_none),
        ("obsm_dense.h5ad", generate_obsm_dense),
        ("obsm_sparse.h5ad", generate_obsm_sparse),
        ("obsm_dataframe.h5ad", generate_obsm_dataframe),
        ("obsp_dense.h5ad", generate_obsp_dense),
        ("obsp_sparse.h5ad", generate_obsp_sparse),
        ("layers_mixed.h5ad", generate_layers_mixed),
        ("uns_nested.h5ad", generate_uns_nested),
        ("edge_minimal.h5ad", generate_edge_minimal),
        ("edge_empty_obs.h5ad", generate_edge_empty_obs),
        ("edge_unicode.h5ad", generate_edge_unicode),
        ("full_featured.h5ad", generate_full_featured),
        ("compression_gzip.h5ad", generate_compression_gzip),
        ("compression_gzip_high.h5ad", generate_compression_gzip_high),
    ]
    
    tqdm.write("Generating comprehensive test h5ad files...")
    tqdm.write("=" * 60)
    
    # Generate all test case files with progress bar
    for filename, generator_func in tqdm(test_generators, desc="Generating test cases", unit="file"):
        generator_func()
    
    # Generate original pbmc3k file
    tqdm.write("\n" + "=" * 60)
    tqdm.write("Generating pbmc3k_processed.h5ad...")
    generate_pbmc3k()
    
    tqdm.write("\n" + "=" * 60)
    tqdm.write(f"All h5ad files generated in '{output_dir}/' directory")
    tqdm.write(f"Total files: {len(test_generators) + 1} (26 test cases + 1 pbmc3k)")
