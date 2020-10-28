import loadable from '@loadable/component'

export const HelpPage = loadable(() => import('./HelpPage'))

export const BasicHelpPage = loadable(() => import('./BasicHelpPage'))

export const DefinitionsHelpPage = loadable(
  () => import('./DefinitionsHelpPage')
)

export const IdeHelpPage = loadable(() => import('./IdeHelpPage'))
