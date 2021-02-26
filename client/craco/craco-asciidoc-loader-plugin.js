const path = require('path')
const { addBeforeLoader, loaderByName } = require('@craco/craco')

module.exports = {
  overrideWebpackConfig: ({ webpackConfig, pluginOptions }) => {
    const asciidocLoader = {
      use: [
        // "Loaders are always called from right to left"
        // so raw-loader will be called with the result from asciidoc loader
        'raw-loader',
        path.resolve(__dirname, 'webpack-asciidoc-loader')
      ],
      ...pluginOptions
    }

    addBeforeLoader(webpackConfig, loaderByName('file-loader'), asciidocLoader)

    return webpackConfig
  }
}
