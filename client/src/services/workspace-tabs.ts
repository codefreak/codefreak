import React from 'react'

/**
 * The different types for workspace-tabs with their string-representations.
 */
export enum WorkspaceTabType {
  EDITOR = 'editor',
  EMPTY = 'empty',
  INSTRUCTIONS = 'instructions',
  SHELL = 'shell',
  EVALUATION = 'evaluation',
  CONSOLE = 'console',
  FILE_TREE = 'file-tree'
}

/**
 * Represents a tab with title and content-panel for workspaces.
 * It also contains it's type and a path to more precisely describe it's content e.g. for editors with a path to the open file.
 */
export abstract class WorkspaceTab {
  public readonly type: WorkspaceTabType
  public readonly path: string

  protected constructor(type: WorkspaceTabType, path: string) {
    this.type = type
    this.path = path
  }

  /**
   * Renders the title for the tab as a ReactNode, i.e. a string, a string with an icon or with a tooltip.
   */
  public abstract renderTitle(): React.ReactNode

  /**
   * Renders the content-area of the tab.
   */
  public abstract renderContent(): React.ReactNode

  /**
   * Returns a unique string to identify the tab in a query parameter.
   *
   * For example when it returns "tabXY" it will be used as `https://codefreak.test/[...]/ide?leftTab=tabXY`.
   */
  public toActiveTabQueryParam(): string {
    return this.type
  }
}

/**
 * Removes a tab of type `WorkspaceTabType.EDITOR` from an array of WorkspaceTabs
 *
 * @param path the path of the file in the editor-tab
 * @param tabs the tabs to remove from
 */
export const removeEditorTab = (path: string, tabs: WorkspaceTab[]) =>
  tabs.filter(tab => tab.type !== WorkspaceTabType.EDITOR || tab.path !== path)

/**
 * Returns the index of the searched tab in the given array or -1 when not found
 *
 * @param tabs the tabs to search in
 * @param searchTab the tab to search
 */
export const indexOf = (tabs: WorkspaceTab[], searchTab: WorkspaceTab) => {
  let tabIndex = -1
  tabs.forEach((tab, index) => {
    if (tab.type === searchTab.type && tab.path === searchTab.path) {
      tabIndex = index
    }
  })
  return tabIndex
}
