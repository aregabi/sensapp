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

package net.modelbased.sensapp.service.oauth

import cc.spray._
import cc.spray.http._
import cc.spray.http.MediaTypes._
import cc.spray.directives._
import net.modelbased.sensapp.library.system.{Service => SensAppService}
import net.modelbased.sensapp.service.oauth.data.ElementJsonProtocol._
import net.modelbased.sensapp.service.oauth.data._
import org.bson.types.ObjectId

trait Service extends SensAppService {
  
  override implicit val partnerName = "oauth"
  
  val service = {
    pathPrefix("oauth") {
      pathPrefix("app" / PathElement) { appID =>
        val appInfos = applicationExists(appID)
        path("") {
          get {
            if(appInfos != null)
              redirect("authenticate")
            else
              completeWith("This application does not exist. You can close this window")
          } ~ cors("GET")
        } ~
        //User authentification: form + handling post values
        path("authenticate") {
          get {
            //Handle the error parameter
            parameter("error"?) { error =>

              //Check value of parameter error to display a printer-friendly description
              var errorDescr = "<hr/>"
              if(!error.isEmpty) {
                if(error.get == "credentials") errorDescr = "Login or password is wrong"
              }

              //If the application exists, then respond with a form
              if(appInfos != null) {
                
                val redirectURIcancel = appInfos(2)+"?error=connection"
                respondWithMediaType(`text/html`) {
                  completeWith {
                    "<html lang=\"en\">"+
                      "<header>"+
                        "<title>SensApp Oauth</title>"+
                      "</header>"+
                      "<body>"+
                        "<style>"+
                          "body{font-family:Arial;background-color:lightgray;}"+
                          "div{width:300px;margin:auto;padding:0;margin-top:200px;background-color:white;border:2px solid gray;box-shadow:0px 0px 100px gray;}"+
                          "h3{margin:10px;text-align:center;font-weight:normal;}"+
                          "form{margin:0;padding:0;}"+
                          "table{margin:auto;}"+
                          "p{margin:0;text-align:center;}"+
                        "</style>"+
                        "<div>"+
                          "<form method=\"post\" action=\"authenticate\">"+
                            "<h3>SensApp Authentication</h3>"+
                            "<table>"+
                              "<tr>"+
                                "<td><label for=\"login\">Login</label></td>"+
                                "<td><input type=\"text\" name=\"login\" id=\"login\"/></td>"+
                              "</tr>"+
                              "<tr>"+
                                "<td><label for=\"pwd\">Password</label></td>"+
                                "<td><input type=\"password\" name=\"pwd\" id=\"pwd\"/></td>"+
                              "</tr>"+
                            "</table>"+
                            "<p>"+
                              "<a href=\""+redirectURIcancel+"\">"+
                                "<input type=\"reset\" value=\"Cancel\" onclick=\"javascript:document.location.href='"+redirectURIcancel+"'\"/>"+
                              "</a>"+
                              "<input type=\"submit\" value=\"Connect\"/>"+
                            "</p>"+
                            "<p>"+
                              errorDescr+
                            "</p>"+
                          "</form>"+
                        "</div>"+
                      "</body>"+
                    "</html>"
                  }
                }
              }
              //If the application does not exist, then go with an error message
              else
                completeWith("This application does not exist. You can close this window.")
            }
          } ~
          post {
            //Retrieve the POST mandatory fields login and pwd and display the app's authorization
            formFields('login, 'pwd) { (login, pwd) =>
              //If application exists
              if(appInfos != null) {
                
                //Access level of the app
                val accessLevel = augmentString(appInfos(3)).toInt
                //If the user exists
                if(userExists(login, pwd)) {
                  
                  //Define redirection URI
                  val redirectURIok = appInfos(2)+"?success="+getTempCode(login, appID)
                  val redirectURIcancel = appInfos(2)+"?error=connection"
                  
                  //Define the rights and the non-rights of the application
                  var rights = ""
                  var nonrights = ""
                  accessLevel match {
                    case 0 => {
                        rights = "<li>Read</li>"
                        nonrights = "<li>Create/Modify/Delete</li>"
                    }
                    case 1 => {
                        rights = "<li>Read</li><li>Create/Modify/Delete</li>"
                        nonrights = "<li>None</li>"
                    }
                  }
                  respondWithMediaType(`text/html`) {
                    completeWith {
                      "<html lang=\"en\">"+
                        "<header>"+
                          "<title>SensApp Oauth</title>"+
                        "</header>"+
                        "<body>"+
                          "<style>"+
                            "body{font-family:Arial;background-color:lightgray;}"+
                            "div.authorizations{background-color:white;padding:10px;}"+
                          "</style>"+
                          "<div>"+
                            "<p>Welcome, "+login+"!</p>"+
                            "<h3>Do you want to authorize "+appInfos(0)+" to access your account?</h3>"+
                            "<div class=\"authorizations\">"+
                              "<p>"+appInfos(1)+"</p>"+
                              "<p>The app will be able to:</p>"+
                              "<ul>"+
                                rights+
                              "</ul>"+
                              "<p>The app will not be able to:</p>"+
                              "<ul>"+
                                nonrights+
                              "</ul>"+
                              "<p>"+
                                "<a href=\""+redirectURIcancel+"\">"+
                                  "<input type=\"button\" value=\"Cancel\" onclick=\"javascript:document.location.href='"+redirectURIcancel+"'\"/>"+
                                "</a>"+
                                "<a href=\""+redirectURIok+"\">"+
                                  "<input type=\"button\" value=\"Authorize\" onclick=\"javascript:document.location.href='"+redirectURIok+"'\"/>"+
                                "</a>"+
                              "</p>"+
                            "</div>"+
                          "</div>"+
                        "</body>"+
                      "</html>"
                    }
                  }
                }
                else
                  redirect("authenticate?error=credentials")
              }
              //If the application does not exist, then go with an error message
              else
                completeWith("This application does not exist. You can close this window.")
            }
          } ~ cors("GET", "POST")
        }
      } ~
      pathPrefix("token") {
        path("") {
          post {
            formFields('code, 'appID, 'appSecret, 'redirectURI) { (code, appID, appSecret, redirectURI) =>
              val oauthToken = getOAuthToken(code, appID, appSecret, redirectURI)
              if(oauthToken != null)
                completeWith(oauthToken)
              else {
                respondWithStatus(StatusCodes.Forbidden) {
                  completeWith("ERROR 403 Forbidden")
                }
              }
            }
          } ~ cors("POST")
        } ~
        path("check") {
          post {
            content(as[CheckOAuthToken]) { request => context =>
              context complete checkOAuthToken(request.token, request.accessLevel, request.secret)
            } ~
            content(as[CheckOAuthTokenApp]) { request => context =>
              context complete checkOAuthTokenApp(request.token, request.appID, request.appSecret)
            }
          } ~ cors("POST")
        }
      }
    }
  }
  
