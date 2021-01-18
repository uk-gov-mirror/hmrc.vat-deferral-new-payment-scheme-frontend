/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.services

import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.{Json, Reads, Writes}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.commands.LastError
import uk.gov.hmrc.cache.repository.CacheMongoRepository
import uk.gov.hmrc.mongo.DatabaseUpdate
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[SessionStoreImpl])
trait SessionStore {
  def get[T](id: String, key: String)(implicit reads: Reads[T]): Future[Option[T]]
  def store[T](sessionId: String, key: String, value: T)(implicit writes: Writes[T], reads: Reads[T]): Future[T]
  def drop(sessionId: String): Unit
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

  def store[T](sessionId: String, key: String, value: T)(implicit writes: Writes[T], reads: Reads[T]): Future[T] = {
    cacheRepository.createOrUpdate(sessionId, key, Json.toJson(value)).map {
      case DatabaseUpdate(e,r) =>
        r.savedValue.data.map (x => (x \ key).as[T])
          .fold(throw new IllegalStateException(s"Problem getting state from cache: ${e.message}"))(identity)
    }
  }

  def drop(sessionId: String): Unit = cacheRepository.removeById(sessionId)
}
