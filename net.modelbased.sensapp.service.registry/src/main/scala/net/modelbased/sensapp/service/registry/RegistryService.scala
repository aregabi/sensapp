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
import cc.spray.json._
import cc.spray.json.DefaultJsonProtocol._
import cc.spray.directives._
import cc.spray.typeconversion.SprayJsonSupport
import net.modelbased.sensapp.library.system.{Service => SensAppService, URLHandler}
import net.modelbased.sensapp.library.senml.spec.{Standard => SenMLStd}
import net.modelbased.sensapp.service.registry.data._
import net.modelbased.sensapp.service.registry.data.ElementJsonProtocol._
import net.modelbased.sensapp.service.registry.data.Backend


trait RegistryService extends SensAppService {
  
  override implicit lazy val partnerName = "registry"
  
  override implicit lazy val partnersNames = List("database.raw", "oauth")
    
  val service = {
    path("registry" / "sensors") {
      get { 
        parameter("flatten" ? false) { flatten =>  context =>
          
          val auth = getAuthorizationCode(context)
          
          //Get sensors
          val descriptors =  _registry.retrieve(List()).par
          
          val userName = checkAuthorization(auth, 0)
          
          //If the authorization code is valid
          if(userName != null) {
            
            //Sensors list
            val sensors = descriptors.toList
            var sensorsList: List[SensorDescription] = List()
            //For each sensor in the sensors list
            for(sensor <- sensors) {
              //If a user name is in the sensor
              if(sensor.infos.tags.contains("user")) {
                sensor.infos.tags.get("user") match {
                  case Some(u) => {
                      if(u == userName) sensorsList ::= sensor
                  }
                }
              }
            }
            
            if (flatten) {
              //context complete descriptors
              context complete sensorsList.par.seq
              
            } else {
              val uris = sensorsList.par map { s => URLHandler.build("/registry/sensors/"+ s.id) }
              context complete uris.seq
            }
          }
          
          //If the authorization code is invalid, return only
          else context complete "The authorization code "+auth+" is no longer valid or does not exist!"
          
        } 
      } ~
      post {
        content(as[CreationRequest]) { request => context =>
          val auth = getAuthorizationCode(context)
          
          val userName = checkAuthorization(auth, 1)
          
          //If the authorization code is valid
          if(userName != null) {
            if (_registry exists ("id", request.id)){
              context fail (StatusCodes.Conflict, "A SensorDescription identified as ["+ request.id +"] already exists!")
            } else {
              // Create the database
              val backend = createDatabase(request.id, request.schema)
              // Store the descriptor
              _registry push (request.toDescription(backend))
              context complete URLHandler.build("/registry/sensors/" + request.id)
            }
          }
          
          //If the authorization code is invalid
          else context complete "The authorization code "+auth+" is no longer valid or does not exist!"
          
        }
      } ~ cors("GET", "POST")
    } ~ 
    path("registry" / "sensors" / SenMLStd.NAME_VALIDATOR.r ) { name =>
      get { context =>
        
        val auth = getAuthorizationCode(context)
        
        val userName = checkAuthorization(auth, 1)
        
        val tmp = (_registry pull ("id", name)).get
        
        //If the authorization code is valid
        if(userName != null) {
          if(tmp.infos.tags.contains("user")) {
            tmp.infos.tags.get("user") match {
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
        
        //If the authorization code is invalid
        else context complete SensorDescriptionLimited(tmp.backend)
      
      } ~
      delete { context =>
        
        val auth = getAuthorizationCode(context)
        
        val userName = checkAuthorization(auth, 1)
        
        //If the authorization code is valid
        if(userName != null) {
          ifExists(context, name, {
            val sensor = _registry pull ("id", name)
            if(sensor.get.infos.tags.contains("user")) {
              sensor.get.infos.tags.get("user") match {
                case Some(u) => {
                  if(u == userName) {
                    delDatabase(name)
                    propagateDeletionToComposite(URLHandler.build("/registry/sensors/" + sensor))
                    _registry drop sensor.get
                    context complete "true"
                  }
                  else context complete "Unauthorized"
                }
                case _ => context complete "Unauthorized"
              }
            }
            context complete "Unauthorized"
          })
        }
        
        //If the authorization code is invalid
        else context complete "The authorization code "+auth+" is no longer valid or does not exist!"
        
      } ~
      put {
        content(as[SensorInformation]) { info => context =>
          
          val auth = getAuthorizationCode(context)
          
          val userName = checkAuthorization(auth, 1)
          
          //If the authorization code is valid
          if(userName != null) {
            ifExists(context, name, {
              val sensor = (_registry pull ("id", name)).get
              
              //If the sensor doesn't contain a user
              if(!sensor.infos.tags.contains("user")) {
                val safe = SensorInformation(info.tags.filter( t => t._1 != "" ).filter( t => t._1 != "user"), info.updateTime, info.localization)
                sensor.infos = safe
                sensor.infos.tags += (("user", userName))
                _registry push sensor
                context complete sensor
              }
              
              //If the sensor contains a user
              else {
                sensor.infos.tags.get("user") match {
                  case Some(u) => {
                    if(u == userName) {
                      val safe = SensorInformation(info.tags.filter( t => t._1 != "" ).filter( t => t._1 != "user"), info.updateTime, info.localization)
                      sensor.infos = safe
                      sensor.infos.tags += (("user", userName))
                      _registry push sensor
                      context complete sensor
                    }
                    else context complete "Unauthorized"
                  }
                  case _ => context complete "Unauthorized"
                }
              }
            })
          }
          
          //If the authorization code is invalid
          else context complete "The authorization code "+auth+" is no longer valid or does not exist!"
          
        } ~
        content(as[DescriptionUpdate]) { request => context =>
          
          val auth = getAuthorizationCode(context)
          
          val userName = checkAuthorization(auth, 1)
          
          //If the authorization code is valid
          if(checkAuthorization(auth, 1) != null) {
            ifExists(context, name, {
              val sensor = (_registry pull ("id", name)).get
              if(sensor.infos.tags.contains("user")) {
                sensor.infos.tags.get("user") match {
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
  
  private[this] def createDatabase(id: String, schema: Schema): Backend = {
    val helper = BackendHelper(schema)
    val urls = helper.createDatabase(id, schema, partners)
    Backend(schema.backend, urls._1, urls._2) 
  }
  
  private[this] def delDatabase(id: String) = {
    val backend = (_registry pull ("id", id)).get.backend
    val helper = BackendHelper(backend)
    helper.deleteDatabase(backend, partners)
  }
  
  private[this] def propagateDeletionToComposite(url: String) {
    // TODO: implement me
    val _compositeRegistry = new CompositeSensorDescriptionRegistry()
    _compositeRegistry.pull(("sensors", ""))
   
  }
  
  private[this] val _registry = new SensorDescriptionRegistry()
  
  private def ifExists(context: RequestContext, id: String, lambda: => Unit) = {
    if (_registry exists ("id", id))
      lambda
    else
      context fail(StatusCodes.NotFound, "Unknown sensor [" + id + "]") 
  }
  
}