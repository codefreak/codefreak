import { useApolloClient } from '@apollo/client'
import moment from 'moment'
import React, { useCallback, useContext, useEffect, useState } from 'react'
import {
  TimeSyncDocument,
  TimeSyncQuery,
  TimeSyncQueryVariables
} from '../generated/graphql'

const ServerTimeOffsetContext =
  React.createContext<number | undefined>(undefined)
export const ServerTimeOffsetProvider = ServerTimeOffsetContext.Provider

/**
 * Returns the time offset between client and server in milliseconds
 * The number might not be 100% precise but good enough for our purpose
 *
 * The calculation is a simplified version of NTP:
 * https://stackoverflow.com/a/15785110/1526257
 *
 * 1. Client sends current timestamp to server (CT1)
 * 2. Server responds with received client time (CT1) and current server time (ST)
 * 3. Client receives both timestamps and stores another timestamp when
 *    he received the message (CT2)
 * 4. We can now calculate two offsets:
 *    SENDING = ST - CT1
 *    RECEIVING = CT2 - ST
 *    These offsets sill contain the time-on-wire which has to be subtracted
 * 5. We calculate the roundtrip time (RT) and the average time for one direction (OT)
 *    RT = Sending + Receiving
 *    OT = RT / 2
 * 6. We can now calculate the real time offset between client and server by
 *    calculating the delta between SENDING and OT
 *    OFFSET = SENDING - OT
 *
 * We could repeat this steps n times to get a more precise value, but this should
 * be sufficient for current needs.
 */
export const useCalculatedServerTimeOffset = (): number | undefined => {
  const apollo = useApolloClient()
  const [offset, setOffset] = useState<number | undefined>(undefined)

  useEffect(() => {
    // we use apollo.query() and not the hooks to be independent from
    // React's rendering schedule. Otherwise the rendering times would cause
    // an additional delay and affect the calculated offset.
    apollo
      .query<TimeSyncQuery, TimeSyncQueryVariables>({
        query: TimeSyncDocument,
        variables: {
          clientTimestamp: Date.now()
        },
        fetchPolicy: 'network-only'
      })
      .then(result => {
        const {
          timeSync: { serverTimestamp, clientTimestamp }
        } = result.data
        const responseTimestamp = Date.now()
        const receiving = responseTimestamp - serverTimestamp
        const sending = serverTimestamp - clientTimestamp
        const roundTrip = receiving + sending
        const oneway = Math.floor(roundTrip / 2)
        const newOffset = sending - oneway
        setOffset(newOffset)
      })
      .catch(e => {
        console.warn(
          'Could not fetch time offset. Relative times might be incorrect.',
          e
        )
        setOffset(0)
      })
  }, [setOffset, apollo])

  return offset
}

/**
 * Always returns a time offset from ServerTimeOffsetContext
 * or throws and exception if no offset has been set, yet.
 */
const useServerTimeOffset = (): number => {
  const offset = useContext<number | undefined>(ServerTimeOffsetContext)
  if (offset === undefined) {
    throw Error('Time offset from server has not been calculated.')
  }
  return offset
}

/**
 * Returns a function that can be used instead of Date.now()
 */
export const useServerNow = (): (() => number) => {
  const offset = useServerTimeOffset()

  return useCallback(() => {
    return Date.now() + (offset ?? 0)
  }, [offset])
}

/**
 * Returns a function that can be used instead of moment()
 */
export const useServerMoment = (): (() => moment.Moment) => {
  const now = useServerNow()
  return useCallback(() => moment(now()), [now])
}

export default useServerTimeOffset
