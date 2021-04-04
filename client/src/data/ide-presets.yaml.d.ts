export interface IdePreset {
  title: string
  ideImage: string
  ideArguments: string
}

export interface IdePresetGroup {
  title?: string
  items: Record<string, IdePreset>
}

const PresetGroups: {
  [key: string]: IdePresetGroup
}

export default PresetGroups
