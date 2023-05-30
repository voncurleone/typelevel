package com.social.core

import cats.*
import cats.effect.*
import cats.implicits.*
import com.social.config.EmailServiceConfig


import java.util.Properties
import javax.mail.*
import javax.mail.internet.MimeMessage

trait Emails[F[_]] {
  def sendEmail(to: String, subject: String, content: String): F[Unit]
  def sendPasswordRecoveryEmail(to: String, token: String): F[Unit]
}

class LiveEmails[F[_]: MonadCancelThrow] private (emailServiceConfig: EmailServiceConfig) extends Emails[F] {
  val host = emailServiceConfig.host
  val port = emailServiceConfig.port
  val user = emailServiceConfig.user
  val pass = emailServiceConfig.pass
  val frontEndUrl = emailServiceConfig.frontEndUrl

  override def sendEmail(to: String, subject: String, content: String): F[Unit] = {
    val messageResource = for {
      prop <- propResource
      auth <- authResource
      session <- createSession(prop, auth)
      message <- createMessage(session)("NoReply@social.com", to, subject, content)
    } yield message

    messageResource.use(msg => Transport.send(msg).pure[F])
  }

  override def sendPasswordRecoveryEmail(to: String, token: String): F[Unit] = {
    val subject = "Email from Social"
    val content =
      s"""
         |<div style="
         |  border: 1px solid black;
         |  padding: 20px;
         |  font-family: sans-serif;
         |  line-height: 2;
         |  font-size: 20px;
         |">
         |<h1> social password recovery 🦁</h1>
         |<p>Your password recovery token is: $token</p>
         |<p>
         |  Click <a href="$frontEndUrl/login"> here</a> to get back to the application
         |</p>
         |</div>""".stripMargin

    sendEmail(to, subject, content)
  }

  //private
  val propResource: Resource[F, Properties] = {
    val prop = new Properties
    prop.put("mail.smtp.auth", true)
    prop.put("mail.smtp.starttls.enable", true)
    prop.put("mail.smtp.host", host)
    prop.put("mail.smtp.port", port)
    prop.put("mail.smtp.sll.trust", host)
    Resource.pure(prop)
  }

  val authResource: Resource[F, Authenticator] = Resource.pure(
    new Authenticator {
      override protected def getPasswordAuthentication: PasswordAuthentication =
        new PasswordAuthentication(user, pass)
    }
  )

  def createSession(prop: Properties, auth: Authenticator): Resource[F, Session] =
    Resource.pure(Session.getInstance(prop, auth))

  def createMessage(session: Session)
                   (from: String, to: String, subject: String, content: String): Resource[F, MimeMessage] =
    val message = new MimeMessage(session)
    message.setFrom(from)
    message.setRecipients(Message.RecipientType.TO, to)
    message.setSubject(subject)
    message.setContent(content, "text/html; charset=utf-8")
    Resource.pure(message)
}

object LiveEmails {
  def apply[F[_]: MonadCancelThrow](emailServiceConfig: EmailServiceConfig): F[LiveEmails[F]] =
    new LiveEmails[F](emailServiceConfig).pure[F]
}