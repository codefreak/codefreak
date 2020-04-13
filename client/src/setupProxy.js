// workaround for https://github.com/facebook/create-react-app/issues/5280

const proxy = require('http-proxy-middleware')

const serverPort = process.env.SERVER_PORT || 8080

module.exports = function(app) {
  app.use(proxy(`http://localhost:${serverPort}/api`))
  app.use(proxy('/graphql', { target: `http://localhost:${serverPort}` }))
  app.use(
    proxy('/subscriptions', {
      target: `ws://localhost:${serverPort}`,
      ws: true
    })
  )
}
