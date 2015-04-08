/*
 * Copyright (c) 2014 - 2015 Ventiv Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.ventiv.docker.manager.model

/**
 * Configuration for displaying Additional Metrics inline with the Service Interface it belongs to
 */
class AdditionalMetricsUiConfiguration {

    /**
     * Html template for the button.  Either buttonTemplate OR buttonPartial should be configured, not both.
     */
    String buttonTemplate;

    /**
     * File for rendering the button.  Either buttonTemplate OR buttonPartial should be configured, not both.
     */
    String buttonPartial;

    /**
     * Html template for the details modal.  Either detailsTemplate OR detailsPartial should be configured, not both.
     */
    String detailsTemplate;

    /**
     * File for rendering the details modal.  Either detailsTemplate OR detailsPartial should be configured, not both.
     */
    String detailsPartial;

}
