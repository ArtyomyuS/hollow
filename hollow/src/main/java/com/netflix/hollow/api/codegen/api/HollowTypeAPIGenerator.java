/*
 *  Copyright 2016-2019 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package com.netflix.hollow.api.codegen.api;

import static com.netflix.hollow.api.codegen.HollowCodeGenerationUtils.typeAPIClassname;

import com.netflix.hollow.api.codegen.CodeGeneratorConfig;
import com.netflix.hollow.api.codegen.HollowConsumerJavaFileGenerator;
import com.netflix.hollow.core.HollowDataset;
import com.netflix.hollow.core.schema.HollowSchema;

public abstract class HollowTypeAPIGenerator extends HollowConsumerJavaFileGenerator {
    public static final String SUB_PACKAGE_NAME = "core";

    protected final String apiClassname;

    public HollowTypeAPIGenerator(String stateEngineClassname, String packageName, HollowSchema schema,
            HollowDataset dataset, CodeGeneratorConfig config) {
        super(packageName, SUB_PACKAGE_NAME, dataset, config);
        this.apiClassname = stateEngineClassname;
        this.className = typeAPIClassname(schema.getName());
    }
}