package com.social.pages
import cats.effect.IO
import com.social.App
import com.social.common.*
import com.social.core.Session
import com.social.domain.post.PostInfo
import tyrian.Html.*
import tyrian.http.*
import tyrian.*
import tyrian.cmds.Logger
import io.circe.generic.auto.*
import io.circe.parser.*
import org.scalajs.dom.{File, FileReader}
import cats.syntax.traverse.*

case class MakePostPage(
                         text: String = "",
                         //likes: Int = 0, //these will always be zero when creating a post
                         //disLikes: Int = 0, //input type("kind") would be "number"
                         tags: Option[String] = None, //parse tags before sending to the server
                         image: Option[String] = None,
                         status: Option[Page.Status] = None
                       )
  extends FormPage("Create a post", status) {
    import MakePostPage.*

    override protected def renderFormContent(): List[Html[App.Msg]] = List(
      renderTextArea("Text", "text", true, UpdateText.apply),
      renderInput("Tags", "tags", "text", true, UpdateTags.apply),
      renderImageUploadInput("Image", "image", image, UpdateImageFile.apply),
      button(`type` := "button", onClick(AttemptPost))("post")
    )

    override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = msg match
      case UpdateText(text) =>
        (this.copy(text = text), Cmd.None)

      case UpdateTags(tags) =>
        (this.copy(tags = Some(tags)), Cmd.None)

      case UpdateImageFile(maybeFile) =>
        (this, Commands.loadFile(maybeFile))

      case UpdateImage(image) =>
        (this.copy(image = image), Logger.consoleLog[IO](s"image: $image"))

      case PostFailure(error) =>
        (setErrorStatus(error), Cmd.None)

      case PostSuccess(postId) =>
        (setSuccessStatus("Success!"), Logger.consoleLog[IO](s"Post Id: $postId"))

      case AttemptPost =>
        (this, Commands.makePost(text, tags, image))

      case _ => (this, Cmd.None)

    override def view(): Html[App.Msg] =
      if(Session.isActive)
        super.view()
      else
        renderInvalidPage

    //ui
    private def renderInvalidPage =
      div(
        h1("Post Page"),
        div("You're not logged in.")
      )

    //util
    def setErrorStatus(message: String): Page =
      this.copy(status = Some(Page.Status(message = message, kind = Page.StatusKind.ERROR)))

    def setSuccessStatus(message: String): Page =
      this.copy(status = Some(Page.Status(message = message, kind = Page.StatusKind.SUCCESS)))
}

object MakePostPage {
  trait Msg extends App.Msg
  case class UpdateText(text: String) extends Msg
  case class UpdateTags(tags: String) extends Msg
  case class UpdateImageFile(file: Option[File]) extends Msg
  case class UpdateImage(image: Option[String]) extends Msg

  //actions
  case object AttemptPost extends Msg
  case class PostFailure(error: String) extends Msg
  case class PostSuccess(postId: String) extends Msg

  object Endpoints {
    val makePost: Endpoint[Msg] = new Endpoint[Msg] {
      override val location: String = Constants.endpoints.makePost
      override val method: Method = Method.Post
      override val onError: HttpError => Msg = e => PostFailure(e.toString)
      override val onResponse: Response => Msg = response => response.status match
        case Status(s, _) if s > 100 && s < 300 =>
          val postId = response.body
          PostSuccess(postId)

        case Status(401, _) =>
          PostFailure("Unauthorized")

        case Status(s, _) if s > 400 && s < 500 =>
          val json = response.body
          val parsed = parse(json).flatMap(_.hcursor.get[String]("error"))
          parsed match
            case Left(e) => PostFailure(s"Error: $e")
            case Right(e) => PostFailure(e)

        case _ => PostFailure("Unknown error")
    }
  }

  object Commands {
    def makePost(text: String, tags: Option[String], image: Option[String]): Cmd[IO, Msg] =
      Endpoints.makePost.authorizedCall(
        PostInfo(
          text,
          0,
          0,
          tags.map(_.split(",").map(_.trim).toList),
          image
        )
      )

    def loadFile(maybeFile: Option[File]): Cmd[IO, Msg] =
      Cmd.Run[IO, Option[String], Msg](
        maybeFile.traverse { file =>
          IO.async_ { cb =>
            //create reader
            val reader = new FileReader
            //set onload
            reader.onload = _ =>
              cb(Right(reader.result.toString))
            //trigger the reader
            reader.readAsDataURL(file)
          }
        }
      )(UpdateImage.apply)
  }
}