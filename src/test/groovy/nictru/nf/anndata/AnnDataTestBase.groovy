package nictru.nf.anndata

import java.nio.file.Path
import java.nio.file.Paths
import spock.lang.Specification

/**
 * Base class for AnnData integration tests
 * 
 * Provides common setup and test file utilities
 */
abstract class AnnDataTestBase extends Specification {

    def setup() {
        def testFile = Paths.get('src/test/data/pbmc3k_processed.h5ad')
        if (!testFile.toFile().exists()) {
            throw new FileNotFoundException("Test file not found: ${testFile.toAbsolutePath()}. Tests require src/test/data/pbmc3k_processed.h5ad")
        }
    }

    protected Path findTestFile() {
        return Paths.get('src/test/data/pbmc3k_processed.h5ad')
    }
}

