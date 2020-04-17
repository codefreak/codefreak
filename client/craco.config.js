const CracoAntDesignPlugin = require('craco-antd')
const MonacoWebpackPlugin = require('monaco-editor-webpack-plugin')
const rawLoader = require('craco-raw-loader')
const CopyPlugin = require('copy-webpack-plugin')

module.exports = {
  plugins: [
    { plugin: CracoAntDesignPlugin },
    { plugin: rawLoader, options: { test: /\.adoc$/ } }
  ],
  webpack: {
    plugins: [
      new MonacoWebpackPlugin(),
      new CopyPlugin([
        {
          from: 'node_modules/bootstrap-less/fonts/*',
          to: 'fonts',
          flatten: true
        }
      ])
    ]
  }
}
