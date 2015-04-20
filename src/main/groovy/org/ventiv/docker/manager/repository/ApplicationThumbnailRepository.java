package org.ventiv.docker.manager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.ventiv.docker.manager.model.ApplicationThumbnail;

/**
 * Created by jcrygier on 4/20/15.
 */
public interface ApplicationThumbnailRepository extends JpaRepository<ApplicationThumbnail, Long> {

    public ApplicationThumbnail findByTierNameAndEnvironmentNameAndApplicationId(String tierName, String environmentName, String applicationId);
}
