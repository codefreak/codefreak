import { HTMLAttributes } from 'react'
import { TaskTemplate } from '../generated/graphql'

interface Template {
  title: string
  description: string
  logo: React.ComponentType<HTMLAttributes<SVGElement>>
}

const ALL_TEMPLATES: {
  [key in TaskTemplate]: Template
} = {
  CSHARP: {
    title: 'C#',
    description: '.NET, NUnit, Code Climate',
    logo: require('devicon/icons/csharp/csharp-plain.svg')
  },
  CPP: {
    title: 'C++',
    description: 'Google Test (gtest)',
    logo: require('devicon/icons/cplusplus/cplusplus-plain.svg')
  },
  JAVA: {
    title: 'Java',
    description: 'JUnit, Code Climate',
    logo: require('devicon/icons/java/java-plain.svg')
  },
  JAVASCRIPT: {
    title: 'JavaScript',
    description: 'Jest, Code Climate',
    logo: require('devicon/icons/javascript/javascript-plain.svg')
  },
  PYTHON: {
    title: 'Python',
    description: 'pytest, Code Climate',
    logo: require('devicon/icons/python/python-plain.svg')
  }
}

export const getAllTemplates = () => ALL_TEMPLATES
