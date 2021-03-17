const { addBeforeLoader, loaderByName } = require('@craco/craco')

module.exports = {
  overrideWebpackConfig: ({ webpackConfig, pluginOptions }) => {
    const yamlLoader = {
      test: /\.ya?ml$/,
      type: 'json',
      use: [
        {
          loader: 'yaml-loader'
        }
      ],
      ...pluginOptions
    }

    addBeforeLoader(webpackConfig, loaderByName('file-loader'), yamlLoader)

    return webpackConfig
  }
}
