if (!Cypress.env('skipKubernetesTests')) {
  describe('Online IDE', () => {
    beforeEach(() => {
      cy.loginTeacher()
      cy.createAndVisitTaskInNewAssignment('python')
      cy.logout()

      cy.loginStudent()
      cy.reload()
      cy.url().should('include', '/ide')
      cy.contains('Start working on this task').click()
    })

    it('opens successfully', () => {
      cy.get('.workspace-page')
        .find('.workspace-tabs-wrapper')
        .should('have.length', 3)
    })

    it('switches between tabs', () => {
      // Wait until the workspace is ready
      // Timeout from `cy.get` is passed down to `should`
      cy.get('.ant-tabs-tab', { timeout: 20000 })
        .should('not.have.class', 'ant-tabs-tab-disabled')
        .contains('Instructions')
        .click()
      cy.url().should('contain', 'rightTab=instructions')

      cy.get('.ant-tabs-tab')
        .not('.ant-tabs-tab-disabled', { timeout: 20000 })
        .contains('Shell')
        .click()
      cy.url().should('contain', 'rightTab=shell')

      cy.get('.ant-tabs-tab')
        .not('.ant-tabs-tab-disabled', { timeout: 20000 })
        .contains('Console')
        .click()
      cy.url().should('contain', 'rightTab=console')

      cy.get('.ant-tabs-tab')
        .not('.ant-tabs-tab-disabled', { timeout: 20000 })
        .contains('Evaluation-Results')
        .click()
      cy.url().should('contain', 'rightTab=evaluation')
    })
  })
}

// Convert this into a module instead of a script
export {}
