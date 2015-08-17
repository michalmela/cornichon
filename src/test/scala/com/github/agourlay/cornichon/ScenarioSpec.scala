package com.github.agourlay.cornichon

import com.github.agourlay.cornichon.core._
import org.scalatest.{ Matchers, WordSpec }

import cats.data.Xor
import Xor.{ left, right }

class ScenarioSpec extends WordSpec with Matchers {

  val resolver = new Resolver
  val engine = new Engine(resolver)

  "A scenario" must {
    "execute all steps" in {
      val session = Session.newSession
      val steps = Seq(Step[Int]("first step", s ⇒ (2 + 1, s), 3))
      val s = Scenario("test", steps)
      engine.runScenario(s)(session).isInstanceOf[SuccessScenarioReport] should be(true)
    }

    "fail if instruction throws exception" in {
      val session = Session.newSession
      val steps = Seq(
        Step[Int]("stupid step", s ⇒ {
          6 / 0
          (2, s)
        }, 2)
      )
      val s = Scenario("scenario with stupid test", steps)
      engine.runScenario(s)(session).isInstanceOf[FailedScenarioReport] should be(true)
    }

    "stop at first failed step" in {
      val session = Session.newSession
      val step1 = Step[Int]("first step", s ⇒ (2, s), 2)
      val step2 = Step[Int]("second step", s ⇒ (5, s), 4)
      val step3 = Step[Int]("third step", s ⇒ (1, s), 1)
      val steps = Seq(
        step1, step2, step3
      )
      val s = Scenario("test", steps)
      engine.runScenario(s)(session) match {
        case s: SuccessScenarioReport ⇒ fail("Should be a FailedScenarioReport")
        case f: FailedScenarioReport ⇒
          f.failedStep.error.msg should be("""
           |expected result was:
           |'4'
           |but actual result is:
           |'5'""".stripMargin.trim)
          f.successSteps should be(Seq(step1.title))
          f.notExecutedStep should be(Seq(step3.title))
      }
    }
  }

}
