describe('Logout', () => {
  it('logs the user out', () => {
    cy.visitIndex()

    cy.login('teacher', '123')

    cy.contains('Kim Teacher').click()

    cy.contains('Logout').click()

    cy.contains('Successfully signed out').should('exist')
  })
})

// Convert this into a module instead of a script
export {}
