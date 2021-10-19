// Setup matchMedia for ant-design
// see https://github.com/ant-design/ant-design/issues/21096#issuecomment-725301551
global.matchMedia =
  global.matchMedia ||
  function () {
    return {
      matches: false,
      addListener: jest.fn(),
      removeListener: jest.fn()
    }
  }
