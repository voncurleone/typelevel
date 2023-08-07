package com.social.pages
import cats.effect.IO
import com.social.domain.post.{Post, PostFilter}
import tyrian.Html.*
import tyrian.{Cmd, Html}
import PostFeedPage.*
import com.social.App
import com.social.common.{Constants, Endpoint}
import tyrian.http.{HttpError, Method, Response, Status}
import io.circe.parser.*
import io.circe.generic.auto.*

final case class PostFeedPage(
    posts: List[Post] = List(),
    canLoadMore: Boolean = true,
    status: Option[Page.Status] = Some(Page.Status("Loading", Page.StatusKind.LOADING))
                             ) extends Page {
  override def initCmd: Cmd[IO, Page.Msg] =
    Commands.getPosts()

  override def update(msg: Page.Msg): (Page, Cmd[IO, Page.Msg]) = msg match
    case AddPosts(list, clm) =>
      (setSuccessStatus("Loaded").copy(posts = this.posts ++ list, canLoadMore = clm), Cmd.None)

    case SetErrorStatus(e) =>
      (setSuccessStatus(e), Cmd.None)

    case LoadMorePosts =>
      (this, Commands.getPosts(offset = posts.length))

    case _ =>
      (this, Cmd.None)


  override def view(): Html[Page.Msg] =
    div(`class` := "posts-container")(
      posts.map(post => renderPost(post)) ++ maybeRenderLoadMore
    )

  //ui
  private def renderPost(post: Post) =
    div(`class` := "post-card")(
      div(`class` := "post-card-content")(
        h4(post.email),
        div(`class` := "post-card-img")(
          img(
            `class` := "post-image",
            src := post.postInfo.image.getOrElse(""),
            alt := post.id.toString
          )
        ),
        div(`class` := "post-text")(post.postInfo.text),
        div(`class` := "post-tags")(post.postInfo.tags.getOrElse(List()).foldLeft("")((acc, t) => acc + t + " ").trim),
        div(`class` := "post-metrics")(s"Likes: ${post.postInfo.likes} DisLikes: ${post.postInfo.disLikes}")
      ),
      //todo: add like and dislike buttons( and their functionality )
      //div(s"${post.email} - ${post.postInfo.text}")
    )

  //ui
  private def maybeRenderLoadMore: Option[Html[App.Msg]] = status.map { s =>
    div(`class` := "load-more-action")(
      s match
        case Page.Status(_, Page.StatusKind.LOADING) => div("Loading...")
        case Page.Status(e, Page.StatusKind.ERROR) => div(e)
        case Page.Status(_, Page.StatusKind.SUCCESS) =>
          if(canLoadMore)
            button(`type` := "button", onClick(LoadMorePosts))("Load more")
          else
            div("All jobs loaded")
    )
  }

  //util
  def setErrorStatus(message: String): PostFeedPage =
    this.copy(status = Some(Page.Status(message = message, kind = Page.StatusKind.ERROR)))

  def setSuccessStatus(message: String): PostFeedPage =
    this.copy(status = Some(Page.Status(message = message, kind = Page.StatusKind.SUCCESS)))
}

object PostFeedPage {
  trait Msg extends App.Msg
  case class SetErrorStatus(error: String) extends Msg
  case class AddPosts(list: List[Post], canLoadMore: Boolean) extends Msg

  //action
  case object LoadMorePosts extends Msg

  object Endpoints {
    def getPosts(limit: Int = Constants.defaultPageSize, offset: Int = 0): Endpoint[Msg] = new Endpoint[Msg] {
      override val location: String = Constants.endpoints.posts + s"?limit=$limit&offset=$offset"
      override val method: Method = Method.Post
      override val onError: HttpError => Msg = e => SetErrorStatus(e.toString)
      override val onResponse: Response => Msg = response => response.status match
        case Status(s, _) if s >= 200 && s < 300 =>
          val json = response.body
          val parsed = parse(json).flatMap(_.as[List[Post]])
          parsed match
            case Left(error) => SetErrorStatus(s"Parsing error: $error")
            case Right(list) => AddPosts(list, canLoadMore = offset == 0 || list.nonEmpty)

        case Status(s, m) if s >= 400 && s < 600 =>
          SetErrorStatus(s"Error: $m")
    }
  }

  object Commands {
    def getPosts(filter: PostFilter = PostFilter(),
                 limit: Int = Constants.defaultPageSize,
                 offset: Int = 0): Cmd[IO, Msg] = Endpoints.getPosts(limit, offset).call(filter)
  }
}