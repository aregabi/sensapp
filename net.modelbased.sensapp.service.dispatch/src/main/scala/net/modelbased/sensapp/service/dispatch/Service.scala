/**
 * This file is part of SensApp [ http://sensapp.modelbased.net ]
 *
 * Copyright (C) 2012-  SINTEF ICT
 * Contact: Sebastien Mosser <sebastien.mosser@sintef.no>
 *
 * Module: net.modelbased.sensapp.service.dispatch
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
package net.modelbased.sensapp.service.dispatch


import cc.spray._
import cc.spray.typeconversion.SprayJsonSupport
import cc.spray.http._
import cc.spray.http.HttpHeaders._
import net.modelbased.sensapp.library.senml.{Root => SenMLRoot}
import net.modelbased.sensapp.library.senml.export.{JsonProtocol => SenMLProtocol}
import net.modelbased.sensapp.library.system.{Service => SensAppService} 

trait Service extends SensAppService {
  
  import SenMLProtocol._
  
  override lazy val partnerName = "dispatch"
  override lazy val partnersNames = List("database.raw", "registry", "notifier")
    
  val service = {
    path("dispatch") {
      detach {
        put { 
          content(as[SenMLRoot]) { data => context =>
            val handled = data.dispatch.par map {
              case (target, data) => {
                  try {
                    var author : String = null;
                    for(hdr <- context.request.headers){
                      if (hdr.name.equals("Authorization")){
                        author = hdr.value.trim;
                      }                  
                    }
                  
                    Dispatch(partners, target, data.measurementsOrParameters.get, author)
                    None
                  } catch { case e => { actorSystem.log.info(e.toString); Some(target) } }
                }
            }
            context complete handled.filter{ _.isDefined }.toList
          }
        }
      } ~ cors("PUT")
    }
  }
}