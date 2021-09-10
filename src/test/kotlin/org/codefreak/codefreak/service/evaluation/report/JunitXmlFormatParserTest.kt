package org.codefreak.codefreak.service.evaluation.report

import org.codefreak.codefreak.entity.Feedback
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test

class JunitXmlFormatParserTest {
  @Test
  fun parse() {
    val parser = JunitXmlFormatParser()
    val feedback = parser.parse("""
<?xml version="1.0" encoding="utf-8"?>
<testsuites>
        <testsuite name="pytest" errors="0" failures="1" skipped="0" tests="2" time="0.016" timestamp="2021-08-06T12:34:02.621108" hostname="arch-desktop">
                <testcase classname="main_test" name="test_function" time="0.000">
                        <failure message="assert 0 == 5&#10; +  where 0 = add(2, 3)">def test_function():
&gt;       assert add(2, 3) == 5
E       assert 0 == 5
E        +  where 0 = add(2, 3)

main_test.py:8: AssertionError</failure>
                </testcase>
                <testcase classname="main_test" name="test_always_true" time="0.000" />
        </testsuite>
</testsuites>
""".trimIndent())
    assertThat(feedback, Matchers.hasSize(2))
    val first = feedback.first()
    assertThat(first.status, Matchers.equalTo(Feedback.Status.FAILED))
    assertThat(first.summary, Matchers.equalTo("test_function"))
    assertThat(first.longDescription, Matchers.equalTo("```\nassert 0 == 5\n +  where 0 = add(2, 3)\n```"))
    val second = feedback[1]
    assertThat(second.status, Matchers.equalTo(Feedback.Status.SUCCESS))
    assertThat(second.summary, Matchers.equalTo("test_always_true"))
  }
}
