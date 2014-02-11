package com.technophobia.substeps

import scala.Some
import com.technophobia.substeps.model._
import com.technophobia.substeps.model.Feature
import com.technophobia.substeps.model.Tag
import scala.Some

class FeatureFileParser extends AbstractParser[Feature] {

  protected override def entryPoint = featureFile

  private def featureFile: Parser[Feature] = opt(tagDef <~ rep1(eol)) ~ (featureDef <~ rep1(eol)) ~ (rep(scenario) <~ rep(eol)) ^^ {

    case (Some(tags) ~ featureName ~ scenarios) => Feature(featureName, scenarios, tags.toSet)
    case (None ~ featureName ~ scenarios) => Feature(featureName, scenarios, Set())
  }

  private def tagDef: Parser[List[Tag]] = "Tags:" ~> opt(whiteSpace) ~> repsep(tag, whiteSpace)

  private def tag: Parser[Tag]   = """[1-9A-Za-z-_~]+""".r ^^ (Tag(_))

  private def featureDef: Parser[String] = "Feature:" ~> opt(whiteSpace) ~> """[^\r\n]+""".r

  private def scenario: Parser[Scenario] = basicScenario | scenarioOutline

  private def basicScenario: Parser[BasicScenario] = (opt(tagDef <~ eol) ~ scenarioDef <~ eol) ~ rep1sep(substepInvocation, eol) <~ rep(eol) ^^ {

    case (Some(tags) ~ scenarioName ~ substepInvocations) => BasicScenario(scenarioName, substepInvocations, tags.toSet)
    case (None ~ scenarioName ~ substepInvocations) => BasicScenario(scenarioName, substepInvocations, Set())
  }

  def substepInvocation: Parser[String] = """([^:\r\n])+""".r ^^ (_.trim)

  private def scenarioDef: Parser[String] = "Scenario:" ~> opt(whiteSpace) ~> """[^\n\r]+""".r

  private def scenarioOutline: Parser[OutlinedScenario] = opt(tagDef <~ rep1(eol)) ~ (scenarioOutlineDef <~ rep1(eol)) ~ (rep1sep(substepInvocation, eol) <~ rep(eol)) ~ exampleSection <~ rep(eol) ^^ {

    case (Some(tags) ~ scenarioName ~ substeps ~ examples) => OutlinedScenario(scenarioName, substeps, examples, tags.toSet)
    case (None ~ scenarioName ~ substeps ~ examples) => OutlinedScenario(scenarioName, substeps, examples, Set())

  }

  private def scenarioOutlineDef: Parser[String] = "Scenario Outline:" ~> opt(whiteSpace) ~> """[^\n\r]+""".r

  private def exampleSection: Parser[List[Map[String, String]]] = ("Examples:" ~ opt(whiteSpace) ~ rep1(eol)) ~> (lineOfCells <~ rep1(eol)) ~ repsep(lineOfCells, eol) ^^ {

    case (headings ~ examples) => for(example <- examples; examplesWithHeading = headings.zip(example)) yield Map(examplesWithHeading:_*)
  }

  private def lineOfCells: Parser[List[String]] = rep(cellSeparator ~> """[^|\n\r ]*(?=([ ]*\|))""".r) <~ cellSeparator

  private def cellSeparator: Parser[Any] = opt(whiteSpace) ~ "|" ~ opt(whiteSpace)

}