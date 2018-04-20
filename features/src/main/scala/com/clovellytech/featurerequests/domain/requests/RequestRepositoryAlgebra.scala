package com.clovellytech.featurerequests.domain
package requests

import java.time.Instant

import com.clovellytech.featurerequests.db.domain._

trait RequestRepositoryAlgebra[F[_]]{
  def create(r : Feature) : F[Unit]

  def show : F[List[(FeatureId, Feature, Instant, Long, Long)]]
}
