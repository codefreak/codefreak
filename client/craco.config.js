const CracoAntDesignPlugin = require('craco-antd')
const MonacoWebpackPlugin = require('monaco-editor-webpack-plugin')
const rawLoader = require('craco-raw-loader')

module.exports = {
  plugins: [
    { plugin: CracoAntDesignPlugin },
    { plugin: rawLoader, options: { test: /\.adoc$/ } }
  ],
  webpack: {
    plugins: [new MonacoWebpackPlugin()]
  }
}
