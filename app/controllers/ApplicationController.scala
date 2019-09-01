package controllers

import javax.inject._
import model.ProductRepo
import model.SchemaDefinition.QueryType
import play.api.libs.json._
import play.api.mvc._
import sangria.ast.Document
import sangria.execution.{ErrorWithResolver, Executor, QueryAnalysisError}
import sangria.parser.{QueryParser, SyntaxError}
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future
import scala.util.{Failure, Success}
import sangria.marshalling.playJson._
import sangria.schema.Schema

@Singleton
class ApplicationController extends InjectedController {

  def graphql: Action[JsValue] = Action.async(parse.json) { request ⇒
    val schema = Schema(QueryType)
    val query = (request.body \ "query").as[String]
    val operation = (request.body \ "operationName").asOpt[String]

    def parseVariables(variables: String) =
      if (variables.trim == "" || variables.trim == "null") Json.obj() else Json.parse(variables).as[JsObject]

    val variables = (request.body \ "variables").toOption.flatMap {
      case JsString(vars) => Some(parseVariables(vars))
      case obj: JsObject => Some(obj)
      case _ => None
    }

    val json = JsObject(Seq(
      "error" -> JsString("Not Found JSON")
    ))

    QueryParser.parse(query) match {
      case Success(queryAst) ⇒ executeGraphQLQuery(schema, queryAst, operation, variables.getOrElse(json))

      case Failure(error: SyntaxError) ⇒
        Future.successful(BadRequest(Json.obj("error" → error.getMessage)))
    }
  }

  def executeGraphQLQuery(schema: Schema[ProductRepo, Unit], query: Document, op: Option[String], vars: JsObject): Future[Result] =
    Executor.execute(schema, query, new ProductRepo, operationName = op, variables = vars)
      .map(Ok(_))
      .recover {
        case error: QueryAnalysisError ⇒ BadRequest(error.resolveError)
        case error: ErrorWithResolver ⇒ InternalServerError(error.resolveError)
      }

}
