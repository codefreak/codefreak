package de.code_freak.codefreak.entity

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

internal class JpaPersistableTest {
  class StubEntity(
    id: Long? = null
  ) : JpaPersistable<Long>(id)

  val ID: Long = 1337

  val entity = StubEntity(ID)

  @Test
  fun `entity and null are not equal`() {
    assertFalse(entity.equals(null))
  }

  @Test
  fun `entity and other object are not equal`() {
    assertFalse(entity.equals("foo"))
  }

  @Test
  fun `entity and itself are equal`() {
    assertTrue(entity.equals(entity))
  }

  @Test
  fun `entities with different id are not equal`() {
    val other = StubEntity(ID + 1)
    assertFalse(entity.equals(other))
  }

  @Test
  fun `entities with same id are equal`() {
    val other = StubEntity(ID)
    assertTrue(entity.equals(other))
  }
}
