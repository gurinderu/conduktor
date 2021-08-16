package io.conduktor.graphql

import caliban.CalibanError.{ExecutionError, ParsingError, ValidationError}
import caliban.ResponseValue.ObjectValue
import caliban.Value.StringValue
import caliban.{CalibanError, GraphQLInterpreter, ResponseValue}
import io.conduktor.service.ServiceError

final case class GraphqlError(
  code: String,
  message: String,
  metadata: Map[String, ResponseValue] = Map.empty
) extends RuntimeException
    with Serializable

object GraphqlError {
  val unexpected: GraphqlError = GraphqlError("UNEXPECTED", "Unexpected error")

  def apply(serviceError: ServiceError): GraphqlError =
    GraphqlError(
      code = "UNKNOWN",
      message = serviceError.getMessage
    )

  def withErrorCodeExtensions[R](
    interpreter: GraphQLInterpreter[R, CalibanError]
  ): GraphQLInterpreter[R, CalibanError] =
    interpreter.mapError {
      case err @ ExecutionError(_, _, _, Some(error: GraphqlError), _) =>
        err.copy(
          extensions = Some(
            ObjectValue(
              List(
                ("errorCode", StringValue(error.code)),
                ("metadata", ObjectValue(error.metadata.toList))
              )
            )
          ),
          msg = error.message
        )
      case err: ExecutionError =>
        err.copy(extensions = Some(ObjectValue(List(("errorCode", StringValue("EXECUTION_ERROR"))))))
      case err: ValidationError =>
        err.copy(extensions = Some(ObjectValue(List(("errorCode", StringValue("VALIDATION_ERROR"))))))
      case err: ParsingError =>
        err.copy(extensions = Some(ObjectValue(List(("errorCode", StringValue("PARSING_ERROR"))))))
    }
}
