describe('Workspace terminal', () => {
  beforeEach(() => {
    cy.loginTeacher()
    cy.createAndVisitTaskInNewAssignment('python')
    cy.logout()

    cy.loginStudent()
    cy.reload()
    cy.url().should('include', '/ide')
    cy.contains('Start working on this task').click()
  })

  it('runs the program in a shell when the run-button is clicked', () => {
    cy.contains('Run')
      // Wait until the workspace is ready
      .not('.ant-btn-loading', { timeout: 20000 })
      .get('button')
      .contains('Run')
      .click()

    cy.url().should('contain', 'rightTab=console')
    cy.get('.shell-root').should('exist')
  })

  it('allows input', () => {
    cy.get('.ant-tabs-tab')
      .not('.ant-tabs-tab-disabled', { timeout: 20000 })
      .contains('Shell')
      .click()

    cy.url().should('contain', 'rightTab=shell')
    cy.get('.shell-root').should('exist').type('typing foo in cypress')
    // The terminal is currently a canvas so we can't assert the text output
  })
})

// Convert this into a module instead of a script
export {}
