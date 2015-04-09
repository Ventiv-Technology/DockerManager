package org.ventiv.docker.manager.controller

import com.google.common.net.MediaType
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.ventiv.docker.manager.metrics.store.AbstractAdditionalMetricsStore
import org.ventiv.docker.manager.metrics.store.AdditionalMetricsStorage
import org.ventiv.docker.manager.model.ServiceInstance
import org.ventiv.docker.manager.service.ServiceInstanceService

import javax.annotation.Resource
import javax.servlet.http.HttpServletResponse

/**
 * Created by jcrygier on 4/9/15.
 */
@Slf4j
@CompileStatic
@RestController
@RequestMapping("/api/metrics")
class AdditionalMetricsController {

    @Resource ServiceInstanceService serviceInstanceService;
    @Resource AbstractAdditionalMetricsStore additionalMetricsStore;

    @RequestMapping("/widgets")
    public void additionalMetricsWidgets(HttpServletResponse response) {
        response.setContentType(MediaType.TEXT_JAVASCRIPT_UTF_8.toString())
    }

    @RequestMapping("/container/{containerId}")
    public List<AdditionalMetricsStorage> getMetricsForContainer(@PathVariable("containerId") String containerId,
                                                            @RequestParam(value = "startDate", required = false) Date startDate,
                                                            @RequestParam(value = "endDate", required = false) Date endDate) {
        ServiceInstance serviceInstance = serviceInstanceService.getServiceInstance(containerId);
        if (serviceInstance)
            return additionalMetricsStore.getAdditionalMetricsBetween(serviceInstance, startDate?.getTime(), endDate?.getTime())
        else
            return null;
    }

}
