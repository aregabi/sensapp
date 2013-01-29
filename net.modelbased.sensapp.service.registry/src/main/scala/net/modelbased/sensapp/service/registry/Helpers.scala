/**
 * This file is part of SensApp [ http://sensapp.modelbased.net ]
 *
 * Copyright (C) 2012-  SINTEF ICT
 * Contact: Sebastien Mosser <sebastien.mosser@sintef.no>
 *
 * Module: net.modelbased.sensapp.service.registry
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
package net.modelbased.sensapp.service.registry

import akka.dispatch.Await
import akka.util.duration._
import cc.spray.client._
import cc.spray.json._
import cc.spray.typeconversion.DefaultUnmarshallers._
import cc.spray.typeconversion.SprayJsonSupport
import net.modelbased.sensapp.library.system._
import java.io._
import java.net._
import net.modelbased.sensapp.library.senml._

object OAuth extends HttpSpraySupport with SprayJsonSupport with DefaultJsonProtocol with io.Marshaller {
  
  def httpClientName = "oauth-helper"
  
  case class OAuthTokenCheckRequest(val token: String, val accessLevel: Int, val secret: String)
  case class OAuthTokenCheckResult(val valid: Boolean)
  implicit val oauthTokenCheckRequest = jsonFormat(OAuthTokenCheckRequest, "token", "accessLevel", "secret")
  implicit val oauthTokenCheckResult = jsonFormat(OAuthTokenCheckResult, "valid")
  
  //Check if the token exists in database oauth and if it is valid for accessLevel
  def checkToken(token: String, accessLevel: Int, secret: String, partners: PartnerHandler): String =  {
    //Get OAuth partner location
    val partner = partners("oauth").get
    
    val conduit = new HttpConduit(httpClient, partner._1, partner._2) {
      val pipeline = simpleRequest[OAuthTokenCheckRequest] ~> sendReceive ~> unmarshal[String]
    }
    
    val response = conduit.pipeline(Post("/sensapp/oauth/token/check", OAuthTokenCheckRequest(token, accessLevel, secret)))
    val valid = Await.result(response, 5 seconds)
    conduit.close
    
    valid match {
      case "false" => null
      case _ => valid
    }
  }
  
}