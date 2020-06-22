const { addBeforeLoader, loaderByName } = require('@craco/craco')

module.exports = {
  overrideWebpackConfig: ({ webpackConfig }) => {
    const reactSvgLoader = {
      test: /\.svg$/,
      use: {
        loader: 'svg-react-loader'
      }
    }

    addBeforeLoader(webpackConfig, loaderByName('file-loader'), reactSvgLoader)

    return webpackConfig
  }
}
