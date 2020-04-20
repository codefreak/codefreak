// workaround for https://github.com/facebook/create-react-app/issues/5280

const proxy = require('http-proxy-middleware')

const serverPort = process.env.SERVER_PORT || 8080

module.exports = function(app) {
  ;['/api', '/graphql', '/lti/login'].forEach(path => {
    app.use(proxy(`http://localhost:${serverPort}${path}`))
  })
  app.use(
    proxy('/subscriptions', {
      target: `ws://localhost:${serverPort}`,
      ws: true
    })
  )
}
