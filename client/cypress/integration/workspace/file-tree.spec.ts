describe('Workspace file-tree', () => {
  beforeEach(() => {
    cy.loginTeacher()
    cy.createAndVisitTaskInNewAssignment('python')
    cy.logout()

    cy.loginStudent()
    cy.reload()
    cy.url().should('include', '/ide')
    cy.contains('Start working on this task').click()
  })

  it('shows files and directories', () => {
    cy.get('.ant-tabs-tab')
      // Wait until the workspace is ready
      .not('.ant-tabs-tab-disabled', { timeout: 20000 })

    cy.get('.ant-tabs-tab').contains('Files')

    cy.get('.ant-tree')
      .should('contain.text', 'main.py')
      .should('contain.text', '.vscode')
  })

  it('loads contents of subdirectories', () => {
    cy.loginStudent()
    cy.get('.ant-tabs-tab')
      // Wait until the workspace is ready
      .not('.ant-tabs-tab-disabled', { timeout: 20000 })

    cy.get('.ant-tree').contains('.vscode').click()
    cy.get('.ant-tree').contains('extensions.json')
    cy.get('.ant-tree').contains('launch.json')
  })

  it('opens files in an editor when selected', () => {
    cy.get('.ant-tabs-tab')
      // Wait until the workspace is ready
      .not('.ant-tabs-tab-disabled', { timeout: 20000 })

    cy.get('.ant-tree').contains('main.py').click()

    cy.url().should('contain', `leftTab=${encodeURIComponent('/')}main.py`)
    cy.get('.ant-tabs-tab').contains('main.py').should('have.length', 1)
  })

  it('opens a right-click-menu', () => {
    cy.get('.ant-tabs-tab')
      // Wait until the workspace is ready
      .not('.ant-tabs-tab-disabled', { timeout: 20000 })

    cy.get('.ant-tree').contains('main.py').rightclick()

    cy.get('.ant-dropdown').contains('Rename')
    cy.get('.ant-dropdown').contains('Delete')
    cy.get('.ant-dropdown').contains('Add file')
    cy.get('.ant-dropdown').contains('Add directory')
  })

  it('adds new files and directories', () => {
    cy.get('.ant-tabs-tab')
      // Wait until the workspace is ready
      .not('.ant-tabs-tab-disabled', { timeout: 20000 })

    cy.get('.ant-tree')
      .should('not.contain.text', 'foo.txt')
      .contains('main.py')
      .rightclick()

    cy.get('.ant-dropdown').contains('Add file').click()

    cy.get('.ant-tree-title')
      .find('.ant-input')
      .should('have.value', '')
      .type('foo.txt{enter}')

    cy.get('.ant-tree')
      .should('contain.text', 'foo.txt')
      .should('not.contain.text', 'bar')
      .contains('main.py')
      .rightclick()

    cy.get('.ant-dropdown')
      .contains('Add directory')
      .should('have.value', '')
      .type('bar{enter}')

    cy.get('.ant-tree')
      .should('contain.text', 'foo.txt')
      .should('contain.text', 'bar')
  })

  it('deletes files and directories', () => {
    cy.get('.ant-tabs-tab')
      // Wait until the workspace is ready
      .not('.ant-tabs-tab-disabled', { timeout: 20000 })

    cy.get('.ant-tree').contains('main.py').rightclick()

    cy.get('.ant-dropdown').contains('Delete').click()

    cy.get('button').contains('Delete').click()

    cy.get('.ant-tree').contains('main.py').should('not.exist')
  })
})

// Convert this into a module instead of a script
export {}
