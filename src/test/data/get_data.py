#!/usr/bin/env python

import scanpy as sc

adata = sc.datasets.pbmc3k_processed()

adata.obs.index.name = None
adata.var.index.name = None
adata.layers["counts"] = adata.X

adata.write_h5ad("pbmc3k_processed.h5ad")
