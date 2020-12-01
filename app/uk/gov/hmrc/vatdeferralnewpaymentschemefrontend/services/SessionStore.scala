/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.services

import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.{Json, Reads, Writes}
import play.modules.reactivemongo.ReactiveMongoComponent
import uk.gov.hmrc.cache.repository.CacheMongoRepository
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[SessionStoreImpl])
trait SessionStore {
  def get[T](id: String, key: String)(implicit reads: Reads[T]): Future[Option[T]]
  def store[T](sessionId: String, key: String, value: T)(implicit writes: Writes[T]): Unit
}

@Singleton
class SessionStoreImpl @Inject()(mongo: ReactiveMongoComponent, serviceConfig: ServicesConfig)(implicit ec: ExecutionContext) extends SessionStore {

  private val expireAfterSeconds = serviceConfig.getDuration("mongodb.session.expireAfter").toSeconds

  private lazy val cacheRepository = new CacheMongoRepository("sessions", expireAfterSeconds)(mongo.mongoConnector.db, ec)

  def get[T](id: String, key: String)(implicit reads: Reads[T]): Future[Option[T]] = {

    cacheRepository.findById(id) map {
      case Some(cache) => cache.data flatMap {
        json =>
          Logger.debug(s"[SessionStore][get] $cache")
          Some((json \ key))
            .filter(_.validate[T].isSuccess)
            .map(_.as[T])
      }
      case None => None
    }
  }

  def store[T](sessionId: String, key: String, value: T)(implicit writes: Writes[T]): Unit = {
    cacheRepository.createOrUpdate(sessionId, key, Json.toJson(value))
  }
}
