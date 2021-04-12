declare global {
  namespace Cypress {
    interface Chainable<Subject> {
      login(username: string, password: string): void
      visitIndex(): Chainable<Window>
      visitAssignmentsListPage(): Chainable<Window>
      visitTaskPoolPage(): Chainable<Window>
    }
  }
}

Cypress.Commands.add('login', (username, password) => {
  cy.get('#login_username').type(username)

  cy.get('#login_password').type(password)

  cy.contains('Sign in').click()
})

Cypress.Commands.add('visitIndex', () => cy.visit('/'))

Cypress.Commands.add('visitAssignmentsListPage', () => cy.visit('/assignments'))

Cypress.Commands.add('visitTaskPoolPage', () => cy.visit('/tasks/pool'))

// Convert this into a module instead of a script
export {}
