import React from 'react'
import { QuestionCircleOutlined } from '@ant-design/icons'

/**
 * Because our task templates do not ship with an icon we have to maintain
 * a list of icons that will be compiled into the frontend. Additionally,
 * the devicon icon names do not match our template names. Maybe one day
 * someone comes up with a more clever idea how to maintain icons for our
 * task templates.
 */
const languageIconMap: Record<string, React.ElementType> = {
  cpp: require('devicon/icons/cplusplus/cplusplus-plain.svg'),
  csharp: require('devicon/icons/csharp/csharp-plain.svg'),
  java: require('devicon/icons/java/java-plain.svg'),
  javascript: require('devicon/icons/javascript/javascript-plain.svg'),
  python: require('devicon/icons/python/python-plain.svg')
}

/**
 * Return an icon for the given language name or a default question mark icon
 * if none is defined.
 *
 * @param language The name of the programming language (name of the task template)
 */
const getIconForLanguage = (language: string): React.ElementType => {
  if (languageIconMap[language.toLowerCase()] !== undefined) {
    return languageIconMap[language.toLowerCase()]
  }
  return QuestionCircleOutlined
}

export interface LanguageIconProps {
  language: string
}

const LanguageIcon: React.FC<LanguageIconProps> = props => {
  const { language } = props
  const Icon = getIconForLanguage(language)
  return <Icon className="language-logo" />
}

export default LanguageIcon
