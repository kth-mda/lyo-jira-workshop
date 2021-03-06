package org.eclipse.lyo.misc.jworkshop.jira.services;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.eclipse.lyo.client.oslc.OslcClient;
import org.eclipse.lyo.client.oslc.resources.OslcQuery;
import org.eclipse.lyo.client.oslc.resources.OslcQueryParameters;
import org.eclipse.lyo.client.oslc.resources.OslcQueryResult;
import org.eclipse.lyo.misc.jworkshop.jira.JiraAdaptorManager;
import org.eclipse.lyo.misc.jworkshop.jira.clients.ActiveDirectoryAdaptorClient;
import org.eclipse.lyo.misc.jworkshop.jira.resources.ChangeRequest;
import org.eclipse.lyo.misc.jworkshop.jira.resources.Project;
import org.eclipse.lyo.oslc4j.core.model.Link;
import org.eclipse.lyo.oslc4j.core.model.QueryCapability;
import org.eclipse.lyo.oslc4j.core.model.Service;
import org.eclipse.lyo.oslc4j.core.model.ServiceProvider;
import org.eclipse.lyo.oslc4j.core.model.ServiceProviderCatalog;
import org.eclipse.lyo.store.update.change.ChangeKind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

@Path("jira/webhooks")
public class JiraWebhooksService {
    @Context
    private HttpServletRequest httpServletRequest;
    @Context
    private HttpServletResponse httpServletResponse;
    @Context
    private UriInfo uriInfo;

    private static final Logger log = LoggerFactory.getLogger(JiraWebhooksService.class);

    public JiraWebhooksService() {
        super();
    }

    public ChangeRequest getChangeRequest(LinkedHashMap<String, Object> issueData) throws URISyntaxException {
        ChangeRequest aChangeRequest = new ChangeRequest();

        aChangeRequest.setIdentifier((String) issueData.get("id"));
        LinkedHashMap<String, Object> issueFieldsData = (LinkedHashMap<String, Object>) issueData.get("fields");
        aChangeRequest.setDescription((String) issueFieldsData.get("description"));
        aChangeRequest.setTitle((String) issueFieldsData.get("summary"));
        LinkedHashMap<String, Object> projectData = (LinkedHashMap<String, Object>) issueFieldsData.get("project");
        // String projectName = (String) projectData.get("name");
        // aChangeRequest.setProject(projectName);
        String projectId = (String) projectData.get("id");
        aChangeRequest.setProject(Project.constructLink("1", projectId));
        LinkedHashMap<String, Object> creatorData = (LinkedHashMap<String, Object>) issueFieldsData.get("creator");
        URI personURI = queryPersonURI((String) creatorData.get("key"));
        aChangeRequest.setCreator(new Link(personURI));
        aChangeRequest.setAbout(ChangeRequest.constructURI("1", aChangeRequest.getIdentifier()));
        return aChangeRequest;
    }

    @POST
    @Path("issues")
    @Produces({ MediaType.TEXT_HTML })
    public Response handleIssuesWebhooks() throws ServletException, IOException, URISyntaxException {
        log.info("Received webhook issue update call");

        ServletInputStream inputStream = httpServletRequest.getInputStream();
        ObjectMapper mapper = new ObjectMapper();

        Map<String, Object> jsonData = new HashMap<String, Object>();
        jsonData = mapper.readValue(inputStream, Map.class);

        LinkedHashMap<String, Object> issueData = (LinkedHashMap<String, Object>) jsonData.get("issue");
        try {
            // Transform the Json Issue into a ChangeRequest resource.
            ChangeRequest aChangeRequest = getChangeRequest(issueData);

            // TODO distinguish change kind
            JiraAdaptorManager.changeProvider.addResourceChange(ChangeKind.MODIFICATION, aChangeRequest);

            JiraAdaptorManager.updateManager.submit(null, new Object());

            // Save the ChangeRequest resource into a triplestore.
            JiraAdaptorManager.store.updateResources(new URI("urn:x-arq:DefaultGraph"), aChangeRequest);
            log.debug("Jira webhook issue update was successfully persisted in the triplestore");
        } catch (Exception e) {
            log.error("Jira webhook issue processing has failed", e);
        }

        return Response.ok().build();
    }

    public Project getProject(LinkedHashMap<String, Object> projectData) throws URISyntaxException {
        Project project = new Project();
        String projectId = projectData.get("id").toString();
        project.setIdentifier(projectId);
        project.setTitle((String) projectData.get("name"));
        project.setAbout(Project.constructURI("1", project.getIdentifier()));
        return project;
    }

