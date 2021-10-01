import { useState } from 'react'
import { Button, Modal } from 'antd'
import { UploadOutlined } from '@ant-design/icons'
import UploadAnswerPage, {
  UploadAnswerPageProps
} from '../pages/answer/UploadAnswerPage'

const UploadAnswerPageButton = ({ answerId }: UploadAnswerPageProps) => {
  const [modalVisible, setModalVisible] = useState(false)
  const showModal = () => setModalVisible(true)
  const hideModal = () => setModalVisible(false)

  return (
    <>
      <Button icon={<UploadOutlined />} type="default" onClick={showModal}>
        Upload Answer
      </Button>
      <Modal
        visible={modalVisible}
        onCancel={hideModal}
        title="Upload Answer"
        footer={[
          <Button type="default" onClick={hideModal} key="cancel">
            Cancel
          </Button>
        ]}
        width="80%"
      >
        <UploadAnswerPage answerId={answerId} />
      </Modal>
    </>
  )
}

export default UploadAnswerPageButton