  private[this] val _users = new UserDescriptionRegistry()
  
  private[this] val _apps = new AppDescriptionRegistry()
  
  private[this] val _oauth_tempcode = new OAuthTempCodeRegistry()
  
  private[this] val _oauth_token = new OAuthTokenRegistry()
  
  
  //Return an array containing the name, description and redirectURI of the app if it exists, null otherwise
  private def applicationExists(id: String): Array[String] = {
    
    //If id is a valid ObjectID, then check if it exists in database
    if(ObjectId.isValid(id)) {
      //Make an ObjectID with id
      val objectID = new ObjectId(id)
      //If application exists, then retrieve all the information in database and return it in an array
      if(_apps exists ("_id", objectID)) {
        val app = (_apps pull ("_id", objectID)).get
        Array(app.name, app.description, app.redirectURI, app.accessLevel.toString)
      }
      else null
    }
    //If id is not a valid ObjectID, then return null
    else null
  }
  
  //Return true if user exists, false otherwise
  private def userExists(login: String, pwd: String): Boolean = {
    
    //Hash the password
    val hashPwd = sha256(pwd)
    
    //If the user login exists
    if(_users exists("login", login)) {
      //Retrieve user's data from database
      val user = (_users pull ("login", login)).get

      //If the user's password matches the pwd parameter, it's an authentication success
      user.pwd == hashPwd
    }
    //If the user login does not exist, return false
    else false
    
  }
  
  //Generate the temporary code the application will use to get the OAuth token
  private def getTempCode(login: String, appID: String): String = {
    
    //If the login exists in database
    if(_users exists ("login", login)) {
      
      //If the appID is a valid ObjectId
      if(ObjectId.isValid(appID)) {
        
        val appObjectID = new ObjectId(appID)
        //If the app exists
        if(_apps exists ("_id", appObjectID)) {
          
          //Generate temporary code
          val tempCode = generateToken("TEMPORARY_CODE")
          
          //Create an OAuth Token as defined in the data package
          val dbTempCode = new OAuthTempCode(tempCode, login, appID)
          
          //Put the dbToken in the database
          _oauth_tempcode push dbTempCode
          
          //Return temporary code
          tempCode
          
        }
        //If the app does not exist
        else null
      }
      //If the appID is not valid, return null
      else null
    }
    //If the user does not exist, return null
    else null
    
  }
  
