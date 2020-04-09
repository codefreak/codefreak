package org.codefreak.codefreak.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

val ID1 = UUID(0, 0)
val ID2 = UUID(0, 1)

internal class BaseEntityTest {
  class StubEntity(id: UUID) : BaseEntity(id)

  val entity = StubEntity(ID1)

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
    assertEquals(entity.hashCode(), entity.hashCode())
  }

  @Test
  fun `entities with different id are not equal`() {
    val other = StubEntity(ID2)
    assertFalse(entity.equals(other))
    assertNotEquals(entity.hashCode(), other.hashCode())
  }

  @Test
  fun `entities with same id are equal`() {
    val other = StubEntity(ID1)
    assertTrue(entity.equals(other))
    assertEquals(entity.hashCode(), other.hashCode())
  }
}
