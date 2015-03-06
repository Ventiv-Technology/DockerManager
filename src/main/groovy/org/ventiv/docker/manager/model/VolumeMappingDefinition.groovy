package org.ventiv.docker.manager.model

/**
 * A fully mapped out Volume, when matched from Host to Container.
 */
class VolumeMappingDefinition {

    String type;
    String hostVolume;
    String containerVolume;

}
