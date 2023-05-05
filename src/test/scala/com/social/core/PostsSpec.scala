package com.social.core

import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import com.social.fixtures.PostFixture
import cats.effect.*
import com.social.domain.pagination.Pagination
import com.social.domain.post.PostFilter
import doobie.implicits.*
import doobie.postgres.implicits.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

class PostsSpec
  extends AsyncFreeSpec
  with AsyncIOSpec
  with Matchers
  with PostFixture
  with DoobieSpec {
  override val initScript: String = "sql/Posts.sql"
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

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
          postsCount <- sql"SELECT COUNT(*) FROM posts WHERE id = $AwesomePostUuid"
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
          postsCount <- sql"SELECT COUNT(*) FROM posts WHERE id = $AwesomePostUuid"
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

    "should filter on hidden == false" in {
      transactor.use { xa =>
        val program = for {
          posts <- LivePosts[IO](xa)
          filteredPosts <- posts.all(PostFilter(), Pagination.default)
        } yield filteredPosts

        program.asserting(_ shouldBe List(AwesomePost))
      }
    }

    "should filter on hidden == ture" in {
      transactor.use { xa =>
        val program = for {
          posts <- LivePosts[IO](xa)
          filteredPosts <- posts.all(PostFilter(hidden = true), Pagination.default)
        } yield filteredPosts

        program.asserting(_ shouldBe List())
      }
    }

    "should filter on a tag and find" in {
      transactor.use { xa =>
        val program = for {
          posts <- LivePosts[IO](xa)
          filteredPosts <- posts.all(PostFilter(tags = List("tag2")), Pagination.default)
        } yield filteredPosts

        program.asserting(_ shouldBe List(AwesomePost))
      }
    }

    "should filter on multiple tags and find" in {
      transactor.use { xa =>
        val program = for {
          posts <- LivePosts[IO](xa)
          filteredPosts <- posts.all(PostFilter(tags = List("tag2", "tag1")), Pagination.default)
        } yield filteredPosts

        program.asserting(_ shouldBe List(AwesomePost))
      }
    }

    "should filter on a tag and find none" in {
      transactor.use { xa =>
        val program = for {
          posts <- LivePosts[IO](xa)
          filteredPosts <- posts.all(PostFilter(tags = List("tag3")), Pagination.default)
        } yield filteredPosts

        program.asserting(_ shouldBe List())
      }
    }

    "should filter on multiple tags and find none" in {
      transactor.use { xa =>
        val program = for {
          posts <- LivePosts[IO](xa)
          filteredPosts <- posts.all(PostFilter(tags = List("tag2", "tag3")), Pagination.default)
        } yield filteredPosts

        program.asserting(_ shouldBe List())
      }
    }
  }
}
