const { DefinePlugin } = require('webpack')
const CracoAntDesignPlugin = require('craco-antd')
const MonacoWebpackPlugin = require('monaco-editor-webpack-plugin')
const rawLoader = require('craco-raw-loader')
const CopyPlugin = require('copy-webpack-plugin')
const GitRevisionPlugin = require('git-revision-webpack-plugin')

const gitRevisionPlugin = new GitRevisionPlugin()

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
      ]),
      new GitRevisionPlugin(),
      new DefinePlugin({
        'process.env.BUILD_YEAR': JSON.stringify(new Date().getFullYear()),
        'process.env.BUILD_VERSION': JSON.stringify(gitRevisionPlugin.version()),
        'process.env.BUILD_HASH': JSON.stringify(gitRevisionPlugin.commithash())
      })
    ]
  }
}
