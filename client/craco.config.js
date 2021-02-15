const CracoAntDesignPlugin = require('craco-antd')
const CracoRawLoaderPlugin = require('craco-raw-loader')
const CracoSvgReactLoaderPlugin = require('./craco/craco-svg-loader-plugin')
const CracoDefinePlugin = require('./craco/craco-define-plugin')
const GitRevisionPlugin = require('git-revision-webpack-plugin')

/**
 * Add globals to environment that expose build year and version/hash from Git
 * via DefinePlugin
 */
const createBuildDefinitions = () => {
  const gitRevisionPlugin = new GitRevisionPlugin()
  return {
    'process.env.BUILD_YEAR': JSON.stringify(new Date().getFullYear()),
    'process.env.BUILD_VERSION': JSON.stringify(gitRevisionPlugin.version()),
    'process.env.BUILD_HASH': JSON.stringify(gitRevisionPlugin.commithash())
  }
}

module.exports = {
  plugins: [
    { plugin: CracoAntDesignPlugin },
    { plugin: CracoRawLoaderPlugin, options: { test: /\.adoc$/ } },
    {
      plugin: CracoSvgReactLoaderPlugin,
      options: {
        exclude: /bootstrap-less\/fonts/ // prevent svg fonts from being processed
      }
    },
    {
      plugin: CracoDefinePlugin,
      options: {
        definitions: [createBuildDefinitions]
      }
    }
  ]
}
