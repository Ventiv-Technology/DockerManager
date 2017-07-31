/**
 * Copyright (c) 2014 - 2017 Ventiv Technology
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
package org.ventiv.docker.manager.utils;

import java.util.LinkedHashMap;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Created by jcrygier on 7/31/17.
 */
public class CollectionUtils {

    public static <T, K, U> Collector<T, ?, LinkedHashMap<K, U>> toLinkedHashMap(Function<? super T, ? extends K> keyMapper,
                                                                                 Function<? super T, ? extends U> valueMapper) {
        return Collectors.toMap(keyMapper, valueMapper, (u, v) -> {
            throw new IllegalStateException(String.format("Duplicate key %s", u));
        }, LinkedHashMap::new);
    }

}
