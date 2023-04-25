package com.social.core

import cats.*
import cats.effect.kernel.{MonadCancelThrow, Resource}
import cats.implicits.*
import com.social.domain.Post.{Post, PostInfo}
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.util.*

import java.util.UUID

trait Posts[F[_]] {
  def create(ownerEmail: String, postInfo: PostInfo):  F[UUID]
  def all(): F[List[Post]]
  def find(id: UUID): F[Option[Post]]
  def update(id: UUID, postInfo: PostInfo): F[Option[Post]]
  def hide(id: UUID): F[Int]
  def delete(id: UUID): F[Int]
}

class LivePosts[F[_]: MonadCancelThrow] private(xa: Transactor[F]) extends Posts[F] {
  override def create(ownerEmail: String, postInfo: PostInfo): F[UUID] =
    PostFragments.create(ownerEmail, postInfo).update.withUniqueGeneratedKeys[UUID]("id").transact(xa)

  override def all(): F[List[Post]] =
    PostFragments.all.query[Post].to[List].transact(xa)

  override def find(id: UUID): F[Option[Post]] =
    PostFragments.find(id).query[Post].option.transact(xa)

  override def update(id: UUID, postInfo: PostInfo): F[Option[Post]] =
    PostFragments.update(id, postInfo).update.run.transact(xa).flatMap { _ => find(id) }

  override def hide(id: UUID): F[Int] =
    PostFragments.hide(id).update.run.transact(xa)

  override def delete(id: UUID): F[Int] =
    PostFragments.delete(id).update.run.transact(xa)
}

/*
id: UUID,
date: Long,
email: String,
text: String,
likes: Int,
disLikes: Int,
tags: Option[List[String]],
image: Option[String],
hidden: Boolean
*/

object LivePosts{
  given postRead: Read[Post] = Read[(
    UUID, //id
    Long, //date
    String, //email
    String, //text
    Int, //likes
    Int, //disLikes
    Option[List[String]], //tags
    Option[String], //image
    Boolean //hidden
    )].map {
    case (
      id: UUID,
      date: Long,
      email: String,
      text: String,
      likes: Int,
      disLikes: Int,
      tags: Option[List[String]] @unchecked,
      image: Option[String] @unchecked,
      hidden: Boolean
    ) => Post(
      id = id,
      date = date,
      email = email,
      postInfo = PostInfo(
        text = text,
        likes = likes,
        disLikes = disLikes,
        tags = tags,
        image = image
      ),
      hidden = hidden
    )
  }

  //creation of live jobs is could be an effect full operation, wrap it in F
  def apply[F[_]: MonadCancelThrow](xa: Transactor[F]): F[LivePosts[F]] = new LivePosts[F](xa).pure[F]
  //another way to write the apply method
  //def apply[F[_]: Applicative]: Resource[F, LivePosts[F]] = ???
}

object PostFragments {
  val all: Fragment =
    sql"""
       SELECT
            id,
            date,
            email,
            text,
            likes,
            disLikes,
            tags,
            image,
            hidden
       FROM posts
       """

  def find(id: UUID): Fragment =
    sql"""
        SELECT
            id,
            date,
            email,
            text,
            likes,
            disLikes,
            tags,
            image,
            hidden
        FROM posts
        WHERE id == $id
       """

  def create(ownerEmail: String, postInfo: PostInfo): Fragment =

    sql"""
       INSERT INTO posts(
                        date,
                        email,
                        text,
                        likes,
                        disLikes,
                        tags,
                        image,
                        hidden
       ) VALUES (
                 ${System.currentTimeMillis()},
                 $ownerEmail,
                 ${postInfo.text},
                 ${postInfo.likes},
                 ${postInfo.disLikes},
                 ${postInfo.tags},
                 ${postInfo.image},
                 ${false}
       )
       """

  def update(id: UUID, postInfo: PostInfo): Fragment =
    sql"""
            UPDATE posts
            SET
                text = ${postInfo.text},
                likes = ${postInfo.likes},
                disLikes = ${postInfo.disLikes},
                tags = ${postInfo.tags},
                image = ${postInfo.image}
            WHERE IS = $id
         """

  def delete(id: UUID): Fragment =
    sql"""
            DELETE FROM posts
            WHERE id = $id
         """

  def hide(id: UUID): Fragment =
    sql"""
            UPDATE posts
            SET
                hidden = ${true}
            WHERE id = $id
         """
}
