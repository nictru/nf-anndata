package nictru.nf.anndata

import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files
import spock.lang.Specification

/**
 * Base class for AnnData tests
 * 
 * Provides utilities for finding test files and common test helpers
 */
abstract class AnnDataTestBase extends Specification {

    protected static final Path TEST_DATA_DIR = Paths.get('src/test/data/test_cases')
    
    /**
     * Find all h5ad test files in the test_cases directory
     */
    protected static List<Path> findAllTestFiles() {
        if (!Files.exists(TEST_DATA_DIR)) {
            return []
        }
        return Files.list(TEST_DATA_DIR)
            .filter { it.toString().endsWith('.h5ad') }
            .sorted()
            .collect { it }
    }
    
    /**
     * Find the default test file (pbmc3k_processed.h5ad)
     */
    protected Path findTestFile() {
        return findTestFile('pbmc3k_processed.h5ad')
    }
    
    /**
     * Find a specific test file by name
     */
    protected Path findTestFile(String filename) {
        def file = TEST_DATA_DIR.resolve(filename)
        if (!Files.exists(file)) {
            throw new FileNotFoundException("Test file not found: ${file.toAbsolutePath()}")
        }
        return file
    }
    
    /**
     * Safely close an AnnData instance
     */
    protected void closeAnnData(AnnData ad) {
        try {
            ad?.close()
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }
}
