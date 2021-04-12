describe('Assignment', () => {
  it('creates an empty assignment', () => {
    cy.visitAssignmentsListPage()

    cy.login('teacher', '123')

    cy.contains('Create Assignment').click()

    cy.get('.ant-modal')
      .get('#new-assignment-title')
      .type('My empty assignment')

    cy.get('.ant-modal-footer')
      .contains('Create Assignment')
      .should('not.be.disabled')

    cy.get('.ant-modal-footer').contains('Create Assignment').click()

    cy.contains('Assignment created').should('exist')
  })
})

// Convert this into a module instead of a script
export {}
