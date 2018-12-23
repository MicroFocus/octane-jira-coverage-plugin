/*
 *     Copyright 2018 EntIT Software LLC, a Micro Focus company, L.P.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */


package com.microfocus.octane.plugins.rest.entities;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by berkovir on 21/11/2016.
 */
public class OctaneEntityCollection {

    private int totalCount;

    private boolean exceedsTotalCount;

    private List<OctaneEntity> data = new ArrayList<>();

    public List<OctaneEntity> getData() {
        return data;
    }

    public void setData(List<OctaneEntity> data) {
        this.data = data;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public boolean isExceedsTotalCount() {
        return exceedsTotalCount;
    }

    public void setExceedsTotalCount(boolean exceedsTotalCount) {
        this.exceedsTotalCount = exceedsTotalCount;
    }
}
