import React from 'react'

export enum WorkspaceTabType {
  EDITOR = 'editor',
  EMPTY = 'empty',
  INSTRUCTIONS = 'instructions',
  SHELL = 'shell',
  EVALUATION = 'evaluation',
  CONSOLE = 'console'
}

export abstract class WorkspaceTab {
  public readonly type: WorkspaceTabType
  public readonly path: string

  protected constructor(type: WorkspaceTabType, path: string) {
    this.type = type
    this.path = path
  }

  public abstract renderTitle(): React.ReactNode

  public abstract renderContent(loading: boolean): React.ReactNode
}
