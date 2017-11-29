/*******************************************************************************
 * Copyright (c) 2011, 2012 IBM Corporation and others.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 *  
 *  The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *  and the Eclipse Distribution License is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 *  
 *  Contributors:
 *  
 *	   Sam Padgett	       - initial API and implementation
 *     Michael Fiedler     - adapted for OSLC4J
 *     Jad El-khoury        - initial implementation of code generator (https://bugs.eclipse.org/bugs/show_bug.cgi?id=422448)
 *     Matthieu Helleboid   - Support for multiple Service Providers.
 *     Anass Radouani       - Support for multiple Service Providers.
 *
 * This file is generated by org.eclipse.lyo.oslc4j.codegenerator
 *******************************************************************************/

package org.eclipse.lyo.misc.jworkshop.jira;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletContextEvent;
import java.util.List;

import org.eclipse.lyo.oslc4j.core.model.ServiceProvider;
import org.eclipse.lyo.oslc4j.core.model.AbstractResource;
import org.eclipse.lyo.misc.jworkshop.jira.servlet.ServiceProviderCatalogSingleton;
import org.eclipse.lyo.misc.jworkshop.jira.ServiceProviderInfo;
import org.eclipse.lyo.misc.jworkshop.jira.resources.ChangeRequest;
import org.eclipse.lyo.misc.jworkshop.jira.resources.Person;
import org.eclipse.lyo.misc.jworkshop.jira.resources.Project;

// Start of user code imports
import java.util.ArrayList;
import org.eclipse.lyo.store.Store;
import org.eclipse.lyo.store.StoreFactory;
import org.eclipse.lyo.store.update.StoreUpdateManager;
import org.eclipse.lyo.store.update.change.ChangeProvider;
import org.eclipse.lyo.store.update.handlers.TrsMqttChangeLogHandler;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Properties;
import org.eclipse.lyo.misc.jworkshop.jira.services.JiraTrsService;
// End of user code

// Start of user code pre_class_code
// End of user code

public class JiraAdaptorManager {

    // Start of user code class_attributes
	public static Store store = null;
	private static final Logger log = LoggerFactory.getLogger(JiraAdaptorManager.class);

	public static JiraChangeProvider changeProvider;
    public static StoreUpdateManager<Object> updateManager;
    private static MqttClient client;
    // End of user code
    
    
    // Start of user code class_methods
    // End of user code

    public static void contextInitializeServletListener(final ServletContextEvent servletContextEvent)
    {
        
        // Start of user code contextInitializeServletListener
        try {
            Properties props = new Properties();
            InputStream jiraPropertiesStream = JiraAdaptorManager.class.getClassLoader().getResourceAsStream("jira.properties");
            props.load(jiraPropertiesStream);
            String sparqlQueryUrl = props.getProperty("sparqlQueryUrl");
            String sparqlUpdateUrl = props.getProperty("sparqlUpdateUrl");
            store = StoreFactory.sparql(sparqlQueryUrl, sparqlUpdateUrl);
            
            changeProvider = new JiraChangeProvider();
            updateManager = new StoreUpdateManager<>(store, changeProvider);
            String topic = "TRSServer";
            client = new MqttClient("tcp://localhost:1883", "JiraAdaptor");
            client.connect();
            updateManager.addHandler(new TrsMqttChangeLogHandler<Object>(JiraTrsService.changeHistories, client, topic));
        } catch (IOException e) {
            log.error("problem loading properties file", e);
            System.exit(1);
        } catch (MqttException e) {
            e.printStackTrace();
        }
        // End of user code
    }

    public static void contextDestroyServletListener(ServletContextEvent servletContextEvent) 
    {
        
        // Start of user code contextDestroyed
        // TODO Implement code to shutdown connections to data backbone etc...
        // End of user code
    }

    public static ServiceProviderInfo[] getServiceProviderInfos(HttpServletRequest httpServletRequest)
    {
        ServiceProviderInfo[] serviceProviderInfos = {};
        
        // Start of user code "ServiceProviderInfo[] getServiceProviderInfos(...)"
        ServiceProviderInfo anSP = new ServiceProviderInfo();
        anSP.name = "the only SP"; 
        anSP.serviceProviderId = "1";
        serviceProviderInfos = new ServiceProviderInfo[1];
        serviceProviderInfos[0] = anSP;
        // End of user code
        return serviceProviderInfos;
    }

    public static List<ChangeRequest> queryChangeRequests(HttpServletRequest httpServletRequest, final String serviceProviderId, String where, int page, int limit)
    {
        List<ChangeRequest> resources = null;
        
        // Start of user code queryChangeRequests
        try {
        	resources = new ArrayList<ChangeRequest>(JiraAdaptorManager.store.getResources(new URI ("urn:x-arq:DefaultGraph"), ChangeRequest.class));
        } catch (Exception e) {
            log.error("Failed to get ChangeRequest resources", e);
        }
        // End of user code
        return resources;
    }


    public static ChangeRequest getChangeRequest(HttpServletRequest httpServletRequest, final String serviceProviderId, final String changeRequestId)
    {
        ChangeRequest aResource = null;
        
        // Start of user code getChangeRequest
        try {
            URI changeRequestUri = ChangeRequest.constructURI(serviceProviderId, changeRequestId);
            aResource = JiraAdaptorManager.store.getResource(new URI ("urn:x-arq:DefaultGraph"), changeRequestUri, ChangeRequest.class);
        } catch (Exception e) {
            log.error("Failed to get a ChangeRequest resource", e);
        }
        // End of user code
        return aResource;
    }



    public static List<Project> queryProjects(HttpServletRequest httpServletRequest, final String serviceProviderId, String where, int page, int limit)
    {
        List<Project> resources = null;
        
        // Start of user code queryProjects
        // TODO Implement code to return a set of resources
        // End of user code
        return resources;
    }


    public static Project getProject(HttpServletRequest httpServletRequest, final String serviceProviderId, final String projectId)
    {
        Project aResource = null;
        
        // Start of user code getProject
        // TODO Implement code to return a resource
        // End of user code
        return aResource;
    }




    public static String getETagFromChangeRequest(final ChangeRequest aResource)
    {
        String eTag = null;
        // Start of user code getETagFromChangeRequest
        // TODO Implement code to return an ETag for a particular resource
        // End of user code
        return eTag;
    }
    public static String getETagFromProject(final Project aResource)
    {
        String eTag = null;
        // Start of user code getETagFromProject
        // TODO Implement code to return an ETag for a particular resource
        // End of user code
        return eTag;
    }

}
