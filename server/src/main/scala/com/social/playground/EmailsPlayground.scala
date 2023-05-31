package com.social.playground

import cats.effect.{IO, IOApp}
import com.social.config.EmailServiceConfig
import com.social.core.LiveEmails

import java.util.Properties
import javax.mail.internet.MimeMessage
import javax.mail.{Authenticator, Message, PasswordAuthentication, Session, Transport}

object EmailsPlayground {
  def main(args: Array[String]): Unit = {
    //configs
    // "SMTP","Brent Cronin","brent56@ethereal.email","fAR4dCQxcUrjNbjq9W","smtp.ethereal.email",587,"STARTTLS"
    val host = "smtp.ethereal.email"
    val port = 587
    val user = "brent56@ethereal.email"
    val pass = "fAR4dCQxcUrjNbjq9W"
    val token = "'RANDOM PASSWORD RESET TOKEN'"
    val frontEndUrl = "gohere.com"

    //properties file
    val prop = new Properties
    prop.put("mail.smtp.auth", true)
    prop.put("mail.smtp.starttls.enable", true)
    prop.put("mail.smtp.host", host)
    prop.put("mail.smtp.port", port)
    prop.put("mail.smtp.sll.trust", host)

    //authentication
    val auth = new Authenticator {
      override protected def getPasswordAuthentication: PasswordAuthentication =
        new PasswordAuthentication(user, pass)
    }

    //build a session
    val session = Session.getInstance(prop, auth)

    //email itself
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

    //message = MIME message
    val message = new MimeMessage(session)
    message.setFrom("NoReply@social.com")
    message.setRecipients(Message.RecipientType.TO, "person@domain.com")
    message.setSubject(subject)
    message.setContent(content, "text/html; charset=utf-8")

    //send
    Transport.send(message)
  }
}

object EmailsEffectPlayground extends IOApp.Simple {
  override def run: IO[Unit] = for {
    emails <- LiveEmails[IO](
      EmailServiceConfig(
     host = "smtp.ethereal.email",
     port = 587,
     user = "brent56@ethereal.email",
     pass = "fAR4dCQxcUrjNbjq9W",
     frontEndUrl = "gohere.com"
      )
    )

    _ <- emails.sendPasswordRecoveryEmail("person@domain.com", "abcd1234")
  } yield ()
}
