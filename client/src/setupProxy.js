// workaround for https://github.com/facebook/create-react-app/issues/5280

const proxy = require('http-proxy-middleware')

module.exports = function(app) {
  app.use(proxy('/graphql', { target: 'http://localhost:8080' }))
  app.use(proxy('/subscriptions', { target: 'ws://localhost:8080', ws: true }))
}