    @POST
    @Path("projects")
    @Produces({ MediaType.TEXT_HTML })
    public Response handleProjectsWebhooks() throws ServletException, IOException, URISyntaxException {
        log.info("Received webhook project update call");
        ServletInputStream inputStream = httpServletRequest.getInputStream();
        ObjectMapper mapper = new ObjectMapper();

        Map<String, Object> jsonData = new HashMap<String, Object>();
        jsonData = mapper.readValue(inputStream, Map.class);

        LinkedHashMap<String, Object> projectData = (LinkedHashMap<String, Object>) jsonData.get("project");
        Project project = getProject(projectData);
        try {
            JiraAdaptorManager.store.updateResources(new URI("urn:x-arq:DefaultGraph"), project);
            log.debug("Jira webhook issue update was successfully persisted in the triplestore");
        } catch (Exception e) {
            log.error("Jira webhook issue processing has failed", e);
        }
        return Response.ok().build();
    }

    @GET
    @Path("initIssues")
    @Produces({ MediaType.TEXT_HTML })
    public Response initializeIssues() throws ServletException, IOException, URISyntaxException {
        try {
            final String username = "admin";
            final String password = "admin";
            final String basePath = "http://localhost:2990/"; // or should I add
                                                              // "/jira" to the
                                                              // end?
            final int pageSize = 2;

            int startAt = 0;
            int maxResults = pageSize;
            log.info("Initialising Jira issues manually");
            while (true) {
                UriBuilder builder = UriBuilder.fromUri(basePath);
                builder.path("rest/api/2/search");
                // builder.queryParam("assignee", "admin");
                builder.queryParam("startAt", startAt);
                builder.queryParam("maxResults", maxResults);

                HttpClient client = new DefaultHttpClient();
                HttpGet request = new HttpGet(builder.build());
                String encoding = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
                request.setHeader("Authorization", "Basic " + encoding);
                request.setHeader("Accept", "application/json");
                HttpResponse response = client.execute(request);

                InputStream inputStream = response.getEntity().getContent();
                ObjectMapper mapper = new ObjectMapper();

                Map<String, Object> jsonData = new HashMap<String, Object>();
                jsonData = mapper.readValue(inputStream, Map.class);
                ArrayList<Object> issuesData = (ArrayList<Object>) jsonData.get("issues");
                if (issuesData.size() == 0) {
                    log.warn("Response contained 0 issues, finishing...");
                    break;
                }

                ChangeRequest[] issues = new ChangeRequest[issuesData.size()];
                int i = 0;
                for (Object issueData : issuesData) {
                    issues[i] = getChangeRequest((LinkedHashMap<String, Object>) issueData);
                    i++;
                }

                try {
                    JiraAdaptorManager.store.updateResources(new URI("urn:x-arq:DefaultGraph"), issues);
                    log.debug("Issues were persisted in the triplestore");
                } catch (Exception e) {
                    log.error("Failed to perform an initial update of Issues resources", e);
                }

                startAt = startAt + maxResults;
            }
        } catch (Exception e) {
            log.error("Failed to perform an initial update of Issues resources", e);
        }

        return Response.ok().build();
    }

    public URI queryPersonURI(String name) {
        ServiceProviderCatalog spc = null;
        try {
            spc = ActiveDirectoryAdaptorClient.getServiceProviderCatalog();
            log.info("Querying the person via the SPC '{}'", spc);
        } catch (Exception e) {
            log.error("Failed to get the ServiceProviderCatalog", e);
            return null;
        }
        ServiceProvider sp = spc.getServiceProviders()[0];
        Service s = sp.getServices()[0];
        QueryCapability qc = s.getQueryCapabilities()[0];
        URI queryBase = qc.getQueryBase();

        String where = name;

        OslcClient client = new OslcClient();
        OslcQueryParameters queryParameters = new OslcQueryParameters();
        queryParameters.setWhere(where);
        OslcQuery query = new OslcQuery(client, queryBase.toString(), queryParameters);
        OslcQueryResult queryResults = query.submit();
        String[] urls = queryResults.getMembersUrls();
        if (urls.length == 0) {
            log.error("No User instance identified with name:" + name);
            return null;
        }
        if (urls.length > 1) {
            log.error("More than one User instance identified with name:" + name);
            return null;
        }
        try {
            return new URI(urls[0]);
        } catch (URISyntaxException e) {
            log.error("URISyntaxException occured when querying for name:" + name);
            return null;
        }
    }

}
