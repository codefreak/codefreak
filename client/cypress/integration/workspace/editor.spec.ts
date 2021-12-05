if (!Cypress.env('skipKubernetesTests')) {
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

      // Wait until the workspace is ready
      // Timeout from `cy.get` is passed down to `should`
      cy.get('.ant-tabs-tab', { timeout: 20000 })
        .should('not.have.class', 'ant-tabs-tab-disabled')
        .contains('main.py')
        .click()

      cy.url().should('contain', `leftTab=${encodeURIComponent('/')}main.py`)
    })

    it('edits file contents', () => {
      cy.get('.workspace-tab-panel')
        .not('.workspace-tab-panel-placeholder', { timeout: 20000 })
        .get('.monaco-editor')
        .should('not.contain.text', 'foo')
        .click('center')
        .type('foo')
        .should('contain.text', 'foo')
    })

    it('saves contents on `ctrl + s`', () => {
      cy.get('.workspace-tab-panel')
        .not('.workspace-tab-panel-placeholder', { timeout: 20000 })
        .get('.monaco-editor')
        .click('center')
        // Macs are special
        .type(Cypress.platform === 'darwin' ? '{cmd+s}' : '{ctrl+s}')

      cy.contains('main.py saved')
    })
  })
}

// Convert this into a module instead of a script
export {}
