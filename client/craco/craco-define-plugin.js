const { DefinePlugin } = require('webpack')

/**
 * Tiny wrapper around webpack's DefinePlugin
 * Allows to supply multiple "definitions" as array of either objects or
 * functions that return key-value-pairs of objects
 *
 * Example:
 * {
 *   plugins: [{
 *    plugins: CracoDefinePlugin,
 *    options: {
 *      definitions: [
 *        {
 *          "process.env.TEST1": "123",
 *        },
 *        () => ({
 *          "process.env.TEST2": "456",
 *        })
 *      ]
 *    }
 *   }]
 * }
 */
module.exports = {
  overrideWebpackConfig: ({ webpackConfig, pluginOptions }) => {
    if (!pluginOptions.definitions) {
      return webpackConfig
    }

    // merge all definitions into a flat object. Later values will take
    // precedence over previous ones.
    const definitions = pluginOptions.definitions.reduce((acc, value) => {
      if (typeof value === 'function') return { ...acc, ...value() }
      if (typeof value === 'object') return { ...acc, ...value }
      throw `Invalid value type supplied for define plugin: ${typeof value}\n
      Expected types are object or function.`
    }, {})

    const definePlugin = new DefinePlugin(definitions)

    const { plugins, ...restWebpackConfig } = webpackConfig
    return {
      plugins: [...plugins, definePlugin],
      ...restWebpackConfig
    }
  }
}
