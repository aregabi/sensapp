/**
 * This file is part of SensApp [ http://sensapp.modelbased.net ]
 *
 * Copyright (C) 2011-  SINTEF ICT
 * Contact: Sebastien Mosser <sebastien.mosser@sintef.no>
 *
 * Module: net.modelbased.sensapp.restful
 *
 * SensApp is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * SensApp is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General
 * Public License along with SensApp. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package net.modelbased.sensapp.restful

import akka.actor._
import akka.http._
import akka.config.Supervision._

abstract class AbstractBoot[T <: Container](implicit val m: scala.reflect.Manifest[T]) {
  val factory = SupervisorFactory(SupervisorConfig(OneForOneStrategy(List(classOf[Exception]), 3, 100),
                                                   Supervise(Actor.actorOf[RootEndpoint], Permanent) ::
                                                   Supervise(Actor.actorOf[T], Permanent) :: Nil))
  factory.newInstance.start
}