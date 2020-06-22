const { DefinePlugin } = require('webpack')
const CracoAntDesignPlugin = require('craco-antd')
const rawLoader = require('craco-raw-loader')
const CopyPlugin = require('copy-webpack-plugin')
const GitRevisionPlugin = require('git-revision-webpack-plugin')
const CracoSvgReactLoaderPlugin = require('./craco/craco-svg-loader-plugin')

const gitRevisionPlugin = new GitRevisionPlugin()

module.exports = {
  plugins: [
    { plugin: CracoAntDesignPlugin },
    { plugin: rawLoader, options: { test: /\.adoc$/ } },
    { plugin: CracoSvgReactLoaderPlugin }
  ],
  webpack: {
    plugins: [
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
        'process.env.BUILD_VERSION': JSON.stringify(
          gitRevisionPlugin.version()
        ),
        'process.env.BUILD_HASH': JSON.stringify(gitRevisionPlugin.commithash())
      })
    ]
  }
}