  //Check if all the parameters are valid and, if so, returns the OAuth Token, registers it in database and delete the temporary code from the database
  private def getOAuthToken(tempCode: String, appID: String, appSecret: String, redirectURI: String): String = {
    
    //If the OAuth temporary code exists and the appID is a valid ObjectId
    if((_oauth_tempcode exists ("tempCode", tempCode)) && ObjectId.isValid(appID)) {
      
      //Retrieve values from the temporary code in the database
      val dbTempCode = (_oauth_tempcode pull ("tempCode", tempCode)).get
      
      //Make the appID a String an ObjectId
      val appObjectId = new ObjectId(appID)
      
      //If
      if((_apps exists ("_id", appObjectId)) && //the app exists and...
          dbTempCode.appID == appID) { //the app ID matches with the app ID in the temporary code database and...
        
        //Retrieve the app data from the database
        val appData = (_apps pull ("_id", appObjectId)).get
        
        //If
        if(appData.secret == appSecret && //the app secret is the same registered in database and...
           appData.redirectURI == redirectURI) { //the redirectURI is the same as registered in database
          
          //Create the OAuth Token
          val token = generateToken("OAUTH_TOKEN")
          
          //Define the OAuth Token to put it in database
          val oauthToken = new OAuthToken(token, dbTempCode.userLogin, appID, appData.accessLevel)
          
          //Delete the temporary code from database
          _oauth_tempcode drop dbTempCode
          
          //Push the token into database
          _oauth_token push oauthToken
          
          //Return the OAuth Token
          token
        }
        else null
      }
      else null
    }
    else null
    
  }
  
  //Check if OAuth token is valid given the token and the access level
  private def checkOAuthToken(token: String, accessLevel: Int, secret: String): String = {
    
    //If the secret is OAUTH_SERVICE_SECRET
    if(secret == "OAUTH_SERVICE_SECRET") {
      //If the token exists
      if(_oauth_token exists ("token", token)) {
        //Retrieve the data associated with the token
        val tokenData = (_oauth_token pull ("token", token)).get

        //If the access level of the token is greater than or equal than the required accessLevel, return true
        if(accessLevel <= tokenData.accessLevel) tokenData.userLogin
        else "false"
      }
      else "false"
    }
    else "false"
  }
  
  //Check if OAuth token is valid given the token, the app ID and the app secret
  private def checkOAuthTokenApp(token: String, appID: String, appSecret: String): String = {
    
    //If  the token exists
    if((_oauth_token exists ("token", token)) && ObjectId.isValid(appID)) {
      //Retrieve the data associated with the token
      val tokenData = (_oauth_token pull ("token", token)).get
      
      val appObjectID = new ObjectId(appID)
      //If the appID corresponds to the appID of the token
      if(tokenData.appID == appID) {
        
        //If the appID corresponds to an app
        if(_apps exists ("_id", appObjectID)) {
          
          val appData = (_apps pull ("_id", appObjectID)).get
          //If the app secret corresponds to the app secret in database
          if(appData.secret == appSecret) "true"
          else "false"
        }
        else "false"
      }
      else "false"
    }
    else "false"
  }
  
  //Hash String t with SHA256
  private def sha256(t: String): String = {
    
    //Import the Java library
    import java.security.MessageDigest
    
    val md = MessageDigest.getInstance("SHA-256")
    md.update(t.getBytes)
    val hashByte = md.digest
    val sb: StringBuffer = new StringBuffer
    for(i <- 0 until hashByte.length)
      sb.append(Integer.toString((hashByte(i) & 0xff) + 0x100, 16).substring(1));
    
    sb.toString
  }
  
  //Generate a token which length is different from one type to another
  private def generateToken(tokenType: String): String = {
    
    import java.security.SecureRandom
    import java.math.BigInteger
    
    //Default length
    var length = 128
    
    //Define length according to the token type
    if(tokenType == "TEMPORARY_CODE")   length = 128
    if(tokenType == "OAUTH_TOKEN")      length = 256
    
    //SecureRandom
    val rd = new SecureRandom
    
    //Return a String containing letters a-z and numbers 0-9
    new BigInteger(length, rd).toString(36)
  }
  
}
