package com.social.core

import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import com.social.fixtures.PostFixture
import cats.effect.*
import doobie.implicits.*
import doobie.postgres.implicits.*


class PostsSpec
  extends AsyncFreeSpec
  with AsyncIOSpec
  with Matchers
  with PostFixture
  with DoobieSpec {
  override val initScript: String = "sql/Posts.sql"

  "Post algebra" - {
    "should return no post if the id does not exist" in {
      transactor.use { xa =>
        val program = for {
          posts <- LivePosts[IO](xa)
          retrieved <- posts.find(NotFoundPostUuid)
        } yield retrieved

        program.asserting(_ shouldBe None)
      }
    }

    "should return by id" in {
      transactor.use { xa =>
        val program = for {
          posts <- LivePosts[IO](xa)
          retrieved <- posts.find(AwesomePostUuid)
        } yield retrieved

        program.asserting(_ shouldBe Some(AwesomePost))
      }
    }

    "should return all posts" in {
      transactor.use { xa =>
        val program = for {
          posts <- LivePosts[IO](xa)
          retrieved <- posts.all()
        } yield retrieved

        program.asserting(_ shouldBe List(AwesomePost))
      }
    }

    "should create a new post" in {
      transactor.use { xa =>
        val program = for {
          posts <- LivePosts[IO](xa)
          retrieved <- posts.create("name@domain.com ",NewPostInfo)
          jobOption <- posts.find(retrieved)
        } yield jobOption

        program.asserting(_.map(_.postInfo) shouldBe Some(NewPostInfo))
      }
    }

    "should return an updated post if it exists" in {
      transactor.use { xa =>
        val program = for {
          posts <- LivePosts[IO](xa)
          retrieved <- posts.update(AwesomePostUuid, UpdatedAwesomePost.postInfo)
        } yield retrieved

        program.asserting(_ shouldBe Some(UpdatedAwesomePost))
      }
    }

    "should return none if attempt update and uuid not found" in {
      transactor.use { xa =>
        val program = for {
          posts <- LivePosts[IO](xa)
          retrieved <- posts.update(NotFoundPostUuid, UpdatedAwesomePost.postInfo)
        } yield retrieved

        program.asserting(_ shouldBe None)
      }
    }

    "should delete an existing Post" in {
      transactor.use { xa =>
        val program = for {
          posts <- LivePosts[IO](xa)
          postsDeleted <- posts.delete(AwesomePostUuid)
          postsCount <- sql"SELECT COUNT(*) FROM posts WHERE id = ${AwesomePostUuid}"
            .query[Int]
            .unique.
            transact(xa)
        } yield (postsDeleted, postsCount)

        program.asserting { case (postsDeleted, postsCount) =>
          postsDeleted shouldBe 1
          postsCount shouldBe 0
        }
      }
    }

    "should return 0 when deleting an id that is not found" in {
      transactor.use { xa =>
        val program = for {
          posts <- LivePosts[IO](xa)
          postsDeleted <- posts.delete(NotFoundPostUuid)
          postsCount <- sql"SELECT COUNT(*) FROM posts WHERE id = ${AwesomePostUuid}"
            .query[Int]
            .unique.
            transact(xa)
        } yield (postsDeleted, postsCount)

        program.asserting { case (postsDeleted, postsCount) =>
          postsDeleted shouldBe 0
          postsCount shouldBe 1
        }
      }
    }
  }
}
