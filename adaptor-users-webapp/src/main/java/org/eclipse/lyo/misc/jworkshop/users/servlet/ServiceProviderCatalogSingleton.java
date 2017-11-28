/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 *
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *
 *     Russell Boykin       - initial API and implementation
 *     Alberto Giammaria    - initial API and implementation
 *     Chris Peters         - initial API and implementation
 *     Gianluca Bernardini  - initial API and implementation
 *     Michael Fiedler      - adapted for Bugzilla service provider
 *     Jad El-khoury        - initial implementation of code generator (422448)
 *     Matthieu Helleboid   - initialize each service provider separately
 *     Anass Radouani       - initialize each service provider separately
 *
 * This file is generated by org.eclipse.lyo.oslc4j.codegenerator
 *******************************************************************************/

package org.eclipse.lyo.misc.jworkshop.users.servlet;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import org.eclipse.lyo.oslc4j.client.ServiceProviderRegistryURIs;
import org.eclipse.lyo.oslc4j.core.model.Publisher;
import org.eclipse.lyo.oslc4j.core.model.Service;
import org.eclipse.lyo.oslc4j.core.model.ServiceProvider;
import org.eclipse.lyo.oslc4j.core.model.ServiceProviderCatalog;
import org.eclipse.lyo.oslc4j.core.OSLC4JUtils;

import org.eclipse.lyo.misc.jworkshop.users.UserDirectoryAdaptorManager;
import org.eclipse.lyo.misc.jworkshop.users.ServiceProviderInfo;

// Start of user code imports
// End of user code

/**
 * This is the OSLC service provider catalog for the Bugzilla adapter.  Service providers are
 * not registered with the catalog until a request comes in to access either the catalog or a
 * specific service provider.   This request could be from an external consumer or an internal
 * request triggered by a consumer accessing a change request.
 *
 * The service providers are created and registered in the initServiceProvidersFromProducts()
 * method.  A list of accessible products is retrieved from Bugzilla and a ServiceProvider is
 * created and registered for each using the Bugzilla productId as the identifier.
 *
 * The registered service providers are refreshed on each catalog or service provider collection
 * request.
 */
public class ServiceProviderCatalogSingleton
{
    private static final ServiceProviderCatalog serviceProviderCatalog;
    private static final SortedMap<String, ServiceProvider> serviceProviders = new TreeMap<String, ServiceProvider>();

    static {
        serviceProviderCatalog = new ServiceProviderCatalog();
        URI catalogUri = UriBuilder.fromUri(OSLC4JUtils.getServletURI()).path("/catalog/singleton").build();
        serviceProviderCatalog.setAbout(catalogUri);
        serviceProviderCatalog.setTitle("Service Provider Catalog");
        serviceProviderCatalog.setDescription("Service Provider Catalog");
    }

    private ServiceProviderCatalogSingleton()
    {
        super();
    }


    public static URI getUri()
    {
        return serviceProviderCatalog.getAbout();
    }

    public static ServiceProviderCatalog getServiceProviderCatalog(HttpServletRequest httpServletRequest)
    {
        initServiceProviders(httpServletRequest);
        return serviceProviderCatalog;
    }

    public static ServiceProvider [] getServiceProviders(HttpServletRequest httpServletRequest)
    {
        synchronized(serviceProviders)
        {
            initServiceProviders(httpServletRequest);
            return serviceProviders.values().toArray(new ServiceProvider[ serviceProviders.size()]);
        }
    }


    private static URI constructServiceProviderURI(final String serviceProviderId)
    {
        String basePath = OSLC4JUtils.getServletURI();
        Map<String, Object> pathParameters = new HashMap<String, Object>();
        pathParameters.put("serviceProviderId", serviceProviderId);
        String instanceURI = "serviceProviders/{serviceProviderId}";

        final UriBuilder builder = UriBuilder.fromUri(basePath);
        return builder.path(instanceURI).buildFromMap(pathParameters);
    }

    private static String serviceProviderIdentifier(final String serviceProviderId)
    {
        String identifier = "/" + serviceProviderId;
        return identifier;
    }

    public static ServiceProvider getServiceProvider(HttpServletRequest httpServletRequest, final String serviceProviderId)
    {
        ServiceProvider serviceProvider;

        synchronized(serviceProviders)
        {
            String identifier = serviceProviderIdentifier(serviceProviderId);
            serviceProvider = serviceProviders.get(identifier);

            //One retry refreshing the service providers
            if (serviceProvider == null)
            {
                getServiceProviders(httpServletRequest);
                serviceProvider = serviceProviders.get(identifier);
            }
        }

        if (serviceProvider != null)
        {
            return serviceProvider;
        }

        throw new WebApplicationException(Status.NOT_FOUND);
    }

