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

/**
 * Description of a SensApp user
 * @param login the login of the user
 * @param pwd the password of the user SHA256-hashed
 */
case class UserDescription(val login: String,
                           val pwd: String)

/**
 * Description of a SensApp external app
 * @param name the login of the app
 * @param description the description of the app
 * @param secret the secret of the application
 * @param redirectURI the URI users will be redirected to when they authorize/don't authorize the app
 * @param accessLevel the access level needed by this application
 */
case class AppDescription(val name: String,
                          val description: String,
                          val secret: String,
                          val redirectURI: String,
                          val accessLevel: Int)

/**
 * OAuth temporary codes to be exchanged for the OAuth Token
 * @param token if generated is true, then token is the oauth token, otherwise it is the temporary code
 * @param userID the id of the user concerned by this token
 * @param appID the id of the app concerned by this token
 */
case class OAuthTempCode(val tempCode: String,
                         val userLogin: String,
                         val appID: String)

/**
 * OAuth temporary codes to be exchanged for the OAuth Token
 * @param token token is the oauth token
 * @param userID the id of the user concerned by this token
 * @param appID the id of the app concerned by this token
 * @param accessLevel the access level of this token
 */
case class OAuthToken(val token: String,
                      val userLogin: String,
                      val appID: String,
                      val accessLevel: Int)

/**
 * Check OAuth token to check if the token is valid for the level access accessLevel
 * @param token is the oauth token
 * @param accessLevel the access level required
 */
case class CheckOAuthToken(val token: String,
                           val accessLevel: Int,
                           val secret: String)

/**
 * Check OAuth token given the token, the appID and the appSecret
 * @param token the token to check
 * @param appID the app ID
 * @param appSecret the app secret
 */
case class CheckOAuthTokenApp(val token: String,
                              val appID: String,
                              val appSecret: String)

/**
 *  Json protocols to support serialization through spray-json
 */
object ElementJsonProtocol extends DefaultJsonProtocol {
  implicit val userDescription = jsonFormat(UserDescription, "login", "pwd")
  implicit val appDescription = jsonFormat(AppDescription, "name", "description", "secret", "redirectURI", "accessLevel")
  implicit val oauthTempCode = jsonFormat(OAuthTempCode, "tempCode", "userLogin", "appID")
  implicit val oauthToken = jsonFormat(OAuthToken, "token", "userLogin", "appID", "accessLevel")
  implicit val checkOAuthToken = jsonFormat(CheckOAuthToken, "token", "accessLevel", "secret")
  implicit val checkOAuthTokenApp = jsonFormat(CheckOAuthTokenApp, "token", "appID", "appSecret")
}