const { DefinePlugin } = require('webpack')
const CracoAntDesignPlugin = require('craco-antd')
const rawLoader = require('craco-raw-loader')
const GitRevisionPlugin = require('git-revision-webpack-plugin')
const CracoSvgReactLoaderPlugin = require('./craco/craco-svg-loader-plugin')

const gitRevisionPlugin = new GitRevisionPlugin()

module.exports = {
  plugins: [
    { plugin: CracoAntDesignPlugin },
    { plugin: rawLoader, options: { test: /\.adoc$/ } },
    {
      plugin: CracoSvgReactLoaderPlugin,
      options: {
        exclude: /bootstrap-less\/fonts/ // prevent svg fonts from being processed
      }
    }
  ],
  webpack: {
    plugins: [
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
