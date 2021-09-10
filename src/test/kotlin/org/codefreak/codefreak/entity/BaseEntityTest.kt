package org.codefreak.codefreak.entity

import java.util.UUID
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class BaseEntityTest {
  companion object {
    val ID1 = UUID(0, 0)
    val ID2 = UUID(0, 1)
  }

  class StubEntity(id: UUID) : BaseEntity(id)

  val entity = StubEntity(ID1)

  @Test
  fun `entity and null are not equal`() {
    Assertions.assertFalse(entity.equals(null))
  }

  @Test
  fun `entity and other object are not equal`() {
    Assertions.assertFalse(entity.equals("foo"))
  }

  @Test
  fun `entity and itself are equal`() {
    Assertions.assertTrue(entity.equals(entity))
    Assertions.assertEquals(entity.hashCode(), entity.hashCode())
  }

  @Test
  fun `entities with different id are not equal`() {
    val other = StubEntity(ID2)
    Assertions.assertFalse(entity.equals(other))
    Assertions.assertNotEquals(entity.hashCode(), other.hashCode())
  }

  @Test
  fun `entities with same id are equal`() {
    val other = StubEntity(ID1)
    Assertions.assertTrue(entity.equals(other))
    Assertions.assertEquals(entity.hashCode(), other.hashCode())
  }
}
