package com.social.pages

import cats.effect.IO
import com.social.common.{Constants, Endpoint}
import com.social.domain.post.Post
import PostPage.{Commands, DislikeAction, InteractionSuccess, LikeAction, SetError, SetPost}
import tyrian.{Cmd, Html}
import tyrian.Html.*
import tyrian.http.*
import io.circe.generic.auto.*
import com.social.App
import tyrian.cmds.Logger

//for rendering markdown
import laika.api.*
import laika.format.*

final case class PostPage(id: String,
                          post: Option[Post] = None,
                          status: Page.Status = Page.Status.LOADING
                         ) extends Page {
  override def initCmd: Cmd[IO, Page.Msg] =
    Commands.getPost(id) |+| Logger.consoleLog[IO]("init cmd for post page")

  override def update(msg: Page.Msg): (Page, Cmd[IO, Page.Msg]) = msg match
    case SetError(error) =>
      (setErrorStatus(error), Cmd.None)

    case SetPost(post) =>
      (setSuccessStatus("Success").copy(post = Some(post)), Logger.consoleLog[IO]("SetPost success"))

    case LikeAction => post match
      case Some(p) =>
        (this, Commands.like(p) |+| Logger.consoleLog[IO]("liked"))
      case None =>
        (this, Logger.consoleLog[IO]("liked but no post"))

    case DislikeAction =>
      (this, Logger.consoleLog[IO]("disliked"))

    case InteractionSuccess(like) =>
      (this, Logger.consoleLog[IO]("todo: implement interaction success"))

    case _ =>
      (this, Cmd.None)

  override def view(): Html[Page.Msg] = post match
    case None => renderNoPostPage()
    case Some(post) => renderPostPage(post)

  //util
  private val markdownTransformer =
    Transformer
      .from(Markdown)
      .to(HTML)
      .build

  def setErrorStatus(message: String): PostPage =
    this.copy(status = Page.Status(message = message, kind = Page.StatusKind.ERROR))

  def setSuccessStatus(message: String): PostPage =
    this.copy(status = Page.Status(message = message, kind = Page.StatusKind.SUCCESS))

  //ui
  private def renderPostPage(post: Post) =
    div(`class` := "post-page")(
      div(`class` := "post-hero")(
        img(
          `class` := "post-image",
          src := post.postInfo.image.getOrElse(""),
          alt := post.id.toString
        ),
        h1(post.email)
      ),
      div(`class` := "post-content")(
        renderPostContent(post)
      ),
      div(`class` := "post-interaction")(
        button(`type` := "button", onClick(LikeAction))("Like"),
        button(`type` := "button", onClick(DislikeAction))("Dislike")
      )
    )

  private def renderPostContent(post: Post) =
    val textHtml = markdownTransformer.transform(post.postInfo.text) match
      case Left(e) => s"ERROR in markdown transformation: $e"
      case Right(text) => text

    div(
      div(`class` := "post-tags")(
        ul(`class` := "post-tag-list")(
          post.postInfo.tags.getOrElse(List()).map(tag => li(tag))
        )
      ),
      div(`class` := "post-text")(/*post.postInfo.text*/).innerHtml(textHtml),
      div(`class` := "post-metrics")(
        s"Likes: ${post.postInfo.likes}, disLikes: ${post.postInfo.disLikes}"
      )
    )

  private def renderNoPostPage() = status.kind match
    case Page.StatusKind.LOADING =>
      div("Loading")

    case Page.StatusKind.ERROR =>
      div("This Post doesnt exist 😢")

    case Page.StatusKind.SUCCESS =>
      div("server healthy but no job...🤔")
}

object PostPage {
  trait Msg extends App.Msg
  case class SetError(error: String) extends Msg
  case class SetPost(post: Post) extends Msg

  case object LikeAction extends Msg
  case object DislikeAction extends Msg
  case class InteractionSuccess(like: Boolean) extends Msg

  object Endpoints {
    def getPost(id: String): Endpoint[Msg] = new Endpoint[Msg] {
      override val location: String = Constants.endpoints.posts + s"/$id"
      override val method: Method = Method.Get
      override val onError: HttpError => Msg = e => SetError(e.toString)
      override val onResponse: Response => Msg =
        Endpoint.onResponse[Post, Msg](
          SetPost.apply,
          SetError.apply
        )
    }

    def interact(like: Boolean, post: Post): Endpoint[Msg] = new Endpoint[Msg]:
      override val location: String = Constants.endpoints.posts + s"/${post.id}"
      override val method: Method = Method.Put
      override val onError: HttpError => Msg = e => SetError(e.toString)
      override val onResponse: Response => Msg = response => response.status match {
        case Status(200, _) => InteractionSuccess(like)
        case _ => SetError("Server error :(")
      }
  }

  object Commands {
    def getPost(id: String): Cmd[IO, Msg] = Endpoints.getPost(id).call()

    def like(post: Post): Cmd[IO, Msg] =
      val updatedPost = post.copy(postInfo = post.postInfo.copy(likes = post.postInfo.likes + 1))
      Endpoints.interact(true, post).authorizedCall(updatedPost)

    def dislike(): Cmd[IO, Msg] = ???
  }
}
