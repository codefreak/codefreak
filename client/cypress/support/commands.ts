import { shorten } from '../../src/services/short-id'
import {
  AddTasksToAssignmentDocument,
  AddTasksToAssignmentMutation,
  AddTasksToAssignmentMutationVariables,
  CreateAssignmentDocument,
  CreateAssignmentMutation,
  CreateAssignmentMutationVariables,
  CreateTaskDocument,
  CreateTaskMutation,
  CreateTaskMutationVariables,
  GetTaskListDocument,
  GetTaskListQuery,
  GetTaskListQueryVariables,
  LoginDocument,
  LoginMutation,
  LoginMutationVariables,
  LogoutDocument,
  LogoutMutation,
  TaskDetailsInput,
  UpdateAssignmentDocument,
  UpdateAssignmentMutation,
  UpdateAssignmentMutationVariables,
  UpdateTaskDetailsDocument,
  UpdateTaskDetailsMutation,
  UpdateTaskDetailsMutationVariables
} from '../../src/generated/graphql'
import { DocumentNode } from 'graphql'
import Chainable = Cypress.Chainable
import Response = Cypress.Response
import moment from 'moment'

declare global {
  namespace Cypress {
    interface Chainable<Subject> {
      graphqlRequest<TResult, TVariables = Record<string, unknown>>(
        document: DocumentNode,
        variables?: TVariables,
        operationName?: string
      ): Chainable<Response<{ data: TResult }>>
      login(username: string, password: string): void
      loginAdmin(): void
      loginTeacher(): void
      loginStudent(): void
      logout(): void
      updateTaskDetails(details: TaskDetailsInput): void
      createAndVisitTaskInNewAssignment(
        templateName?: string | null,
        setAssignmentActive?: boolean
      ): Chainable<string>
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

Cypress.Commands.add(
  'graphqlRequest',
  <TResult, TVariables = Record<string, unknown>>(
    document: DocumentNode,
    variables?: TVariables,
    operationName?: string
  ): Chainable<Response<{ data: TResult }>> =>
    cy.request('POST', '/graphql', {
      operationName: operationName,
      query: document.loc?.source.body,
      variables
    })
)

Cypress.Commands.add('loginAdmin', () =>
  cy.graphqlRequest<LoginMutation, LoginMutationVariables>(LoginDocument, {
    username: 'admin',
    password: '123'
  })
)

Cypress.Commands.add('loginTeacher', () =>
  cy.graphqlRequest<LoginMutation, LoginMutationVariables>(LoginDocument, {
    username: 'teacher',
    password: '123'
  })
)

Cypress.Commands.add('loginStudent', () =>
  cy.graphqlRequest<LoginMutation, LoginMutationVariables>(LoginDocument, {
    username: 'student',
    password: '123'
  })
)

Cypress.Commands.add('logout', () =>
  cy.graphqlRequest<LogoutMutation>(LogoutDocument)
)

Cypress.Commands.add('updateTaskDetails', (details: TaskDetailsInput) =>
  cy.graphqlRequest<
    UpdateTaskDetailsMutation,
    UpdateTaskDetailsMutationVariables
  >(UpdateTaskDetailsDocument, { input: details })
)

Cypress.Commands.add(
  'createAndVisitTaskInNewAssignment',
  (templateName: string | null = null, setAssignmentActive = true) => {
    let assignmentId: string
    let taskId: string
    let taskInAssignmentId: string

    return cy
      .graphqlRequest<
        CreateAssignmentMutation,
        CreateAssignmentMutationVariables
      >(CreateAssignmentDocument)
      .then(({ body }) => {
        assignmentId = body.data.createAssignment.id

        return cy.graphqlRequest<
          CreateTaskMutation,
          CreateTaskMutationVariables
        >(CreateTaskDocument, { templateName })
      })
      .then(({ body }) => {
        taskId = body.data.createTask.id

        return cy.graphqlRequest<
          AddTasksToAssignmentMutation,
          AddTasksToAssignmentMutationVariables
        >(AddTasksToAssignmentDocument, {
          assignmentId: assignmentId,
          taskIds: [taskId]
        })
      })
      .then(() => {
        return cy.graphqlRequest<
          UpdateAssignmentMutation,
          UpdateAssignmentMutationVariables
        >(UpdateAssignmentDocument, {
          id: assignmentId,
          title: 'Test Assignment',
          active: setAssignmentActive,
          openFrom: moment().toISOString()
        })
      })
      .then(() => {
        return cy.graphqlRequest<GetTaskListQuery, GetTaskListQueryVariables>(
          GetTaskListDocument,
          {
            assignmentId
          }
        )
      })
      .then(({ body }) => {
        taskInAssignmentId = body.data.assignment.tasks[0].id

        cy.visit(`/tasks/${shorten(taskInAssignmentId)}`)
        cy.url().should('include', `/tasks/${shorten(taskInAssignmentId)}`)
      })
      .then(() => {
        return Promise.resolve(taskInAssignmentId)
      })
  }
)

Cypress.Commands.add('visitIndex', () => cy.visit('/'))

Cypress.Commands.add('visitAssignmentsListPage', () => cy.visit('/assignments'))

Cypress.Commands.add('visitTaskPoolPage', () => cy.visit('/tasks/pool'))

// Convert this into a module instead of a script
export {}
