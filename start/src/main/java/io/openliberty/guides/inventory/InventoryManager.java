/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - Initial implementation
 *******************************************************************************/
package io.openliberty.guides.inventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.eclipse.microprofile.metrics.annotation.SimplyTimed;
import org.eclipse.microprofile.metrics.annotation.Timed;

import io.openliberty.guides.inventory.model.InventoryList;
import io.openliberty.guides.inventory.model.SystemData;

@ApplicationScoped
public class InventoryManager {

  private List<SystemData> systems = Collections.synchronizedList(new ArrayList<>());
  private InventoryUtils invUtils = new InventoryUtils();

  @Timed( name = "inventoryProcessingTime" , 
		  tags = {"method=get"} ,
		  absolute = true , 
		  description = "Time needed to process the inventory.")
  public Properties get(String hostname) {
    return invUtils.getProperties(hostname);
  }

  @SimplyTimed( name = "inventoryAddingTime" , 
		  absolute = true , 
		  description = "Time needed to add system properties to the inventory.")
  public void add(String hostname, Properties systemProps) {
    Properties props = new Properties();
    props.setProperty("os.name", systemProps.getProperty("os.name"));
    props.setProperty("user.name", systemProps.getProperty("user.name"));

    SystemData host = new SystemData(hostname, props);
    if (!systems.contains(host))
      systems.add(host);
  }

  @Timed( name = "inventoryProcessingTime" , 
		  absolute = true ,
		  tags = {"method=list"},
		  description = "Time needed to process the inventory.")
  @Counted( 
		  name= "inventoryAccessCount",
  		  absolute = true , 
  		  description = "Number of times the list of systems method is requested.")
  public InventoryList list() {
    return new InventoryList(systems);
  }

  @Gauge( name = "inventorySizeGauge" , 
		  unit = MetricUnits.NONE , 
		  absolute = true , 
		  description = "Number of systems in the inventory.")
  public int getTotal() {
    return systems.size();
  }
}
