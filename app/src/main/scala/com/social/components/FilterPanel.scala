package com.social.components

import cats.effect.IO
import com.social.App
import com.social.common.*
import com.social.domain.post.PostFilter
import com.social.pages.Page
import tyrian.{Cmd, Html}
import tyrian.Html.*
import tyrian.http.{HttpError, Method, Response}
import io.circe.generic.auto.*
import FilterPanel.*

case class FilterPanel (
    filters: PostFilter = PostFilter(),
    error: Option[String] = None
                       ) extends Component[App.Msg, FilterPanel] {
  override def initCmd: Cmd[IO, App.Msg] =
    Commands.getFilters

  override def update(msg: App.Msg): (FilterPanel, Cmd[IO, App.Msg]) = msg match
    case SetFilters(filters) =>
      (this.copy(filters = filters), Cmd.None)

    case FilterPanelError(error) =>
      (this.copy(error = Some(error)), Cmd.None)

    case _ => (this, Cmd.None)

  override def view(): Html[App.Msg] =
    div(`class` := "filter-panel-container")(
      renderError(),
      div(filters.toString)
    )

  private def renderError() =
    error.map { e =>
      div(`class` := "filter-panel-error")(e)
    }.getOrElse(div())
}

object FilterPanel {
  trait Msg extends App.Msg
  case class FilterPanelError(error: String) extends Msg
  case class SetFilters(filters: PostFilter) extends Msg

  object EndPoints {
    val getFilters: Endpoint[Msg] = new Endpoint[Msg] {
      override val location: String = Constants.endpoints.filters
      override val method: Method = Method.Get
      override val onError: HttpError => Msg = e => FilterPanelError(e.toString)
      override val onResponse: Response => Msg =
        Endpoint.onResponse[PostFilter, Msg](
          SetFilters.apply,
          FilterPanelError.apply
        )
    }
  }

  object Commands {
    def getFilters: Cmd[IO, Msg] = EndPoints.getFilters.call()
  }
}