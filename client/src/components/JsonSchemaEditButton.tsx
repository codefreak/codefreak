import { Button, Modal } from 'antd'
import { ButtonProps } from 'antd/lib/button'
import { JSONSchema6 } from 'json-schema'
import React, { createRef, memo, ReactNode, useState } from 'react'
import Form from 'react-jsonschema-form'
import './JsonSchemaEditButton.less'

interface JsonSchemaEditButtonProps<T> {
  title: string
  value: T
  schema: JSONSchema6
  onSubmit: (newValue: T) => Promise<unknown>
  extraContent?: ReactNode
  buttonProps?: ButtonProps
}

function JsonSchemaEditButton<T>(
  props: React.PropsWithChildren<JsonSchemaEditButtonProps<T>>
) {
  const [idPrefix] = useState(Math.random().toString(36).substring(2))
  const [modalVisible, setModalVisible] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const showModal = () => setModalVisible(true)
  const hideModal = () => setModalVisible(false)
  const onSubmit = ({ formData }: { formData: T }) => {
    setSubmitting(true)
    props
      .onSubmit(formData)
      .then(hideModal)
      .finally(() => setSubmitting(false))
  }

  // react-jsonschema-form does not support programmatic form submission
  // so we have to add a hidden submit button and trigger a click on it
  // @see https://github.com/rjsf-team/react-jsonschema-form/issues/500
  const submitRef = createRef<HTMLButtonElement>()
  const trySubmit = () => submitRef.current && submitRef.current.click()
  const fakeSubmit = (
    <button className="fake-submit" type="submit" ref={submitRef}>
      Submit
    </button>
  )

  return (
    <>
      <Button
        icon="edit"
        type="link"
        onClick={showModal}
        {...props.buttonProps}
      />
      <Modal
        visible={modalVisible}
        onCancel={hideModal}
        title={props.title}
        okButtonProps={{ loading: submitting }}
        onOk={trySubmit}
      >
        {props.extraContent}
        <div className="bootstrap">
          <Form
            showErrorList={false}
            schema={props.schema}
            formData={props.value}
            idPrefix={idPrefix}
            onSubmit={onSubmit}
            children={fakeSubmit}
          />
        </div>
      </Modal>
    </>
  )
}

export default memo(JsonSchemaEditButton) as typeof JsonSchemaEditButton
