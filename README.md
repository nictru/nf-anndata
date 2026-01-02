# nf-plugin-template plugin

## Creating your plugin

The Nextflow plugin template is a scaffold for plugin development. The simplest way to create a new plugin is to use the `nextflow plugin create` sub-command to create a plugin project based on the template.

Create a new plugin with the following command:

```bash
nextflow plugin create
```

See [Creating a plugin](https://www.nextflow.io/docs/latest/guides/gradle-plugin.html#gradle-plugin-create) for more information.

## Building

To build the plugin:

```bash
make assemble
```

## Testing with Nextflow

The plugin can be tested without a local Nextflow installation:

1. Build and install the plugin to your local Nextflow installation: `make install`
2. Run a pipeline with the plugin: `nextflow run hello -plugins nf-plugin-template@0.1.0`

## Publishing

Plugins can be published to a central plugin registry to make them accessible to the Nextflow community. 

Follow these steps to publish the plugin to the Nextflow Plugin Registry:

1. Create a file named `$HOME/.gradle/gradle.properties`, where `$HOME` is your home directory. Add the following properties:
    * `npr.apiKey`: Your Nextflow Plugin Registry access token.
2. Package your plugin and publish it to the registry: `make release`.
