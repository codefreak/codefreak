const CracoAntDesignPlugin = require('craco-antd')
const MonacoWebpackPlugin = require('monaco-editor-webpack-plugin')

module.exports = {
  plugins: [{ plugin: CracoAntDesignPlugin }],
  webpack: {
    plugins: [new MonacoWebpackPlugin()]
  }
}
