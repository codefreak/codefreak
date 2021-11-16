describe('Workspace editor', () => {
  beforeEach(() => {
    cy.loginTeacher()
    cy.createAndVisitTaskInNewAssignment('python').then(id =>
      cy.updateTaskDetails({
        id,
        protectedFiles: [],
        hiddenFiles: [],
        defaultFiles: ['/main.py']
      })
    )
    cy.logout()

    cy.loginStudent()
    cy.reload()
    cy.url().should('include', '/ide')
    cy.contains('Start working on this task').click()
  })

  it('edits file contents', () => {
    cy.get('.ant-tabs-tab')
      // Wait until the workspace is ready
      .not('.ant-tabs-tab-disabled', { timeout: 20000 })
      .contains('main.py')
      .click()

    cy.url().should('contain', `leftTab=${encodeURIComponent('/')}main.py`)

    cy.get('.workspace-tab-panel')
      .not('.workspace-tab-panel-placeholder', { timeout: 20000 })
      .get('.monaco-editor')
      .should('not.contain.text', 'foo')
      .click('center')
      .type('foo')
      .should('contain.text', 'foo')
  })

  it('saves contents on `ctrl + s`', () => {
    cy.get('.ant-tabs-tab')
      // Wait until the workspace is ready
      .not('.ant-tabs-tab-disabled', { timeout: 20000 })
      .contains('main.py')
      .click()

    cy.url().should('contain', `leftTab=${encodeURIComponent('/')}main.py`)

    cy.get('.workspace-tab-panel')
      .not('.workspace-tab-panel-placeholder', { timeout: 20000 })
      .get('.monaco-editor')
      .click('center')
      // Macs are special
      .type(Cypress.platform === 'darwin' ? '{cmd+s}' : '{ctrl+s}')

    cy.contains('main.py saved')
  })
})

// Convert this into a module instead of a script
export {}
