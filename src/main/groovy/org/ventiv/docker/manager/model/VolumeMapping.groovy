package org.ventiv.docker.manager.model

import javax.validation.constraints.NotNull

/**
 * A configured Volume Mapping
 */
class VolumeMapping {

    @NotNull
    String type;

    @NotNull
    String path;

}
