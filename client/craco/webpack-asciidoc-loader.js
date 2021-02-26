const asciidoctor = require('asciidoctor')()

/**
 * This should be used in conjunction with raw-loader to consume the converted
 * HTML as string.
 */
module.exports = function (content) {
  this.cacheable()
  return asciidoctor.convert(content)
}
