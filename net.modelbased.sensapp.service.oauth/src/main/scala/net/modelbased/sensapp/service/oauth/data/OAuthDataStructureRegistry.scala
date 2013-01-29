/**
 * This file is part of SensApp [ http://sensapp.modelbased.net ]
 *
 * Copyright (C) 2012-  SINTEF ICT
 * Contact: Sebastien Mosser <sebastien.mosser@sintef.no>
 *
 * Module: net.modelbased.sensapp.service.oauth
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
package net.modelbased.sensapp.service.oauth.data

import cc.spray.json._
import net.modelbased.sensapp.library.datastore._
import ElementJsonProtocol._

class UserDescriptionRegistry extends DataStore[UserDescription] {
  
  override val databaseName = "sensapp_db"
  override val collectionName = "users" 
  override val key = "login"
  
  override def getIdentifier(e: UserDescription) = e.login
  
  override def deserialize(json: String): UserDescription = { json.asJson.convertTo[UserDescription] }
 
  override def serialize(e: UserDescription): String = { e.toJson.toString }
  
}

class AppDescriptionRegistry extends DataStore[AppDescription] {
  
  override val databaseName = "sensapp_db"
  override val collectionName = "apps" 
  override val key = "name"
  
  override def getIdentifier(e: AppDescription) = e.name
  
  override def deserialize(json: String): AppDescription = { json.asJson.convertTo[AppDescription] }
 
  override def serialize(e: AppDescription): String = { e.toJson.toString }
  
}

class OAuthTempCodeRegistry extends DataStore[OAuthTempCode] {
  
  override val databaseName = "sensapp_db"
  override val collectionName = "oauth.tempcode" 
  override val key = "tempCode"
  
  override def getIdentifier(e: OAuthTempCode) = e.tempCode
  
  override def deserialize(json: String): OAuthTempCode = { json.asJson.convertTo[OAuthTempCode] }
 
  override def serialize(e: OAuthTempCode): String = { e.toJson.toString }
  
}

class OAuthTokenRegistry extends DataStore[OAuthToken] {
  
  override val databaseName = "sensapp_db"
  override val collectionName = "oauth.token" 
  override val key = "token"
  
  override def getIdentifier(e: OAuthToken) = e.token
  
  override def deserialize(json: String): OAuthToken = { json.asJson.convertTo[OAuthToken] }
 
  override def serialize(e: OAuthToken): String = { e.toJson.toString }
  
}