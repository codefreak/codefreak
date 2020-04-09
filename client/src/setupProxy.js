// workaround for https://github.com/facebook/create-react-app/issues/5280

const proxy = require('http-proxy-middleware')

const proxyHost = process.env.NODE_PROXY_HOST || 'localhost'
const proxyPort = process.env.NODE_PROXY_PORT || '8080'
const proxyUrl = `${proxyHost}:${proxyPort}`

module.exports = function(app) {
  app.use(proxy(`http://${proxyUrl}/api`))
  app.use(proxy('/graphql', { target: `http://${proxyUrl}` }))
  app.use(proxy('/subscriptions', { target: `ws://${proxyUrl}`, ws: true }))
}