    public static ServiceProvider registerServiceProvider(final HttpServletRequest httpServletRequest,
                                                          final ServiceProvider serviceProvider,
                                                          final String serviceProviderId)
                                                throws URISyntaxException
    {
        synchronized(serviceProviders)
        {
            final URI serviceProviderURI = constructServiceProviderURI(serviceProviderId);
            return registerServiceProviderNoSync(serviceProviderURI,
                                                 serviceProvider,
                                                 serviceProviderId);
        }
    }

    /**
    * Register a service provider with the OSLC catalog
    *
    * @param serviceProviderURI
    * @param serviceProvider
    * @param productId
    * @return
    */
    private static ServiceProvider registerServiceProviderNoSync(final URI serviceProviderURI,
                                                                 final ServiceProvider serviceProvider
                                                                 , final String serviceProviderId)
    {
        final SortedSet<URI> serviceProviderDomains = getServiceProviderDomains(serviceProvider);

        String identifier = serviceProviderIdentifier(serviceProviderId);
        serviceProvider.setAbout(serviceProviderURI);
        serviceProvider.setIdentifier(identifier);
        serviceProvider.setCreated(new Date());
        serviceProvider.setDetails(new URI[] {serviceProviderURI});

        serviceProviderCatalog.addServiceProvider(serviceProvider);
        serviceProviderCatalog.addDomains(serviceProviderDomains);

        serviceProviders.put(identifier, serviceProvider);

        return serviceProvider;
    }

    // This version is for self-registration and thus package-protected
    static ServiceProvider registerServiceProvider(final ServiceProvider serviceProvider, final String serviceProviderId)
                                            throws URISyntaxException
    {
        synchronized(serviceProviders)
        {
            final URI serviceProviderURI = constructServiceProviderURI(serviceProviderId);

            return registerServiceProviderNoSync(serviceProviderURI, serviceProvider, serviceProviderId);
        }
    }

    public static void deregisterServiceProvider(final String serviceProviderId)
    {
        synchronized(serviceProviders)
        {
            final ServiceProvider deregisteredServiceProvider =
                serviceProviders.remove(serviceProviderIdentifier(serviceProviderId));

            if (deregisteredServiceProvider != null)
            {
                final SortedSet<URI> remainingDomains = new TreeSet<URI>();

                for (final ServiceProvider remainingServiceProvider : serviceProviders.values())
                {
                    remainingDomains.addAll(getServiceProviderDomains(remainingServiceProvider));
                }

                final SortedSet<URI> removedServiceProviderDomains = getServiceProviderDomains(deregisteredServiceProvider);

                removedServiceProviderDomains.removeAll(remainingDomains);
                serviceProviderCatalog.removeDomains(removedServiceProviderDomains);
                serviceProviderCatalog.removeServiceProvider(deregisteredServiceProvider);
            }
            else
            {
                throw new WebApplicationException(Status.NOT_FOUND);
            }
        }
    }

    private static SortedSet<URI> getServiceProviderDomains(final ServiceProvider serviceProvider)
    {
        final SortedSet<URI> domains = new TreeSet<URI>();

        if (serviceProvider!=null) {
            final Service [] services = serviceProvider.getServices();
            for (final Service service : services)
            {
                final URI domain = service.getDomain();

                domains.add(domain);
            }
        }
        return domains;
    }

    /**
     * Retrieve a list of products from Bugzilla and construct a service provider for each.
     *
     * Each product ID is added to the parameter map which will be used during service provider
     * creation to create unique URI paths for each Bugzilla product.  See @Path definition at
     * the top of BugzillaChangeRequestService.
     *
     * @param httpServletRequest
     */
    protected static void initServiceProviders (HttpServletRequest httpServletRequest)
    {
        try {
            // Start of user code initServiceProviders
            // End of user code

            String basePath = OSLC4JUtils.getServletURI();

            ServiceProviderInfo [] serviceProviderInfos = UserDirectoryAdaptorManager.getServiceProviderInfos(httpServletRequest);
            //Register each service provider
            for (ServiceProviderInfo serviceProviderInfo : serviceProviderInfos) {
                String identifier = serviceProviderIdentifier(serviceProviderInfo.serviceProviderId);
                if (!serviceProviders.containsKey(identifier)) {
                    String serviceProviderName = serviceProviderInfo.name;
                    String title = String.format("Service Provider '%s'", serviceProviderName);
                    String description = String.format("%s (id: %s; kind: %s)",
                        "Service Provider",
                        identifier,
                        "Service Provider");
                    Publisher publisher = null;
                    Map<String, Object> parameterMap = new HashMap<String, Object>();
                    parameterMap.put("serviceProviderId", serviceProviderInfo.serviceProviderId);
                    final ServiceProvider aServiceProvider = ServiceProvidersFactory.createServiceProvider(basePath, title, description, publisher, parameterMap);
                    registerServiceProvider(aServiceProvider, serviceProviderInfo.serviceProviderId);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new WebApplicationException(e,Status.INTERNAL_SERVER_ERROR);
        }
    }
}
