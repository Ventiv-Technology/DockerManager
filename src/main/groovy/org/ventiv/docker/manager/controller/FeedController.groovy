package org.ventiv.docker.manager.controller

import com.rometools.rome.feed.rss.Channel
import com.rometools.rome.feed.rss.Description
import com.rometools.rome.feed.rss.Item
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.feed.AbstractRssFeedView
import org.ventiv.docker.manager.DockerManagerApplication
import org.ventiv.docker.manager.config.DockerManagerConfiguration
import org.ventiv.docker.manager.model.UserAudit
import org.ventiv.docker.manager.repository.UserAuditRepository

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Controller to serve Feeds, generally based on UserAuditRepository.
 */
@Controller
@CompileStatic
class FeedController {

    @Resource UserAuditRepository auditRepository;

    @CompileDynamic
    @RequestMapping("/api/feed.rss")
    public ModelAndView getRssFeed(Pageable page) {
        if (page instanceof PageRequest && page.getSort() == null)
            page = new PageRequest(page.getPageNumber(), page.getPageSize(), new Sort(Sort.Direction.DESC, "requestFinished"));

        return new ModelAndView(new UserAuditRssView(), [userAudits: auditRepository.findAll(page)?.getContent()])
    }

    public static final class UserAuditRssView extends AbstractRssFeedView {

        @Override
        protected void buildFeedMetadata(Map<String, Object> model, Channel feed, HttpServletRequest request) {

            feed.setTitle("Docker Manager Events");
            feed.setDescription("All events occurring on Docker Manager for Tiers: " + DockerManagerApplication.getApplicationContext().getBean(DockerManagerConfiguration).getActiveTiers()?.join(", ") ?: "All Tiers");
            feed.setLink(request.getRequestURL().toString());

            super.buildFeedMetadata(model, feed, request);
        }

        @Override
        protected List<Item> buildFeedItems(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
            Collection<UserAudit> userAudits = (Collection<UserAudit>) model.userAudits;

            return userAudits.collect {
                Item item = new Item();
                item.setAuthor(it.getPrincipal())
                item.setTitle("${it.getPermission()} on ${it.getApplicationThumbnail().getTierName()}.${it.getApplicationThumbnail().getEnvironmentName()}.${it.getApplicationThumbnail().getApplicationId()}.${it.getServiceInstanceThumbnail().getName()}.${it.getServiceInstanceThumbnail().getInstanceNumber()}")
                item.setPubDate(it.getPermissionEvaluated())
                item.setLink(request.getRequestURL().replaceAll("/api/feed.rss", "").toString() + "/#/env/" + it.getApplicationThumbnail().getTierName() + "/" + it.getApplicationThumbnail().getEnvironmentName() + "/" + it.getApplicationThumbnail().getApplicationId())

                Description description = new Description()
                description.setValue(item.getTitle())
                item.setDescription(description)

                return item;
            }
        }

    }

}
