package acme.plugin

import spock.lang.Requires
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * End-to-end test for remote h5ad staging through the installed Nextflow plugin.
 *
 * Gradle unit tests run on a single classloader and cannot reproduce PF4J/Guava
 * loader conflicts. This test installs the plugin and runs Nextflow with a
 * remote HTTP URL so {@code CacheHelper.hasher()} is exercised in production conditions.
 */
class AnnDataPluginRemoteTest extends Specification {

    static final String REMOTE_URL =
        'https://github.com/nictru/nf-anndata/raw/refs/heads/main/src/test/data/test_cases/pbmc3k_processed.h5ad'

    static final int EXPECTED_N_OBS = 2638
    static final int EXPECTED_N_VARS = 1838

    static Path projectRoot() {
        Paths.get('').toAbsolutePath()
    }

    static String pluginVersion() {
        def matcher = projectRoot().resolve('build.gradle').text =~ /version = '([^']+)'/
        assert matcher.find()
        return matcher.group(1)
    }

    static boolean nextflowAvailable() {
        def cmd = nextflowCommand()
        if (!cmd) {
            return false
        }
        try {
            def proc = new ProcessBuilder(cmd, '-version').redirectErrorStream(true).start()
            return proc.waitFor() == 0
        } catch (IOException ignored) {
            return false
        }
    }

    static boolean networkAvailable() {
        try {
            new URL('https://github.com').openConnection().with {
                connectTimeout = 5000
                readTimeout = 5000
                connect()
            }
            return true
        } catch (Exception ignored) {
            return false
        }
    }

    static String nextflowCommand() {
        def fromEnv = System.getenv('NEXTFLOW_CMD')
        if (fromEnv) {
            return fromEnv
        }

        def pathEnv = System.getenv('PATH') ?: ''
        for (def dir : pathEnv.split(':')) {
            if (!dir) {
                continue
            }
            def candidate = Paths.get(dir, 'nextflow')
            if (Files.isExecutable(candidate)) {
                return candidate.toString()
            }
        }

        for (def candidate : ['/usr/local/bin/nextflow', "${System.getProperty('user.home')}/.micromamba/envs/nf-core/bin/nextflow"]) {
            if (Files.isExecutable(Paths.get(candidate))) {
                return candidate
            }
        }

        return null
    }

    static void installPlugin() {
        def pluginsDir = Paths.get(System.getProperty('user.home'), '.nextflow', 'plugins')
        if (Files.exists(pluginsDir)) {
            Files.list(pluginsDir)
                .filter { it.fileName.toString().startsWith('nf-anndata-') }
                .forEach { path ->
                    path.toFile().deleteDir()
                }
        }

        def proc = new ProcessBuilder('./gradlew', 'installPlugin', '-q')
            .directory(projectRoot().toFile())
            .redirectErrorStream(true)
            .start()
        def output = proc.inputStream.text
        assert proc.waitFor() == 0 : "installPlugin failed:\n${output}"
    }

    @Requires({ nextflowAvailable() && networkAvailable() })
    def 'remote HTTP h5ad staging works via installed plugin'() {
        given:
        installPlugin()
        def workDir = Files.createTempDirectory('nf-anndata-plugin-remote-test')

        workDir.resolve('nextflow.config').text = """\
plugins {
    id 'nf-anndata@${pluginVersion()}'
}
""".stripIndent()

        workDir.resolve('main.nf').text = """\
include { anndata } from 'plugin/nf-anndata'

workflow {
    def ad = anndata(file('${REMOTE_URL}'))
    println "n_obs: \${ad.n_obs}"
    println "n_vars: \${ad.n_vars}"
}
""".stripIndent()

        when:
        def proc = new ProcessBuilder(nextflowCommand(), 'run', 'main.nf')
            .directory(workDir.toFile())
            .redirectErrorStream(true)
            .start()
        def output = proc.inputStream.text
        def exitCode = proc.waitFor()

        then:
        assert exitCode == 0 : "Nextflow failed (exit ${exitCode}):\n${output}"
        output.contains("n_obs: ${EXPECTED_N_OBS}")
        output.contains("n_vars: ${EXPECTED_N_VARS}")

        cleanup:
        workDir.toFile().deleteDir()
    }
}
