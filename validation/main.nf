include { anndata } from 'plugin/nf-anndata'

workflow {
    // Example: Load an AnnData file and access its properties
    // This example demonstrates the plugin functionality using the test data file
    
    def testFile = file('src/test/data/pbmc3k_processed.h5ad', checkIfExists: true)
    
    Channel.of(testFile)
        .map { file ->
            def ad = anndata(file.toString())
            
            // Access basic properties
            println "AnnData object: ${ad.n_obs} × ${ad.n_vars}"
            
            // Access obs dataframe
            println "Obs columns: ${ad.obs.colnames.join(', ')}"
            println "Obs row names (first 5): ${ad.obs.rownames.take(5).join(', ')}"
            
            // Access var dataframe
            println "Var columns: ${ad.var.colnames.join(', ')}"
            println "Var row names (first 5): ${ad.var.rownames.take(5).join(', ')}"
            
            // Access available fields
            if (!ad.layers.isEmpty()) {
                println "Layers: ${ad.layers.join(', ')}"
            }
            if (!ad.obsm.isEmpty()) {
                println "obsm: ${ad.obsm.join(', ')}"
            }
            if (!ad.varm.isEmpty()) {
                println "varm: ${ad.varm.join(', ')}"
            }
            if (!ad.obsp.isEmpty()) {
                println "obsp: ${ad.obsp.join(', ')}"
            }
            if (!ad.varp.isEmpty()) {
                println "varp: ${ad.varp.join(', ')}"
            }
            if (!ad.uns.isEmpty()) {
                println "uns: ${ad.uns.join(', ')}"
            }
            
            // Access column data (if available)
            if (ad.obs.colnames.size() > 0) {
                def firstCol = ad.obs.colnames[0]
                def col = ad.obs.get(firstCol)
                println "Column '${firstCol}' has ${col.n_unique()} unique values"
                if (col.unique.size() > 0) {
                    println "Unique values (first 10): ${col.unique.take(10).join(', ')}"
                }
            }
            
            ad.close()
            return file
        }
        .view()
}
