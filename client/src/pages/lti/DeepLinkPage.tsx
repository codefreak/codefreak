import { Button, Checkbox, Icon, List, Typography } from 'antd'
import { createRef, useEffect, useState } from 'react'
import AsyncPlaceholder from '../../components/AsyncContainer'
import {
  Assignment,
  useCreateLtiDeepLinkResponseMutation,
  useGetAssignmentListQuery
} from '../../generated/graphql'
import { useQueryParam } from '../../hooks/useQuery'
import NotFoundPage from '../NotFoundPage'
import { extractTargetChecked } from '../../services/util'
import { HIDE_NAVIGATION_QUERY_PARAM } from '../../hooks/useHideNavigation'

const DeepLinkPage: React.FC = () => {
  const result = useGetAssignmentListQuery()
  const jwtId = useQueryParam('jwt')
  const [
    createLtiDeepLinkResponse,
    { data: deepLinkData }
  ] = useCreateLtiDeepLinkResponseMutation()
  const formRef = createRef<HTMLFormElement>()
  const [hideNavigation, setHideNavigation] = useState(true)

  useEffect(() => {
    if (
      formRef.current &&
      deepLinkData &&
      deepLinkData.ltiCreateDeepLinkResponse
    ) {
      formRef.current.submit()
    }
  }, [deepLinkData, formRef])

  if (!jwtId) {
    return <NotFoundPage />
  }

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  const onItemSelection = (assignmentId: string) => () => {
    const additionalQuery = hideNavigation
      ? HIDE_NAVIGATION_QUERY_PARAM + '=true'
      : null
    createLtiDeepLinkResponse({
      variables: { assignmentId, additionalQuery, jwtId }
    }).then(({ data }) => {
      if (!data || !data.ltiCreateDeepLinkResponse) {
        return
      }
      const postData = new FormData()
      postData.append('JWT', data.ltiCreateDeepLinkResponse.signedJwt)
    })
  }

  let redirectUrl = ''
  let jwt = ''
  if (deepLinkData && deepLinkData.ltiCreateDeepLinkResponse) {
    redirectUrl = deepLinkData.ltiCreateDeepLinkResponse.redirectUrl
    jwt = deepLinkData.ltiCreateDeepLinkResponse.signedJwt
  }

  const renderListItem = (
    item: Pick<Assignment, 'id' | 'status' | 'title'>
  ) => (
    <List.Item
      actions={[
        <Button
          key={item.id}
          type="default"
          icon="link"
          onClick={onItemSelection(item.id)}
        />
      ]}
    >
      <List.Item.Meta title={item.title} description={item.status} />
    </List.Item>
  )

  return (
    <>
      <Typography.Title level={2}>Select assignment</Typography.Title>
      <p>
        Create link to an assignment by clicking the <Icon type="link" />{' '}
        button.
      </p>
      <p>
        <Checkbox
          checked={hideNavigation}
          onChange={extractTargetChecked(setHideNavigation)}
        >
          Single assignment view (hide navigation)
        </Checkbox>
      </p>
      <List
        style={{ backgroundColor: 'white' }}
        bordered
        dataSource={result.data.assignments}
        renderItem={renderListItem}
      />
      <form
        style={{ display: 'none' }}
        method="POST"
        action={redirectUrl}
        ref={formRef}
      >
        <input type="hidden" name="JWT" value={jwt} />
      </form>
    </>
  )
}

export default DeepLinkPage
