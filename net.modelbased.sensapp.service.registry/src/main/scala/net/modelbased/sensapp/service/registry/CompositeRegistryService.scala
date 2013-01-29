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

import cc.spray._
import cc.spray.http._
import net.modelbased.sensapp.library.system.{Service => SensAppService, URLHandler}
import net.modelbased.sensapp.library.senml.spec.{Standard => SenMLStd}
import net.modelbased.sensapp.service.registry.data._
import net.modelbased.sensapp.service.registry.data.ElementJsonProtocol._


trait CompositeRegistryService extends SensAppService {
  
  override implicit lazy val partnerName = "registry.composite"
  override implicit lazy val partnersNames = List("oauth")
  
  val service = {
    path("registry" / "composite" / "sensors") {
      get {
        parameters("flatten" ? false) { flatten => context =>
          
          val auth = getAuthorizationCode(context)
          
          //Sensors list
          val descriptors =  _registry.retrieve(List()).par
          
          val userName = checkAuthorization(auth, 0)
          
          //If the authorization code is valid
          if(userName != null) {
            
            //Sensors list
            val sensors = descriptors.toList
            var sensorsList: List[CompositeSensorDescription] = List()
            
            //For each sensor in the sensors list
            for(sensor <- sensors) {
              //If a user name is in the sensor
              sensor.tags match {
                case Some(m) => {
                  if(m.contains("user")) {
                    m.get("user") match {
                      case Some(u) => {
                        if(u == userName) sensorsList ::= sensor
                      }
                    }
                  }
                }
              }
            }
            
            if (flatten) {
              context complete sensorsList.par.seq
            } else {
              val uris = sensorsList.par map { s => URLHandler.build("/registry/composite/sensors/"+ s.id) }
              context complete uris.seq
            }
          }
          
          //If the authorization code is invalid
          else context complete "The authorization code "+auth+" is no longer valid or does not exist!"
          
        } 
      } ~ 
      post {
        content(as[CompositeSensorDescription]) { request => context =>
          
          val auth = getAuthorizationCode(context)
        
          val userName = checkAuthorization(auth, 0)
          
          //If the authorization code is valid
          if(userName != null) {
            if (_registry exists ("id", request.id)){
              context fail (StatusCodes.Conflict, "A CompositeSensorDescription identified as ["+ request.id +"] already exists!")
            } else {
              var tags = request.tags.get
              tags += "user" -> userName
              request.tags = Some(tags)
              _registry push (request)
              context complete URLHandler.build("/registry/composite/sensors/"+ request.id)
            }
          }
          //If the authorization code is invalid
          else context complete "The authorization code "+auth+" is no longer valid or does not exist!"
        }
        
      } ~ cors("GET", "POST")
    } ~ 
    path("registry" / "composite" / "sensors" / SenMLStd.NAME_VALIDATOR.r ) { name =>
      get { context =>
        
        val auth = getAuthorizationCode(context)
        
        val userName = checkAuthorization(auth, 0)
        
        val tmp = (_registry pull ("id", name)).get
        
        //If the authorization code is valid
        if(userName != null) {
          tmp.tags match {
            case Some(m) => {
              if(m.contains("user")) {
                m.get("user") match {
                  case Some(u) => {
                    if(u == userName)
                      ifExists(context, name, {context complete tmp})
                    else
                      context complete "Unauthorized"
                  }
                  case _ => context complete "Unauthorized"
                }
              }
              else context complete "Unauthorized"
            }
            case _ => context complete "Unauthorized"
          }
        }
        
        //If the authorization code is invalid
        else context complete "The authorization code "+auth+" is no longer valid or does not exist!"
        
      } ~
      delete { context =>
        
        val auth = getAuthorizationCode(context)
        
        val userName = checkAuthorization(auth, 0)
        
        //If the authorization code is valid
        if(userName != null) {
          ifExists(context, name, {
            val tmp = (_registry pull ("id", name)).get
            tmp.tags match {
              case Some(m) => {
                if(m.contains("user")) {
                  m.get("user") match {
                    case Some(u) => {
                      if(u == userName) {
                        ifExists(context, name, {
                          _registry drop tmp
                          context complete "true"
                        })
                      }
                      else context complete "Unauthorized"
                    }
                    case _ => context complete "Unauthorized"
                  }
                }
                else context complete "Unauthorized"
              }
              case _ => context complete "Unauthorized"
            }
          })
        }
        
        //If the authorization code is invalid
        else context complete "The authorization code "+auth+" is no longer valid or does not exist!"
      } ~
      put {
        content(as[SensorList]) { sensorList => context =>
          val auth = getAuthorizationCode(context)
          
          val userName = checkAuthorization(auth, 0)

          //If the authorization code is valid
          if(userName != null) {

            ifExists(context, name, {
              val sensor = (_registry pull ("id", name)).get
              sensor.sensors = sensorList.sensors
              sensor.tags match {
                case Some(m) => {
                  if(m.contains("user")) {
                    m.get("user") match {
                      case Some(u) => {
                        if(u == userName) {
                          _registry push sensor
                          context complete sensor
                        }
                        else context complete "Unauthorized"
                      }
                      case _ => context complete "Unauthorized"
                    }
                  }
                  else context complete "Unauthorized"
                }
                case _ => context complete "Unauthorized"
              }
            })
          }

          //If the authorization code is invalid
          else context complete "The authorization code "+auth+" is no longer valid or does not exist!"

        } ~
        content(as[SensorTags]) { tags => context =>
          val auth = getAuthorizationCode(context)

          val userName = checkAuthorization(auth, 0)

          //If the authorization code is valid
          if(userName != null) {

            ifExists(context, name, {
              val sensor = (_registry pull ("id", name)).get
              sensor.tags match {
                case Some(m) => {
                  if(m.contains("user")) {
                    m.get("user") match {
                      case Some(u) => {
                        if(u == userName) {
                          //Define tagsAddUser in order to add the user tag
                          var tagsAddUser = tags.tags.filter( t => t._1 != "" ).filter( t => t._1 != "user" )
                          //Add the user tag
                          tagsAddUser += "user" -> userName
                          sensor.tags = Some(tagsAddUser)
                          _registry push sensor
                          context complete sensor
                        }
                        else context complete "Unauthorized"
                      }
                      case _ => context complete "Unauthorized"
                    }
                  }
                  else context complete "Unauthorized"
                }
                case _ => context complete "Unauthorized"
              }
            })
          }

          //If the authorization code is invalid
          else context complete "The authorization code "+auth+" is no longer valid or does not exist!"

        } ~
        content(as[DescriptionUpdate]) { request => context =>
          val auth = getAuthorizationCode(context)

          val userName = checkAuthorization(auth, 0)

          //If the authorization code is valid
          if(userName != null) {
            ifExists(context, name, {
              val sensor = (_registry pull ("id", name)).get
              sensor.tags match {
                case Some(m) => {
                  if(m.contains("user")) {
                    m.get("user") match {
                      case Some(u) => {
                        if(u == userName) {
                          sensor.description = request.description
                          _registry push sensor
                          context complete sensor
                        }
                        else context complete "Unauthorized"
                      }
                      case _ => context complete "Unauthorized"
                    }
                  }
                  else context complete "Unauthorized"
                }
                case _ => context complete "Unauthorized"
              }
            })
          }

          //If the authorization code is invalid
          else context complete "The authorization code "+auth+" is no longer valid or does not exist!"

        }
        
      } ~ cors("GET", "DELETE", "PUT")
    }
  }
  
  //Giving a code and an accessLevel, check if the resource is accessible
  private[this] def checkAuthorization(code: String, accessLevel: Int): String = {
    checkOAuthToken(code, accessLevel)
  }
  
  //Check OAuth token in oauth.token database
  private[this] def checkOAuthToken(token: String, accessLevel: Int): String = {
    OAuth.checkToken(token, accessLevel, "OAUTH_SERVICE_SECRET", partners)
  }
  
  private[this] def getAuthorizationCode(context: RequestContext): String = {
    var auth = ""
    for(header <- context.request.headers) {
      if(header.name equals "X-Authorization")
        auth = header.value
    }
    auth
  }
  
  private[this] val _registry = new CompositeSensorDescriptionRegistry()
  
  private def ifExists(context: RequestContext, id: String, lambda: => Unit) = {
    if (_registry exists ("id", id))
      lambda
    else
      context fail(StatusCodes.NotFound, "Unknown sensor [" + id + "]") 
  } 
  
}