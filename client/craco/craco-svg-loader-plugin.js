const { addBeforeLoader, loaderByName } = require('@craco/craco')

module.exports = {
  overrideWebpackConfig: ({ webpackConfig, pluginOptions }) => {
    const reactSvgLoader = {
      test: /\.svg$/,
      use: {
        loader: 'svg-react-loader'
      },
      ...pluginOptions
    }

    addBeforeLoader(webpackConfig, loaderByName('file-loader'), reactSvgLoader)

    return webpackConfig
  }
}
