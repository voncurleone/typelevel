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
import org.scalajs.dom.HTMLInputElement
import tyrian.cmds.Logger

case class FilterPanel (
    filters: PostFilter = PostFilter(),
    error: Option[String] = None,
    minLikes: Int = 0,
    minDisLikes: Int = 0,
    selectedFilters: Map[String, Set[String]] = Map(),
    dirty: Boolean = false,
    filterAction: Map[String, Set[String]] => App.Msg = _ => App.NoOp
                       ) extends Component[App.Msg, FilterPanel] {
  override def initCmd: Cmd[IO, App.Msg] =
    Commands.getFilters

  override def update(msg: App.Msg): (FilterPanel, Cmd[IO, App.Msg]) = msg match
    case SetFilters(filters) =>
      (this.copy(filters = filters), Cmd.None)

    case FilterPanelError(error) =>
      (this.copy(error = Some(error)), Cmd.None)

    case UpdateLikesInput(likes) =>
      (this.copy(minLikes = likes, dirty = true), Cmd.None)

    case UpdateDisLikesInput(dislikes) =>
      (this.copy(minDisLikes = dislikes, dirty = true), Cmd.None)

    case UpdateCheckBoxs(groupName, value, checked) =>
      val oldGroup = selectedFilters.getOrElse(groupName, Set())
      val newGroup = if(checked) oldGroup + value else oldGroup - value
      val newGroups = selectedFilters + (groupName -> newGroup)
      (this.copy(selectedFilters = newGroups, dirty = true), Logger.consoleLog[IO](s"Filters: $newGroups"))

    case TriggerFilter =>
      (this.copy(dirty = false), Cmd.emit(filterAction(selectedFilters)))

    case _ => (this, Cmd.None)

  override def view(): Html[App.Msg] =
    div(`class` := "filter-panel-container")(
      renderError(),
      renderLikesFilter(),
      renderDisLikesFilter(),
      renderCheckBoxGroup("tags", filters.tags),
      renderFiltersButton()
    )

  //ui
  private def renderError() =
    error.map { e =>
      div(`class` := "filter-panel-error")(e)
    }.getOrElse(div())

  private def renderLikesFilter() =
    div(`class` := "filter-group")(
      h6(`class` := "filter-group-header")("likes"),
      div(`class` := "filter-group-content")(
        label(`for` := "filter-likes")("minimum likes"),
        input(
          `type` := "number",
          id := "filter-likes",
          onInput(likes => if(likes.isEmpty) UpdateLikesInput(0) else UpdateLikesInput(likes.toInt))
        )
      )
    )

  private def renderDisLikesFilter() =
    div(`class` := "filter-group")(
      h6(`class` := "filter-group-header")("dislikes"),
      div(`class` := "filter-group-content")(
        label(`for` := "filter-dislikes")("minimum dislikes"),
        input(
          `type` := "number",
          id := "filter-dislikes",
          onInput(likes => if (likes.isEmpty) UpdateDisLikesInput(0) else UpdateDisLikesInput(likes.toInt))
        )
      )
    )

  private def renderCheckBoxGroup(groupName: String, values: List[String]) =
    val selectedValues = selectedFilters.getOrElse(groupName, Set())

    div(`class` := "filter-group")(
      h6(`class` := "filter-group-header")(groupName),
      div(`class` := "filter-group-content")(
        values.map { value =>
          renderCheckBox(groupName, value, selectedValues)
        }
      )
    )

  private def renderCheckBox(groupName: String, value: String, selectedValues: Set[String]) =
    div(`class` := "filter-group-content")(
      label(`for` := s"filter-$groupName-$value")(value),
      input(
        `type` := "checkbox",
        id := s"filter-$groupName-$value",
        checked(selectedValues.contains(value)),
        onEvent(
          "change",
          event => {
            val checkbox = event.target.asInstanceOf[HTMLInputElement]
            UpdateCheckBoxs(groupName, value, checkbox.checked)
          }
        )
      )
    )

  private def renderFiltersButton() =
    button(
      `type` := "button",
      disabled(!dirty),
      onClick(TriggerFilter)
    )("Apply filyers")
}

object FilterPanel {
  trait Msg extends App.Msg
  case class FilterPanelError(error: String) extends Msg
  case class SetFilters(filters: PostFilter) extends Msg

  //content
  case class UpdateLikesInput(likes: Int) extends Msg
  case class UpdateDisLikesInput(dislikes: Int) extends Msg
  case class UpdateCheckBoxs(groupName: String, value: String, checked: Boolean) extends Msg

  //action
  case object TriggerFilter extends Msg

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