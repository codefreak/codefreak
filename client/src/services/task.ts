import { compare } from './util'
import { matches } from './strings'
import {
  GetTaskPoolForAddingQueryResult,
  GetTaskPoolQueryResult
} from './codefreak-api'

type TaskToAdd = NonNullable<
  GetTaskPoolForAddingQueryResult['data']
>['taskPool'][number]

type TaskPoolItem = NonNullable<
  GetTaskPoolQueryResult['data']
>['taskPool'][number]

const sortByNewest = (
  a: TaskToAdd | TaskPoolItem,
  b: TaskToAdd | TaskPoolItem
) => {
  const result = compare(a.updatedAt, b.updatedAt, value => Date.parse(value))

  // Reverse the order, if both exist
  // The list has to be reverse sorted, because newer timestamps are greater than older ones
  return a.updatedAt && b.updatedAt ? -1 * result : result
}

export const TaskSortMethods: Record<
  string,
  (a: TaskToAdd | TaskPoolItem, b: TaskToAdd | TaskPoolItem) => number
> = {
  NEWEST: (a, b) => sortByNewest(a, b),
  OLDEST: (a, b) => sortByNewest(b, a)
}

export const TaskSortMethodNames = Object.keys(TaskSortMethods)

export function filterTasks<T extends TaskToAdd>(list: T[], criteria: string) {
  return list.filter(task => matches(criteria, task.title))
}
