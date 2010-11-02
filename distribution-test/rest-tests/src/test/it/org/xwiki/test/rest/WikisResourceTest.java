/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.test.rest;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.xwiki.rest.Relations;
import org.xwiki.test.rest.framework.AbstractHttpTest;
import org.xwiki.rest.model.jaxb.Attachment;
import org.xwiki.rest.model.jaxb.Attachments;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.rest.model.jaxb.PageSummary;
import org.xwiki.rest.model.jaxb.Pages;
import org.xwiki.rest.model.jaxb.SearchResult;
import org.xwiki.rest.model.jaxb.SearchResults;
import org.xwiki.rest.model.jaxb.Wiki;
import org.xwiki.rest.model.jaxb.Wikis;
import org.xwiki.rest.resources.wikis.WikiAttachmentsResource;
import org.xwiki.rest.resources.wikis.WikiPagesResource;
import org.xwiki.rest.resources.wikis.WikiSearchResource;
import org.xwiki.rest.resources.wikis.WikisResource;

import java.util.Arrays;
import java.util.List;

public class WikisResourceTest extends AbstractHttpTest
{
    @Override
    public void testRepresentation() throws Exception
    {
        GetMethod getMethod = executeGet(getFullUri(WikisResource.class));
        assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        Wikis wikis = (Wikis) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
        assertTrue(getHttpMethodInfo(getMethod), wikis.getWikis().size() > 0);

        for (Wiki wiki : wikis.getWikis()) {
            Link link = getFirstLinkByRelation(wiki, Relations.SPACES);
            assertNotNull(link);

            link = getFirstLinkByRelation(wiki, Relations.CLASSES);
            assertNotNull(link);

            link = getFirstLinkByRelation(wiki, Relations.MODIFICATIONS);
            assertNotNull(link);

            link = getFirstLinkByRelation(wiki, Relations.SEARCH);
            assertNotNull(link);

            checkLinks(wiki);
        }
    }

    public void testSearch() throws Exception
    {
        GetMethod getMethod =
            executeGet(String.format("%s?q=easy-to-edit", getUriBuilder(WikiSearchResource.class).build(getWiki())));
        assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        SearchResults searchResults = (SearchResults) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        int resultSize = searchResults.getSearchResults().size();
        assertTrue(String.format("Found %s results", resultSize), resultSize >= 1);

        for (SearchResult searchResult : searchResults.getSearchResults()) {
            checkLinks(searchResult);
        }

        getMethod =
            executeGet(String.format("%s?q=WebHome&scope=name", getUriBuilder(WikiSearchResource.class)
                .build(getWiki())));
        assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        searchResults = (SearchResults) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        resultSize = searchResults.getSearchResults().size();
        assertTrue(String.format("Found %s results", resultSize), resultSize >= 3);

        for (SearchResult searchResult : searchResults.getSearchResults()) {
            checkLinks(searchResult);
        }

        // Note: we use $msg.get(...) a bit everywhere in our title for the moment... The search is a search in the DB
        // and not on the rendered content. Thus for our tests we search on msg...
        getMethod =
            executeGet(String.format("%s?q=msg&scope=title", getUriBuilder(WikiSearchResource.class).build(getWiki())));
        assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        searchResults = (SearchResults) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        resultSize = searchResults.getSearchResults().size();
        assertTrue(String.format("Found %s results", resultSize), searchResults.getSearchResults().size() >= 1);

        for (SearchResult searchResult : searchResults.getSearchResults()) {
            checkLinks(searchResult);
        }
    }

    public void testPages() throws Exception
    {
        // Get all pages
        GetMethod getMethod = executeGet(String.format("%s", getUriBuilder(WikiPagesResource.class).build(getWiki())));
        assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        Pages pages = (Pages) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        assertTrue(pages.getPageSummaries().size() > 0);

        for (PageSummary pageSummary : pages.getPageSummaries()) {
            checkLinks(pageSummary);
        }

        // Get all pages having a document name that contains "WebHome" (for all spaces)
        getMethod =
            executeGet(String.format("%s?name=WebHome", getUriBuilder(WikiPagesResource.class).build(getWiki())));
        assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        pages = (Pages) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        List<PageSummary> pageSummaries = pages.getPageSummaries();
        assertTrue(pageSummaries.size() > 0);
        // Verify that some WebHomes we expect are found.
        int foundCounter = 0;
        List<String> expectedWebHomes = Arrays.asList("Main.WebHome", "Sandbox.WebHome", "XWiki.WebHome");
        for (PageSummary pageSummary : pages.getPageSummaries()) {
            if (expectedWebHomes.contains(pageSummary.getFullName())) {
                foundCounter++;
            }
            assertTrue(pageSummary.getFullName().endsWith(".WebHome"));
            checkLinks(pageSummary);
        }
        // Note: since we can have translations, the number of found pages can be greater than the expected size.
        assertTrue("Some WebHome pages were not found!", foundCounter >= expectedWebHomes.size());

        // Get all pages having a document name that contains "WebHome" and a space with an "s" in its name.
        getMethod =
            executeGet(String
                .format("%s?name=WebHome&space=s", getUriBuilder(WikiPagesResource.class).build(getWiki())));
        assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        pages = (Pages) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        pageSummaries = pages.getPageSummaries();
        assertTrue(pageSummaries.size() > 0);
        // Verify that some WebHomes we expect are found.
        foundCounter = 0;
        expectedWebHomes = Arrays.asList("ColorThemes.WebHome", "Stats.WebHome", "Sandbox.WebHome", "Panels.WebHome",
            "Scheduler.WebHome", "Sandbox.WebHome");
        for (PageSummary pageSummary : pages.getPageSummaries()) {
            if (expectedWebHomes.contains(pageSummary.getFullName())) {
                foundCounter++;
            }
            assertTrue(pageSummary.getFullName().endsWith(".WebHome"));
            checkLinks(pageSummary);
        }
        // Note: since we can have translations, the number of found pages can be greater than the expected size.
        assertTrue("Some WebHome pages were not found!", foundCounter >= expectedWebHomes.size());
    }

    public void testAttachments() throws Exception
    {
        GetMethod getMethod =
            executeGet(String.format("%s", getUriBuilder(WikiAttachmentsResource.class).build(getWiki())));
        assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        Attachments attachments = (Attachments) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        assertTrue(attachments.getAttachments().size() > 0);

        for (Attachment attachment : attachments.getAttachments()) {
            checkLinks(attachment);
        }

        // Matches Sandbox.WebHome@XWikLogo.png
        getMethod =
            executeGet(String.format("%s?name=Logo", getUriBuilder(WikiAttachmentsResource.class).build(getWiki())));
        assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        attachments = (Attachments) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        assertEquals(getAttachmentsInfo(attachments), 1, attachments.getAttachments().size());

        for (Attachment attachment : attachments.getAttachments()) {
            checkLinks(attachment);
        }

        getMethod =
            executeGet(String.format("%s?space=sandbox", getUriBuilder(WikiAttachmentsResource.class).build(getWiki())));
        assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        attachments = (Attachments) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        assertEquals(getAttachmentsInfo(attachments), 1, attachments.getAttachments().size());

        for (Attachment attachment : attachments.getAttachments()) {
            checkLinks(attachment);
        }

        getMethod =
            executeGet(String.format("%s?name=rq&space=Main", getUriBuilder(WikiAttachmentsResource.class).build(
                getWiki())));
        assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        attachments = (Attachments) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        assertEquals(getAttachmentsInfo(attachments), 1, attachments.getAttachments().size());

        for (Attachment attachment : attachments.getAttachments()) {
            checkLinks(attachment);
        }
    }
}
